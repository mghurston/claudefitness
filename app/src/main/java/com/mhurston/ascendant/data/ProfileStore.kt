package com.mhurston.ascendant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Sex
import com.mhurston.ascendant.domain.VideoLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ascendant_settings")

private const val VIDEO_SEP = "<|>" // delimiter — won't appear in titles/URLs

/** Persists the user's body profile, first-run flags, and video favorites/additions. */
class ProfileStore(private val context: Context) {

    private object Keys {
        val SEX_MALE = booleanPreferencesKey("sex_male")
        val AGE = intPreferencesKey("age")
        val HEIGHT = doublePreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val GOAL_WEIGHT = doublePreferencesKey("goal_weight_kg")
        val START_WEIGHT = doublePreferencesKey("start_weight_kg")
        val SEEDED = booleanPreferencesKey("seeded")
        val DECAY_ANCHOR = stringPreferencesKey("decay_anchor") // ISO date of first app use
        val FAV_VIDEOS = stringSetPreferencesKey("fav_videos")  // favorited URLs
        val USER_VIDEOS = stringSetPreferencesKey("user_videos") // "key<|>title<|>url"
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val AVATAR = stringPreferencesKey("avatar")
        val CUSTOM_EXERCISES = stringSetPreferencesKey("custom_exercises") // "id<|>name"
    }

    // --- Custom (supplementary) exercise definitions --------------------------
    val customExercises: Flow<List<com.mhurston.ascendant.domain.CustomExercise>> =
        context.dataStore.data.map { p ->
            (p[Keys.CUSTOM_EXERCISES] ?: emptySet()).mapNotNull { encoded ->
                val parts = encoded.split(VIDEO_SEP)
                if (parts.size == 2) com.mhurston.ascendant.domain.CustomExercise(parts[0], parts[1]) else null
            }.sortedBy { it.name.lowercase() }
        }

    suspend fun addCustomExercise(name: String) {
        val clean = name.trim().take(40)
        if (clean.isBlank()) return
        val id = "c${System.currentTimeMillis()}"
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.CUSTOM_EXERCISES] ?: emptySet()
            prefs[Keys.CUSTOM_EXERCISES] = cur + "$id$VIDEO_SEP$clean"
        }
    }

    suspend fun removeCustomExercise(id: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.CUSTOM_EXERCISES] ?: emptySet()
            prefs[Keys.CUSTOM_EXERCISES] = cur.filterNot { it.substringBefore(VIDEO_SEP) == id }.toSet()
        }
    }

    val avatar: Flow<com.mhurston.ascendant.domain.Avatar> = context.dataStore.data.map {
        runCatching { com.mhurston.ascendant.domain.Avatar.valueOf(it[Keys.AVATAR] ?: "MALE") }
            .getOrDefault(com.mhurston.ascendant.domain.Avatar.MALE)
    }

    suspend fun setAvatar(a: com.mhurston.ascendant.domain.Avatar) {
        context.dataStore.edit { it[Keys.AVATAR] = a.name }
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMINDER_ON] ?: false }

    suspend fun setReminderEnabled(on: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_ON] = on }
    }

    // Default to IMPERIAL (US units) unless the user switches to metric.
    val unitSystem: Flow<com.mhurston.ascendant.domain.UnitSystem> =
        context.dataStore.data.map {
            runCatching { com.mhurston.ascendant.domain.UnitSystem.valueOf(it[Keys.UNIT_SYSTEM] ?: "IMPERIAL") }
                .getOrDefault(com.mhurston.ascendant.domain.UnitSystem.IMPERIAL)
        }

    suspend fun setUnitSystem(u: com.mhurston.ascendant.domain.UnitSystem) {
        context.dataStore.edit { it[Keys.UNIT_SYSTEM] = u.name }
    }

    val profile: Flow<Profile> = context.dataStore.data.map { p ->
        Profile(
            sex = if (p[Keys.SEX_MALE] != false) Sex.MALE else Sex.FEMALE,
            age = p[Keys.AGE] ?: 30,
            heightCm = p[Keys.HEIGHT] ?: 178.0,
            weightKg = p[Keys.WEIGHT] ?: 80.0,
            goalWeightKg = p[Keys.GOAL_WEIGHT] ?: 0.0,
            startWeightKg = p[Keys.START_WEIGHT] ?: 0.0
        )
    }

    val seeded: Flow<Boolean> = context.dataStore.data.map { it[Keys.SEEDED] ?: false }

    /** The date inactivity decay starts counting from (first app use). */
    val decayAnchor: Flow<String?> = context.dataStore.data.map { it[Keys.DECAY_ANCHOR] }

    suspend fun ensureDecayAnchor(today: String) {
        context.dataStore.edit {
            if (it[Keys.DECAY_ANCHOR] == null) it[Keys.DECAY_ANCHOR] = today
        }
    }

    suspend fun saveProfile(p: Profile) {
        context.dataStore.edit {
            it[Keys.SEX_MALE] = p.sex == Sex.MALE
            it[Keys.AGE] = p.age
            it[Keys.HEIGHT] = p.heightCm
            it[Keys.WEIGHT] = p.weightKg
            it[Keys.GOAL_WEIGHT] = p.goalWeightKg
            // Capture the starting weight the first time a goal is set so progress has an anchor.
            val existingStart = it[Keys.START_WEIGHT] ?: 0.0
            it[Keys.START_WEIGHT] = when {
                p.startWeightKg > 0.0 -> p.startWeightKg
                p.goalWeightKg > 0.0 && existingStart <= 0.0 -> p.weightKg
                else -> existingStart
            }
        }
    }

    suspend fun resetGoalStart(toWeight: Double) {
        context.dataStore.edit { it[Keys.START_WEIGHT] = toWeight }
    }

    suspend fun markSeeded() {
        context.dataStore.edit { it[Keys.SEEDED] = true }
    }

    // --- Videos: favorites + user-added ---------------------------------------
    val favoriteVideoUrls: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAV_VIDEOS] ?: emptySet() }

    val userVideos: Flow<List<VideoLink>> = context.dataStore.data.map { p ->
        (p[Keys.USER_VIDEOS] ?: emptySet()).mapNotNull { encoded ->
            val parts = encoded.split(VIDEO_SEP)
            if (parts.size == 3) VideoLink(parts[0], parts[1], parts[2], userAdded = true) else null
        }
    }

    suspend fun toggleFavorite(url: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.FAV_VIDEOS] ?: emptySet()
            prefs[Keys.FAV_VIDEOS] = if (url in cur) cur - url else cur + url
        }
    }

    suspend fun addUserVideo(v: VideoLink) {
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.USER_VIDEOS] ?: emptySet()
            prefs[Keys.USER_VIDEOS] = cur + "${v.exerciseKey}$VIDEO_SEP${v.title}$VIDEO_SEP${v.url}"
        }
    }
}
