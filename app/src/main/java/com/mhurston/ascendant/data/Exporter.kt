package com.mhurston.ascendant.data

import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import com.mhurston.ascendant.domain.Sex
import org.json.JSONObject

data class Backup(val days: List<WorkoutDayEntity>, val profile: Profile?)

/** Produces and parses export payloads: CSV (spreadsheet layout) and a full JSON backup. */
object Exporter {

    /** Original spreadsheet column order, so the file round-trips back to the sheet. */
    fun toCsv(days: List<WorkoutDayEntity>): String {
        val sb = StringBuilder()
        sb.append("date,pushups,squats,leglifts,calfraises,curls,miles,completion\n")
        days.sortedBy { it.date }.forEach { d ->
            val comp = Progression.completion(d.toDayData())
            sb.append("${d.date},${d.pushups},${d.squats},${d.legLifts},")
            sb.append("${d.calfRaises},${d.curls},${d.miles},")
            sb.append(String.format("%.4f", comp)).append("\n")
        }
        return sb.toString()
    }

    /** Full backup of every field + profile. Hand-built JSON (no extra deps). */
    fun toJson(days: List<WorkoutDayEntity>, profile: Profile, exportedAt: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"ASCENDANT\",\n")
        sb.append("  \"exportedAt\": \"${esc(exportedAt)}\",\n")
        sb.append("  \"profile\": {")
        sb.append("\"sex\": \"${profile.sex}\", \"age\": ${profile.age}, ")
        sb.append("\"heightCm\": ${profile.heightCm}, \"weightKg\": ${profile.weightKg}, ")
        sb.append("\"goalWeightKg\": ${profile.goalWeightKg}, \"startWeightKg\": ${profile.startWeightKg}},\n")
        sb.append("  \"days\": [\n")
        val sorted = days.sortedBy { it.date }
        sorted.forEachIndexed { i, d ->
            sb.append("    {")
            sb.append("\"date\": \"${d.date}\", ")
            sb.append("\"pushups\": ${d.pushups}, \"squats\": ${d.squats}, ")
            sb.append("\"legLifts\": ${d.legLifts}, \"calfRaises\": ${d.calfRaises}, ")
            sb.append("\"curls\": ${d.curls}, \"miles\": ${d.miles}, ")
            sb.append("\"caloriesConsumed\": ${d.caloriesConsumed}, ")
            sb.append("\"isRestDay\": ${d.isRestDay}, ")
            sb.append("\"mood\": ${d.mood}, ")
            sb.append("\"customReps\": \"${esc(d.customReps)}\", ")
            sb.append("\"notes\": \"${esc(d.notes)}\"}")
            sb.append(if (i < sorted.lastIndex) ",\n" else "\n")
        }
        sb.append("  ]\n}\n")
        return sb.toString()
    }

    /** Parse a previously exported JSON backup. Throws on malformed input. */
    fun fromJson(text: String): Backup {
        val root = JSONObject(text)
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
                add(
                    WorkoutDayEntity(
                        date = d.getString("date"),
                        pushups = d.optInt("pushups"),
                        squats = d.optInt("squats"),
                        legLifts = d.optInt("legLifts"),
                        calfRaises = d.optInt("calfRaises"),
                        curls = d.optInt("curls"),
                        miles = d.optDouble("miles", 0.0),
                        caloriesConsumed = d.optInt("caloriesConsumed"),
                        isRestDay = d.optBoolean("isRestDay", false),
                        notes = d.optString("notes", ""),
                        mood = d.optInt("mood", 0),
                        customReps = d.optString("customReps", "")
                    )
                )
            }
        }
        return Backup(days, profile)
    }
}
