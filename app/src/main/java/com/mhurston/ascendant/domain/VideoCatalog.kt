package com.mhurston.ascendant.domain

import java.net.URLEncoder

/**
 * Per-exercise form-video links. Per the design (Video Integration.md), v1 uses
 * YouTube *search* deep-links from stable query seeds — no API key, no quota, never
 * goes stale, and respects creator monetization (views happen on YouTube). The user
 * can also add their own video URLs.
 */
data class VideoLink(
    val exerciseKey: String,
    val title: String,
    val url: String,
    val userAdded: Boolean = false
)

object VideoCatalog {

    // Keys match the enum ids in Models.kt (PushExercise/CoreExercise/CardioActivity)
    // so a variant row can open its own videos with no mapping layer.
    val EXERCISE_KEYS = listOf(
        "pushups", "db_chest_fly", "upward_chest_fly", "pull_ups", "chin_ups", "dips",
        "leglifts", "situps", "high_knees",
        "squats", "calfraises", "curls",
        "walking", "bike", "swim"
    )

    fun displayName(key: String): String = when (key) {
        "pushups" -> "Push-ups"
        "db_chest_fly" -> "Standing Dumbbell Chest Fly"
        "upward_chest_fly" -> "Standing Upward Chest Fly"
        "pull_ups" -> "Pull Ups"
        "chin_ups" -> "Chin Ups"
        "dips" -> "Dips"
        "leglifts" -> "Leg Lifts"
        "situps" -> "Sit-ups"
        "high_knees" -> "High Knees"
        "squats" -> "Squats"
        "calfraises" -> "Calf Raises"
        "curls" -> "Curls"
        "walking" -> "Walking"
        "bike" -> "Bike Riding"
        "swim" -> "Swimming"
        else -> key
    }

    private fun search(query: String): String =
        "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")

    private val seeds: Map<String, List<Pair<String, String>>> = mapOf(
        "pushups" to listOf(
            "Perfect Push-up Form" to "perfect push up form",
            "Push-up Variations" to "push up variations tutorial",
            "100 Push-ups Follow Along" to "100 push ups challenge follow along",
            "Knee → Full Push-up Progression" to "push up progression beginner"
        ),
        "squats" to listOf(
            "Bodyweight Squat Form" to "bodyweight squat proper form",
            "Knee Tracking & Depth" to "squat depth knee tracking cue",
            "Squat Variations" to "bodyweight squat variations",
            "Daily Squat Routine" to "daily squat routine follow along"
        ),
        "leglifts" to listOf(
            "Lying Leg Raise Form" to "lying leg raise form",
            "Hanging Leg Raise" to "hanging leg raise tutorial",
            "Core Engagement Cues" to "leg raise core engagement cue",
            "Ab Routine w/ Leg Lifts" to "ab routine leg lifts follow along"
        ),
        "calfraises" to listOf(
            "Standing Calf Raise Form" to "standing calf raise proper form",
            "Single-Leg Progression" to "single leg calf raise progression",
            "Calf Endurance Sets" to "calf raise endurance workout"
        ),
        "curls" to listOf(
            "Bicep Curl Form" to "bicep curl proper form dumbbell",
            "Avoid Swinging" to "bicep curl avoid swinging strict form",
            "Resistance-Band Curls" to "resistance band bicep curl"
        ),
        "walking" to listOf(
            "Indoor Walking Workout" to "indoor walking workout follow along",
            "Brisk Walk for Fitness" to "brisk walking for fitness tips",
            "Walking Pace & Posture" to "walking posture pace tips"
        ),
        "db_chest_fly" to listOf(
            "Standing Chest Fly Form" to "standing dumbbell chest fly proper form",
            "Standing Fly Mistakes" to "standing chest fly common mistakes",
            "Chest Fly Follow Along" to "standing dumbbell chest fly workout"
        ),
        "upward_chest_fly" to listOf(
            "Upward Chest Fly Form" to "standing upward dumbbell chest fly form",
            "Low-to-High Fly Tutorial" to "low to high dumbbell fly standing tutorial",
            "Upper Chest at Home" to "upper chest dumbbell exercises standing"
        ),
        "pull_ups" to listOf(
            "Perfect Pull-up Form" to "perfect pull up form",
            "First Pull-up Progression" to "pull up progression beginner cannot do one",
            "Grip & Scapula Cues" to "pull up scapular engagement cue",
            "Pull-up Follow Along" to "pull up workout follow along"
        ),
        "chin_ups" to listOf(
            "Chin-up Form" to "chin up proper form",
            "Chin-up vs Pull-up" to "chin up vs pull up difference",
            "Chin-up Progression" to "chin up progression beginner"
        ),
        "dips" to listOf(
            "Dip Form Basics" to "dips proper form tutorial",
            "Chair / Bench Dips" to "chair dips at home tutorial",
            "Dip Progression" to "dip progression beginner",
            "Shoulder-Safe Dips" to "dips shoulder pain safe form"
        ),
        "situps" to listOf(
            "Sit-up Form" to "sit up proper form",
            "Neck-Safe Sit-ups" to "sit ups without neck strain",
            "Sit-up Follow Along" to "sit up workout follow along"
        ),
        "high_knees" to listOf(
            "High Knees Form" to "high knees proper form",
            "High Knees Cardio" to "high knees cardio workout follow along",
            "Low-Impact Alternative" to "low impact high knees alternative"
        ),
        "bike" to listOf(
            "Cycling Posture Basics" to "cycling posture tips beginner",
            "Indoor Cycling Workout" to "indoor cycling workout follow along",
            "Bike Fit at Home" to "bike seat height fit basics"
        ),
        "swim" to listOf(
            "Freestyle Technique" to "freestyle swimming technique beginner",
            "Breathing Basics" to "freestyle breathing technique swimming",
            "Lap Swim Workout" to "beginner lap swimming workout"
        )
    )

    fun defaultsFor(key: String): List<VideoLink> =
        seeds[key].orEmpty().map { (title, q) -> VideoLink(key, title, search(q)) }
}
