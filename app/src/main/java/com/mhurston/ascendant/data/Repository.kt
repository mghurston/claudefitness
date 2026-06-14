package com.mhurston.ascendant.data

import android.content.Context
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.VideoLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Single entry point to all persistence: workout log (Room) + profile (DataStore). */
class Repository private constructor(
    private val dao: WorkoutDao,
    private val profileStore: ProfileStore
) {
    val days: Flow<List<WorkoutDayEntity>> = dao.observeAll()
    val profile: Flow<Profile> = profileStore.profile
    val decayAnchor: Flow<String?> = profileStore.decayAnchor
    val favoriteVideoUrls: Flow<Set<String>> = profileStore.favoriteVideoUrls
    val userVideos: Flow<List<VideoLink>> = profileStore.userVideos
    val reminderEnabled: Flow<Boolean> = profileStore.reminderEnabled
    val unitSystem: Flow<com.mhurston.ascendant.domain.UnitSystem> = profileStore.unitSystem
    val avatar: Flow<com.mhurston.ascendant.domain.Avatar> = profileStore.avatar

    suspend fun setReminderEnabled(on: Boolean) = profileStore.setReminderEnabled(on)
    suspend fun setUnitSystem(u: com.mhurston.ascendant.domain.UnitSystem) = profileStore.setUnitSystem(u)
    suspend fun setAvatar(a: com.mhurston.ascendant.domain.Avatar) = profileStore.setAvatar(a)

    suspend fun toggleFavoriteVideo(url: String) = profileStore.toggleFavorite(url)
    suspend fun addUserVideo(v: VideoLink) = profileStore.addUserVideo(v)

    suspend fun getDay(date: String): WorkoutDayEntity? = dao.getDay(date)

    suspend fun saveDay(day: WorkoutDayEntity) = dao.upsert(day)

    /** Clear everything logged for a day back to zero (for fixing a mis-entry). */
    suspend fun resetDay(date: String) {
        dao.upsert(WorkoutDayEntity(date = date))
    }

    suspend fun saveProfile(p: Profile) = profileStore.saveProfile(p)

    suspend fun resetGoalStart(toWeight: Double) = profileStore.resetGoalStart(toWeight)

    /** Restore a backup: upsert all days and (optionally) the profile. */
    suspend fun importBackup(backup: Backup) {
        if (backup.days.isNotEmpty()) dao.upsertAll(backup.days)
        backup.profile?.let { profileStore.saveProfile(it) }
    }

    suspend fun ensureDecayAnchor(today: String) = profileStore.ensureDecayAnchor(today)

    /** Import the 30-day baseline exactly once, on first launch. */
    suspend fun seedIfNeeded() {
        val alreadySeeded = profileStore.seeded.first()
        if (!alreadySeeded && dao.count() == 0) {
            dao.upsertAll(SeedData.entities())
            profileStore.markSeeded()
        }
    }

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
