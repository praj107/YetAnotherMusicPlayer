package com.yamp.recommendation

data class ScoringWeights(
    val playCount: Float = 0.25f,
    val completionRate: Float = 0.20f,
    val timeOfDayMatch: Float = 0.20f,
    val genreAffinity: Float = 0.20f,
    val recency: Float = 0.15f
)
