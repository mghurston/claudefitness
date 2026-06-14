package com.mhurston.ascendant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mhurston.ascendant.data.Repository
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.AchStatus
import com.mhurston.ascendant.domain.Achievements
import com.mhurston.ascendant.domain.CharacterState
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.DayDerived
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import com.mhurston.ascendant.domain.Quest
import com.mhurston.ascendant.domain.Quests
import com.mhurston.ascendant.domain.VideoLink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ExerciseKind { PUSHUPS, SQUATS, LEG_LIFTS, CALF_RAISES, CURLS }

data class UiState(
    val loading: Boolean = true,
    val character: CharacterState,
    val today: WorkoutDayEntity,
    val todayDerived: DayDerived,
    val days: List<WorkoutDayEntity> = emptyList(),
    val derivedByDate: Map<LocalDate, DayDerived> = emptyMap(),
    val profile: Profile = Profile(),
    val achievements: List<AchStatus> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val favoriteVideoUrls: Set<String> = emptySet(),
    val userVideos: List<VideoLink> = emptyList()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)

    init {
        viewModelScope.launch {
            // No seeding — every fresh install starts empty at Level 1.
            repo.ensureDecayAnchor(LocalDate.now().toString())
        }
    }

    val state: StateFlow<UiState> =
        combine(
            repo.days, repo.profile, repo.decayAnchor, repo.favoriteVideoUrls, repo.userVideos
        ) { days, profile, anchorStr, favVideos, userVideos ->
            val today = LocalDate.now()
            val anchor = anchorStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val todayStr = today.toString()
            val todayEntity = days.firstOrNull { it.date == todayStr }
                ?: WorkoutDayEntity(date = todayStr)
            val dayData: List<DayData> = days.map { it.toDayData() } +
                if (days.any { it.date == todayStr }) emptyList() else listOf(todayEntity.toDayData())
            val (character, derived) = Progression.rebuild(dayData, today, anchor)
            UiState(
                loading = false,
                character = character,
                today = todayEntity,
                todayDerived = derived[today] ?: DayDerived(0.0, 0L),
                days = days,
                derivedByDate = derived,
                profile = profile,
                achievements = Achievements.evaluate(dayData, character),
                quests = Quests.generate(today, todayEntity.toDayData(), dayData),
                favoriteVideoUrls = favVideos,
                userVideos = userVideos
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
    private fun mutateDay(date: String, transform: (WorkoutDayEntity) -> WorkoutDayEntity) {
        viewModelScope.launch {
            val cur = repo.getDay(date) ?: WorkoutDayEntity(date = date)
            repo.saveDay(transform(cur))
        }
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

    fun setConsumed(value: Int) = mutateDay(todayStr()) { cur ->
        cur.copy(caloriesConsumed = value.coerceAtLeast(0))
    }

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
