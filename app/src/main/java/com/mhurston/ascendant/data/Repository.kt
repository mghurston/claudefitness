package com.mhurston.ascendant.data

import android.content.Context
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.VideoLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Single entry point to all persistence: workout log (Room) + profile (DataStore). */
class Repository private constructor(
    private val dao: WorkoutDao,
    private val profileStore: ProfileStore
) {
    /** Serializes every read-modify-write of a day row. The UI and the passive-sync worker
     *  both do getDay → copy → upsert; without this lock a sync landing mid-edit (or two fast
     *  taps) could write back a stale row and silently drop logged reps. */
    private val dayWrites = Mutex()
    val days: Flow<List<WorkoutDayEntity>> = dao.observeAll()
    val profile: Flow<Profile> = profileStore.profile
    val decayAnchor: Flow<String?> = profileStore.decayAnchor
    val favoriteVideoUrls: Flow<Set<String>> = profileStore.favoriteVideoUrls
    val userVideos: Flow<List<VideoLink>> = profileStore.userVideos
    val reminderEnabled: Flow<Boolean> = profileStore.reminderEnabled
    val passiveSyncEnabled: Flow<Boolean> = profileStore.passiveSyncEnabled
    val lastPassiveSync: Flow<String?> = profileStore.lastPassiveSync
    val unitSystem: Flow<com.mhurston.ascendant.domain.UnitSystem> = profileStore.unitSystem
    val avatar: Flow<com.mhurston.ascendant.domain.Avatar> = profileStore.avatar
    val customExercises: Flow<List<com.mhurston.ascendant.domain.CustomExercise>> = profileStore.customExercises

    suspend fun setReminderEnabled(on: Boolean) = profileStore.setReminderEnabled(on)
    suspend fun setPassiveSyncEnabled(on: Boolean) = profileStore.setPassiveSyncEnabled(on)
    suspend fun setLastPassiveSync(instant: String) = profileStore.setLastPassiveSync(instant)

    /** Atomically read-modify-write one day under the day-write lock. All mutations of a
     *  day row — user edits and passive banking alike — must go through here. */
    suspend fun updateDay(date: String, transform: (WorkoutDayEntity) -> WorkoutDayEntity) {
        dayWrites.withLock {
            val cur = dao.getDay(date) ?: WorkoutDayEntity(date = date)
            dao.upsert(transform(cur))
        }
    }

    /** Overwrite a day's passive (Health Connect) totals, preserving everything else logged.
     *  Overwrite-not-add: each sync re-reads the authoritative aggregate, so re-syncing the
     *  same day can never double count. */
    suspend fun bankPassive(date: String, steps: Int, kcal: Int) = updateDay(date) { cur ->
        cur.copy(passiveSteps = steps.coerceAtLeast(0), passiveKcal = kcal.coerceAtLeast(0))
    }
    suspend fun setUnitSystem(u: com.mhurston.ascendant.domain.UnitSystem) = profileStore.setUnitSystem(u)
    suspend fun setAvatar(a: com.mhurston.ascendant.domain.Avatar) = profileStore.setAvatar(a)
    suspend fun addCustomExercise(name: String) = profileStore.addCustomExercise(name)
    suspend fun removeCustomExercise(id: String) = profileStore.removeCustomExercise(id)
    suspend fun archiveCustomExercise(id: String) = profileStore.archiveCustomExercise(id)

    suspend fun toggleFavoriteVideo(url: String) = profileStore.toggleFavorite(url)
    suspend fun addUserVideo(v: VideoLink) = profileStore.addUserVideo(v)

    suspend fun getDay(date: String): WorkoutDayEntity? = dao.getDay(date)

    /** Clear everything logged for a day back to zero (for fixing a mis-entry). Passive
     *  Health Connect totals are kept — they're tracked history, not a hand-entry, and the
     *  sync back-window can't restore them for older days. */
    suspend fun resetDay(date: String) = updateDay(date) { cur ->
        WorkoutDayEntity(date = date)
            .copy(passiveSteps = cur.passiveSteps, passiveKcal = cur.passiveKcal)
    }

    suspend fun saveProfile(p: Profile) = profileStore.saveProfile(p)

    suspend fun resetGoalStart(toWeight: Double) = profileStore.resetGoalStart(toWeight)

    /** Restore a backup: upsert all days and (when present) the profile, custom-exercise
     *  definitions, video favorites/additions, unit system, and avatar. Non-day settings are
     *  merged (existing entries kept) so a restore never deletes anything already on-device. */
    suspend fun importBackup(backup: Backup) {
        if (backup.days.isNotEmpty()) dayWrites.withLock { dao.upsertAll(backup.days) }
        backup.profile?.let { profileStore.saveProfile(it) }
        if (backup.customExercises.isNotEmpty()) profileStore.mergeCustomExercises(backup.customExercises)
        if (backup.favoriteVideoUrls.isNotEmpty()) profileStore.mergeFavorites(backup.favoriteVideoUrls)
        if (backup.userVideos.isNotEmpty()) profileStore.mergeUserVideos(backup.userVideos)
        backup.unitSystem?.let { profileStore.setUnitSystem(it) }
        backup.avatar?.let { profileStore.setAvatar(it) }
    }

    suspend fun ensureDecayAnchor(today: String) = profileStore.ensureDecayAnchor(today)

    companion object {
        @Volatile private var INSTANCE: Repository? = null

        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(
                    AppDatabase.get(context).workoutDao(),
                    ProfileStore(context.applicationContext)
                ).also { INSTANCE = it }
            }
    }
}
