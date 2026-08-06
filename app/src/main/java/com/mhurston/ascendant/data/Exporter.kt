package com.mhurston.ascendant.data

import com.mhurston.ascendant.domain.Avatar
import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.CardioIntensity
import com.mhurston.ascendant.domain.CardioMode
import com.mhurston.ascendant.domain.CardioRate
import com.mhurston.ascendant.domain.CustomExercise
import com.mhurston.ascendant.domain.ExerciseGoal
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import com.mhurston.ascendant.domain.Sex
import com.mhurston.ascendant.domain.UnitSystem
import com.mhurston.ascendant.domain.VideoLink
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Everything a JSON backup can carry. Settings fields are optional so older backups
 *  (schema 1: days + profile only) still restore cleanly. */
data class Backup(
    val days: List<WorkoutDayEntity>,
    val profile: Profile?,
    val customExercises: List<CustomExercise> = emptyList(),
    val favoriteVideoUrls: Set<String> = emptySet(),
    val userVideos: List<VideoLink> = emptyList(),
    val unitSystem: UnitSystem? = null,
    val avatar: Avatar? = null
)

/** Resolve an enum by name, falling back when the value is absent (older backup) or unknown. */
private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

/** Produces and parses export payloads: CSV (spreadsheet layout) and a full JSON backup. */
object Exporter {

    /** Backup format version. 1 = days + profile, caloriesConsumed 0 meant "not logged".
     *  2 = adds settings (customs/videos/unit/avatar); caloriesConsumed -1 = not logged,
     *  0 = a logged fasting day. 3 = custom exercises carry a "goal" (the daily goal their
     *  reps fill); a missing goal reads as NONE, so schema 1-2 backups restore unchanged.
     *  4 = goals are the four training categories rather than one per exercise slot (a schema-3
     *  PUSH/CURLS reads as UPPER, SQUATS/CALF_RAISES as LOWER), and days carry "customDistance"
     *  for Cardio-assigned customs; a missing value is "" = none logged.
     *  5 = Cardio exercises carry how they are measured ("cardioMode"/"cardioRate"/
     *  "cardioIntensity") and days carry "customMinutes" for the time-logged ones. Missing
     *  values take their defaults (distance, walking pace), so older backups restore unchanged. */
    private const val SCHEMA = 5

    /** Original spreadsheet column order, so the file round-trips back to the sheet
     *  (steps/totalmiles/cardiokcal are appended after so the first eight columns stay put).
     *  `miles` is the manual/treadmill entry only; `totalmiles` = miles + step-tracked
     *  distance — the walking total. `cardiokcal` is what the cardio sixth of completion
     *  actually scored, since that goal is calories rather than miles.
     *  Locale-pinned: a comma-decimal device locale must not corrupt the CSV. */
    fun toCsv(
        days: List<WorkoutDayEntity>,
        specs: Map<String, CustomExercise> = emptyMap(),
        profile: Profile = Profile()
    ): String {
        val sb = StringBuilder()
        sb.append("date,pushups,squats,leglifts,calfraises,curls,miles,completion,steps," +
            "totalmiles,cardiokcal,cardiotarget\n")
        days.sortedBy { it.date }.forEach { d ->
            // Read every column off the scored day, so the CSV shows exactly the numbers
            // completion was computed from (customs assigned to a category included).
            val scored = d.toDayData(specs)
            val comp = Progression.completion(scored, profile)
            sb.append("${d.date},${scored.pushups},${scored.squats},")
            sb.append("${scored.legLifts},")
            sb.append("${scored.calfRaises},${scored.curls},${scored.miles},")
            sb.append(String.format(Locale.US, "%.4f", comp)).append(",")
            sb.append("${d.passiveSteps},")
            sb.append(String.format(Locale.US, "%.2f", scored.walkMiles)).append(",")
            sb.append(Math.round(Calories.cardioKcal(profile, scored))).append(",")
            sb.append(Math.round(Calories.cardioTarget(profile, scored))).append("\n")
        }
        return sb.toString()
    }

