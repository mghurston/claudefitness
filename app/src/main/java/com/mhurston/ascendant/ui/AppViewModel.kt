package com.mhurston.ascendant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mhurston.ascendant.data.Repository
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.AchStatus
import com.mhurston.ascendant.domain.CharacterState
import com.mhurston.ascendant.domain.CustomExercise
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.DayDerived
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import com.mhurston.ascendant.domain.Quest
import com.mhurston.ascendant.domain.Quests
import com.mhurston.ascendant.domain.VideoLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class ExerciseKind { PUSHUPS, SQUATS, LEG_LIFTS, CALF_RAISES, CURLS }

data class UiState(
    val loading: Boolean = true,
    val character: CharacterState,
    val today: WorkoutDayEntity,
    /** Today after carry-forward — the exact DayData the XP engine scored. UI that shows
     *  today's burn must read THIS (not today.toDayData()) so the kcal number and the XP
     *  number come from the same body weight and stay 1:1. */
    val todayData: DayData = DayData(LocalDate.now()),
    val todayDerived: DayDerived,
    /** Today's calories-consumed after carry-forward — the value in effect for today's diet
     *  XP and the one the Energy tab pre-fills (yesterday's number until you change it).
     *  -1 = nothing entered anywhere yet; 0 = a fasting day. */
    val todayConsumed: Int = -1,
    val days: List<WorkoutDayEntity> = emptyList(),
    val derivedByDate: Map<LocalDate, DayDerived> = emptyMap(),
    val profile: Profile = Profile(),
    val achievements: List<AchStatus> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val favoriteVideoUrls: Set<String> = emptySet(),
    val userVideos: List<VideoLink> = emptyList(),
    /** Active (non-archived) custom exercises — the loggable options shown on the dashboard. */
    val customExercises: List<CustomExercise> = emptyList(),
    /** Every custom exercise incl. archived — used by history to resolve names of past logs. */
    val allCustomExercises: List<CustomExercise> = emptyList()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)

    init {
        viewModelScope.launch {
            // No seeding — every fresh install starts empty at Level 1.
            repo.ensureDecayAnchor(LocalDate.now().toString())
        }
    }

    /** Emits today's date now and again at each local midnight, so an app left open overnight
     *  rolls the dashboard/quests to the new day without needing a DB write to trigger it.
     *  Cold flow: WhileSubscribed also restarts it (re-reading the date) on each resubscribe. */
    private val todayTicker: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis() + 1_000)
        }
    }

    val state: StateFlow<UiState> =
        combine(
            repo.days,
            repo.profile,
            repo.decayAnchor,
            todayTicker,
            combine(repo.favoriteVideoUrls, repo.userVideos, repo.customExercises) {
                fav, uv, custom -> Triple(fav, uv, custom)
            }
        ) { days, profile, anchorStr, today, videoAndCustom ->
            val (favVideos, userVideos, customExercises) = videoAndCustom
            val anchor = anchorStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val todayStr = today.toString()
            val todayEntity = days.firstOrNull { it.date == todayStr }
                ?: WorkoutDayEntity(date = todayStr)
            val rawDays: List<DayData> = days.map { it.toDayData() } +
                if (days.any { it.date == todayStr }) emptyList() else listOf(todayEntity.toDayData())
            // Carry weight + intake forward so unlogged days inherit the last entered values
            // (and each day's energy math uses the body weight in effect then, not just current).
            val dayData = Progression.carryForward(rawDays, profile.weightKg)
            val todayData = dayData.firstOrNull { it.date == today } ?: todayEntity.toDayData()
            // XP = calories only (burn − shortfall + diet); quests/achievements are badges.
            val full = Progression.rebuildFull(dayData, today, anchor, profile)
            UiState(
                loading = false,
                character = full.state,
                today = todayEntity,
                todayData = todayData,
                todayDerived = full.derived[today] ?: DayDerived(0.0, 0L),
                todayConsumed = todayData.caloriesConsumed,
                days = days,
                derivedByDate = full.derived,
                profile = profile,
                achievements = full.achievements,
                quests = Quests.generate(today, todayData, dayData, profile),
                favoriteVideoUrls = favVideos,
                userVideos = userVideos,
                customExercises = customExercises.filterNot { it.archived },
                allCustomExercises = customExercises
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = run {
                val (c, _) = Progression.rebuild(emptyList())
                UiState(
                    loading = true,
                    character = c,
                    today = WorkoutDayEntity(date = LocalDate.now().toString()),
                    todayDerived = DayDerived(0.0, 0L)
                )
            }
        )

    // --- generic per-date mutation -------------------------------------------
    // All read-modify-writes go through Repository.updateDay, which serializes them against
    // each other AND against the passive-sync worker (see the mutex there).
    private fun mutateDay(date: String, transform: (WorkoutDayEntity) -> WorkoutDayEntity) {
        viewModelScope.launch { repo.updateDay(date, transform) }
    }

    private fun applyReps(cur: WorkoutDayEntity, kind: ExerciseKind, delta: Int): WorkoutDayEntity =
        when (kind) {
            ExerciseKind.PUSHUPS -> cur.copy(pushups = (cur.pushups + delta).coerceAtLeast(0))
            ExerciseKind.SQUATS -> cur.copy(squats = (cur.squats + delta).coerceAtLeast(0))
            ExerciseKind.LEG_LIFTS -> cur.copy(legLifts = (cur.legLifts + delta).coerceAtLeast(0))
            ExerciseKind.CALF_RAISES -> cur.copy(calfRaises = (cur.calfRaises + delta).coerceAtLeast(0))
            ExerciseKind.CURLS -> cur.copy(curls = (cur.curls + delta).coerceAtLeast(0))
        }

    private fun todayStr() = LocalDate.now().toString()

    // --- today (Dashboard) ----------------------------------------------------
    fun addReps(kind: ExerciseKind, delta: Int) = mutateDay(todayStr()) { applyReps(it, kind, delta) }

    fun addMiles(delta: Double) = mutateDay(todayStr()) { cur ->
        cur.copy(miles = (cur.miles + delta).coerceAtLeast(0.0))
    }

    fun quickLogFullDay() = mutateDay(todayStr()) { cur ->
        cur.copy(
            pushups = Progression.REP_TARGET,
            squats = Progression.REP_TARGET,
            legLifts = Progression.REP_TARGET,
            calfRaises = Progression.REP_TARGET,
            curls = Progression.REP_TARGET,
            miles = if (cur.miles < Progression.MILE_TARGET) Progression.MILE_TARGET else cur.miles
        )
    }

    /** Record a day's calorie intake. 0 = a deliberate fast (its full deficit counts as XP);
     *  -1 clears the entry back to "not logged" (the day then has no diet XP term). */
    fun setConsumedForDate(date: String, value: Int) = mutateDay(date) { cur ->
        cur.copy(caloriesConsumed = value.coerceAtLeast(-1))
    }

    fun setConsumed(value: Int) = setConsumedForDate(todayStr(), value)

    /** Record a weigh-in (kg) on a day. 0 clears it (the prior weight then carries forward).
     *  Weigh-ins are occasional — the Energy tab stamps today; the Log tab edits any past day. */
    fun setWeightForDate(date: String, weightKg: Double) = mutateDay(date) { cur ->
        cur.copy(weightKg = weightKg.coerceAtLeast(0.0))
    }

    fun setWeightToday(weightKg: Double) = setWeightForDate(todayStr(), weightKg)

    // --- journal: notes (any date) --------------------------------------------
    fun setNotesForDate(date: String, notes: String) =
        mutateDay(date) { it.copy(notes = notes.take(500)) }

    fun setNotesToday(notes: String) = setNotesForDate(todayStr(), notes)

    // --- custom (supplementary) exercises ------------------------------------
    fun addCustomExercise(name: String) { viewModelScope.launch { repo.addCustomExercise(name) } }

    /** Remove from the active list. If it was ever logged, archive it (so history keeps the
     *  name and reps); otherwise hard-delete since there's nothing to preserve. */
    fun removeCustomExercise(id: String) {
        viewModelScope.launch {
            val everLogged = repo.days.first().any { day ->
                (WorkoutDayEntity.decodeCustomReps(day.customReps)[id] ?: 0) > 0
            }
            if (everLogged) repo.archiveCustomExercise(id) else repo.removeCustomExercise(id)
        }
    }

    fun addCustomRepsForDate(date: String, id: String, delta: Int) = mutateDay(date) { cur ->
        val m = WorkoutDayEntity.decodeCustomReps(cur.customReps).toMutableMap()
        val next = ((m[id] ?: 0) + delta).coerceAtLeast(0)
        if (next == 0) m.remove(id) else m[id] = next
        cur.copy(customReps = WorkoutDayEntity.encodeCustomReps(m))
    }

    fun addCustomRepsToday(id: String, delta: Int) = addCustomRepsForDate(todayStr(), id, delta)

    // --- one-off activities (logged to a single day; never an option on other days) ----
    /** Append a one-off (name + calorie estimate + optional distance/reps metrics) to a day.
     *  Stays in that day's history. */
    fun addOneOffForDate(date: String, oneOff: com.mhurston.ascendant.domain.OneOff) = mutateDay(date) { cur ->
        val cleaned = oneOff.name.trim().take(40)
        if (cleaned.isEmpty()) return@mutateDay cur
        val list = WorkoutDayEntity.decodeOneOffs(cur.oneOffs) +
            oneOff.copy(
                name = cleaned,
                kcal = oneOff.kcal.coerceAtLeast(0),
                distanceMi = oneOff.distanceMi.coerceAtLeast(0.0),
                reps = oneOff.reps.coerceAtLeast(0)
            )
        cur.copy(oneOffs = WorkoutDayEntity.encodeOneOffs(list))
    }

    fun addOneOffToday(oneOff: com.mhurston.ascendant.domain.OneOff) = addOneOffForDate(todayStr(), oneOff)

    /** Replace the one-off at [index] on a day with edited values (name + calories + metrics). */
    fun updateOneOffForDate(date: String, index: Int, oneOff: com.mhurston.ascendant.domain.OneOff) =
        mutateDay(date) { cur ->
            val list = WorkoutDayEntity.decodeOneOffs(cur.oneOffs).toMutableList()
            if (index !in list.indices) return@mutateDay cur
            val cleaned = oneOff.name.trim().take(40)
            if (cleaned.isEmpty()) return@mutateDay cur
            list[index] = oneOff.copy(
                name = cleaned,
                kcal = oneOff.kcal.coerceAtLeast(0),
                distanceMi = oneOff.distanceMi.coerceAtLeast(0.0),
                reps = oneOff.reps.coerceAtLeast(0)
            )
            cur.copy(oneOffs = WorkoutDayEntity.encodeOneOffs(list))
        }

    fun updateOneOffToday(index: Int, oneOff: com.mhurston.ascendant.domain.OneOff) =
        updateOneOffForDate(todayStr(), index, oneOff)

    fun removeOneOffForDate(date: String, index: Int) = mutateDay(date) { cur ->
        val list = WorkoutDayEntity.decodeOneOffs(cur.oneOffs).toMutableList()
        if (index in list.indices) list.removeAt(index)
        cur.copy(oneOffs = WorkoutDayEntity.encodeOneOffs(list))
    }

    fun removeOneOffToday(index: Int) = removeOneOffForDate(todayStr(), index)

    /** "Pin" a one-off so it becomes a reusable recurring custom exercise going forward. */
    fun pinOneOffAsExercise(name: String) { viewModelScope.launch { repo.addCustomExercise(name) } }

    // --- push-ups variants (all sum 1:1 toward the push-ups goal) ------------
    /** Add reps to one Push-ups variant. The base "pushups" variant writes its own column;
     *  the alternatives live in the encoded pushVariants column. */
    fun addPushVariantForDate(date: String, variantId: String, delta: Int) = mutateDay(date) { cur ->
        if (variantId == com.mhurston.ascendant.domain.PushExercise.PUSHUPS.id) {
            cur.copy(pushups = (cur.pushups + delta).coerceAtLeast(0))
        } else {
            val m = WorkoutDayEntity.decodeCustomReps(cur.pushVariants).toMutableMap()
            val next = ((m[variantId] ?: 0) + delta).coerceAtLeast(0)
            if (next == 0) m.remove(variantId) else m[variantId] = next
            cur.copy(pushVariants = WorkoutDayEntity.encodeCustomReps(m))
        }
    }

    fun addPushVariantToday(variantId: String, delta: Int) =
        addPushVariantForDate(todayStr(), variantId, delta)

    // --- core variants (Leg Lifts / Sit-ups / High Knees all sum 1:1 toward the core goal) --
    /** Add reps to one Core variant. The base "leglifts" variant writes its own column;
     *  the alternatives live in the encoded coreVariants column. */
    fun addCoreVariantForDate(date: String, variantId: String, delta: Int) = mutateDay(date) { cur ->
        if (variantId == com.mhurston.ascendant.domain.CoreExercise.LEG_LIFTS.id) {
            cur.copy(legLifts = (cur.legLifts + delta).coerceAtLeast(0))
        } else {
            val m = WorkoutDayEntity.decodeCustomReps(cur.coreVariants).toMutableMap()
            val next = ((m[variantId] ?: 0) + delta).coerceAtLeast(0)
            if (next == 0) m.remove(variantId) else m[variantId] = next
            cur.copy(coreVariants = WorkoutDayEntity.encodeCustomReps(m))
        }
    }

    fun addCoreVariantToday(variantId: String, delta: Int) =
        addCoreVariantForDate(todayStr(), variantId, delta)

    // --- time-based extra cardio (bike/swim): minutes → calories → XP, own thing -------------
    fun addCardioMinutesForDate(date: String, activityId: String, deltaMin: Int) = mutateDay(date) { cur ->
        val m = WorkoutDayEntity.decodeCustomReps(cur.cardioMinutes).toMutableMap()
        val next = ((m[activityId] ?: 0) + deltaMin).coerceAtLeast(0)
        if (next == 0) m.remove(activityId) else m[activityId] = next
        cur.copy(cardioMinutes = WorkoutDayEntity.encodeCustomReps(m))
    }

    fun addCardioMinutesToday(activityId: String, deltaMin: Int) =
        addCardioMinutesForDate(todayStr(), activityId, deltaMin)

    fun resetToday() = resetDay(todayStr())

    // --- any date (Calendar / history editing) -------------------------------
    fun addRepsForDate(date: String, kind: ExerciseKind, delta: Int) =
        mutateDay(date) { applyReps(it, kind, delta) }

    fun addMilesForDate(date: String, delta: Double) = mutateDay(date) { cur ->
        cur.copy(miles = (cur.miles + delta).coerceAtLeast(0.0))
    }

    fun resetDay(date: String) {
        viewModelScope.launch { repo.resetDay(date) }
    }

    fun saveProfile(p: Profile) {
        viewModelScope.launch { repo.saveProfile(p) }
    }

    fun resetGoalStart(toWeight: Double) {
        viewModelScope.launch { repo.resetGoalStart(toWeight) }
    }

    val reminderEnabled: StateFlow<Boolean> =
        repo.reminderEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val unitSystem: StateFlow<com.mhurston.ascendant.domain.UnitSystem> =
        repo.unitSystem.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            com.mhurston.ascendant.domain.UnitSystem.IMPERIAL
        )

    fun setUnitSystem(u: com.mhurston.ascendant.domain.UnitSystem) {
        viewModelScope.launch { repo.setUnitSystem(u) }
    }

    val avatar: StateFlow<com.mhurston.ascendant.domain.Avatar> =
        repo.avatar.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            com.mhurston.ascendant.domain.Avatar.MALE
        )

    fun setAvatar(a: com.mhurston.ascendant.domain.Avatar) {
        viewModelScope.launch { repo.setAvatar(a) }
    }

    fun setReminderEnabled(on: Boolean) {
        viewModelScope.launch {
            repo.setReminderEnabled(on)
            val app = getApplication<Application>()
            if (on) com.mhurston.ascendant.notify.Reminders.schedule(app)
            else com.mhurston.ascendant.notify.Reminders.cancel(app)
        }
    }

    // --- passive activity sync (Health Connect) ------------------------------
    val passiveSyncEnabled: StateFlow<Boolean> =
        repo.passiveSyncEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastPassiveSync: StateFlow<String?> =
        repo.lastPassiveSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Toggle passive sync. Enabling schedules the background job and runs an immediate sync;
     *  disabling cancels the job. Caller must have already secured Health Connect permission. */
    fun setPassiveSyncEnabled(on: Boolean) {
        viewModelScope.launch {
            repo.setPassiveSyncEnabled(on)
            val app = getApplication<Application>()
            if (on) {
                com.mhurston.ascendant.health.PassiveSync.schedule(app)
                com.mhurston.ascendant.health.PassiveSync.sync(app)
            } else {
                com.mhurston.ascendant.health.PassiveSync.cancel(app)
            }
        }
    }

    /** Foreground trigger — pull the latest passive totals when the app comes to the front. */
    fun syncPassiveNow() {
        viewModelScope.launch {
            com.mhurston.ascendant.health.PassiveSync.sync(getApplication())
        }
    }

    /** Build the full JSON backup from current state — days + profile + settings (custom
     *  exercises, video favorites/additions, unit system, avatar), so a restore on a fresh
     *  device brings everything back, not just the day log. */
    fun buildBackupJson(): String = com.mhurston.ascendant.data.Exporter.toJson(
        days = state.value.days,
        profile = state.value.profile,
        exportedAt = java.time.LocalDateTime.now().toString(),
        customExercises = state.value.allCustomExercises,
        favoriteVideoUrls = state.value.favoriteVideoUrls,
        userVideos = state.value.userVideos,
        unitSystem = unitSystem.value,
        avatar = avatar.value
    )

    fun importBackupJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = com.mhurston.ascendant.data.Exporter.fromJson(json)
                repo.importBackup(backup)
                onResult(true, "Restored ${backup.days.size} days")
            } catch (e: Exception) {
                onResult(false, "Import failed: not a valid ASCENDANT backup")
            }
        }
    }

    fun toggleFavoriteVideo(url: String) {
        viewModelScope.launch { repo.toggleFavoriteVideo(url) }
    }

    fun addUserVideo(exerciseKey: String, title: String, url: String) {
        viewModelScope.launch { repo.addUserVideo(VideoLink(exerciseKey, title, url, userAdded = true)) }
    }
}
