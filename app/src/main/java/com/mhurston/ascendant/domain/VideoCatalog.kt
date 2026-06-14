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

    val EXERCISE_KEYS = listOf("pushups", "squats", "leglifts", "calfraises", "curls", "walking")

    fun displayName(key: String): String = when (key) {
        "pushups" -> "Push-ups"
        "squats" -> "Squats"
        "leglifts" -> "Leg Lifts"
        "calfraises" -> "Calf Raises"
        "curls" -> "Curls"
        "walking" -> "Walking"
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
        )
    )

    fun defaultsFor(key: String): List<VideoLink> =
        seeds[key].orEmpty().map { (title, q) -> VideoLink(key, title, search(q)) }
}