    /** Full backup of every field + profile + settings. Hand-built JSON (no extra deps). */
    fun toJson(
        days: List<WorkoutDayEntity>,
        profile: Profile,
        exportedAt: String,
        customExercises: List<CustomExercise> = emptyList(),
        favoriteVideoUrls: Set<String> = emptySet(),
        userVideos: List<VideoLink> = emptyList(),
        unitSystem: UnitSystem? = null,
        avatar: Avatar? = null
    ): String {
        // Escape JSON specials AND control chars (the oneOffs column uses U+001F/U+001E
        // delimiters, which are illegal raw in JSON and must be \u-escaped).
        fun esc(s: String) = buildString {
            s.forEach { c ->
                when {
                    c == '\\' -> append("\\\\")
                    c == '"' -> append("\\\"")
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c < ' ' -> append("\\u%04x".format(Locale.US, c.code))
                    else -> append(c)
                }
            }
        }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"ASCENDANT\",\n")
        sb.append("  \"schema\": $SCHEMA,\n")
        sb.append("  \"exportedAt\": \"${esc(exportedAt)}\",\n")
        sb.append("  \"profile\": {")
        sb.append("\"sex\": \"${profile.sex}\", \"age\": ${profile.age}, ")
        sb.append("\"heightCm\": ${profile.heightCm}, \"weightKg\": ${profile.weightKg}, ")
        sb.append("\"goalWeightKg\": ${profile.goalWeightKg}, \"startWeightKg\": ${profile.startWeightKg}},\n")
        unitSystem?.let { sb.append("  \"unitSystem\": \"${it.name}\",\n") }
        avatar?.let { sb.append("  \"avatar\": \"${it.name}\",\n") }
        if (customExercises.isNotEmpty()) {
            sb.append("  \"customExercises\": [\n")
            customExercises.forEachIndexed { i, ex ->
                sb.append("    {\"id\": \"${esc(ex.id)}\", \"name\": \"${esc(ex.name)}\", ")
                sb.append("\"archived\": ${ex.archived}, \"goal\": \"${ex.goal.name}\", ")
                sb.append("\"cardioMode\": \"${ex.cardioMode.name}\", ")
                sb.append("\"cardioRate\": \"${ex.cardioRate.name}\", ")
                sb.append("\"cardioIntensity\": \"${ex.cardioIntensity.name}\"}")
                sb.append(if (i < customExercises.lastIndex) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }
        if (favoriteVideoUrls.isNotEmpty()) {
            sb.append("  \"favoriteVideos\": [")
            sb.append(favoriteVideoUrls.joinToString(", ") { "\"${esc(it)}\"" })
            sb.append("],\n")
        }
        if (userVideos.isNotEmpty()) {
            sb.append("  \"userVideos\": [\n")
            userVideos.forEachIndexed { i, v ->
                sb.append("    {\"exerciseKey\": \"${esc(v.exerciseKey)}\", ")
                sb.append("\"title\": \"${esc(v.title)}\", \"url\": \"${esc(v.url)}\"}")
                sb.append(if (i < userVideos.lastIndex) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }
        sb.append("  \"days\": [\n")
        val sorted = days.sortedBy { it.date }
        sorted.forEachIndexed { i, d ->
            sb.append("    {")
            sb.append("\"date\": \"${d.date}\", ")
            sb.append("\"pushups\": ${d.pushups}, \"squats\": ${d.squats}, ")
            sb.append("\"legLifts\": ${d.legLifts}, \"calfRaises\": ${d.calfRaises}, ")
            sb.append("\"curls\": ${d.curls}, \"miles\": ${d.miles}, ")
            sb.append("\"caloriesConsumed\": ${d.caloriesConsumed}, ")
            sb.append("\"weightKg\": ${d.weightKg}, ")
            sb.append("\"isRestDay\": ${d.isRestDay}, ")
            sb.append("\"mood\": ${d.mood}, ")
            sb.append("\"customReps\": \"${esc(d.customReps)}\", ")
            sb.append("\"pushVariants\": \"${esc(d.pushVariants)}\", ")
            sb.append("\"coreVariants\": \"${esc(d.coreVariants)}\", ")
            sb.append("\"cardioMinutes\": \"${esc(d.cardioMinutes)}\", ")
            sb.append("\"customDistance\": \"${esc(d.customDistance)}\", ")
            sb.append("\"customMinutes\": \"${esc(d.customMinutes)}\", ")
            sb.append("\"oneOffs\": \"${esc(d.oneOffs)}\", ")
            sb.append("\"passiveSteps\": ${d.passiveSteps}, ")
            sb.append("\"passiveKcal\": ${d.passiveKcal}, ")
            sb.append("\"notes\": \"${esc(d.notes)}\"}")
            sb.append(if (i < sorted.lastIndex) ",\n" else "\n")
        }
        sb.append("  ]\n}\n")
        return sb.toString()
    }

    /** Parse a previously exported JSON backup (any schema). Throws on malformed input. */
    fun fromJson(text: String): Backup {
        val root = JSONObject(text)
        val schema = root.optInt("schema", 1)
        val profile = root.optJSONObject("profile")?.let { p ->
            Profile(
                sex = runCatching { Sex.valueOf(p.optString("sex", "MALE")) }.getOrDefault(Sex.MALE),
                age = p.optInt("age", 30),
                heightCm = p.optDouble("heightCm", 178.0),
                weightKg = p.optDouble("weightKg", 80.0),
                goalWeightKg = p.optDouble("goalWeightKg", 0.0),
                startWeightKg = p.optDouble("startWeightKg", 0.0)
            )
        }
        val arr = root.optJSONArray("days")
        val days = buildList {
            if (arr != null) for (i in 0 until arr.length()) {
                val d = arr.getJSONObject(i)
                // Schema 1 wrote 0 for "no food logged"; that's -1 in the current model
                // (0 now means a logged fasting day).
                val rawConsumed = d.optInt("caloriesConsumed", -1)
                val consumed = if (schema < 2 && rawConsumed == 0) -1 else rawConsumed
                add(
                    WorkoutDayEntity(
                        date = d.getString("date"),
                        pushups = d.optInt("pushups"),
                        squats = d.optInt("squats"),
                        legLifts = d.optInt("legLifts"),
                        calfRaises = d.optInt("calfRaises"),
                        curls = d.optInt("curls"),
                        miles = d.optDouble("miles", 0.0),
                        caloriesConsumed = consumed,
                        weightKg = d.optDouble("weightKg", 0.0),
                        isRestDay = d.optBoolean("isRestDay", false),
                        notes = d.optString("notes", ""),
                        mood = d.optInt("mood", 0),
                        customReps = d.optString("customReps", ""),
                        pushVariants = d.optString("pushVariants", ""),
                        coreVariants = d.optString("coreVariants", ""),
                        cardioMinutes = d.optString("cardioMinutes", ""),
                        customDistance = d.optString("customDistance", ""),
                        customMinutes = d.optString("customMinutes", ""),
                        oneOffs = d.optString("oneOffs", ""),
                        passiveSteps = d.optInt("passiveSteps", 0),
                        passiveKcal = d.optInt("passiveKcal", 0)
                    )
                )
            }
        }
        val customs = root.optJSONArray("customExercises").toObjectList { o ->
            CustomExercise(
                id = o.optString("id", ""),
                name = o.optString("name", ""),
                archived = o.optBoolean("archived", false),
                // Absent in schema ≤ 2 backups → NONE, i.e. the old calories-only behavior.
                goal = ExerciseGoal.forName(o.optString("goal", "")),
                // Absent in schema ≤ 4 → the defaults: distance, at walking pace.
                cardioMode = enumOrDefault(o.optString("cardioMode", ""), CardioMode.DISTANCE),
                cardioRate = enumOrDefault(o.optString("cardioRate", ""), CardioRate.WALK),
                cardioIntensity =
                    enumOrDefault(o.optString("cardioIntensity", ""), CardioIntensity.MODERATE)
            )
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
        val favorites = buildSet {
            val fav = root.optJSONArray("favoriteVideos")
            if (fav != null) for (i in 0 until fav.length()) {
                fav.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
        val userVideos = root.optJSONArray("userVideos").toObjectList { o ->
            VideoLink(
                exerciseKey = o.optString("exerciseKey", ""),
                title = o.optString("title", ""),
                url = o.optString("url", ""),
                userAdded = true
            )
        }.filter { it.exerciseKey.isNotBlank() && it.url.isNotBlank() }
        val unit = root.optString("unitSystem", "")
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
        val avatar = root.optString("avatar", "")
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Avatar.valueOf(it) }.getOrNull() }
        return Backup(days, profile, customs, favorites, userVideos, unit, avatar)
    }

    private fun <T> JSONArray?.toObjectList(map: (JSONObject) -> T): List<T> = buildList {
        if (this@toObjectList != null) for (i in 0 until this@toObjectList.length()) {
            this@toObjectList.optJSONObject(i)?.let { add(map(it)) }
        }
    }
}
