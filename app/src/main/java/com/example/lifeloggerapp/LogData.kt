package com.example.lifeloggerapp

import androidx.compose.runtime.mutableStateListOf

// 1. The Data Model
data class LifeLog(
    val title: String,
    val note: String,
    val mood: String,
    val tag: String,
    val date: String = "APRIL 23",
    val time: String = "10:40 PM",
    val imageUri: String? = null // Added this so your images work later too!
)

// 2. The Global List (This stays alive while the app is running)
val GlobalLogs = mutableStateListOf<LifeLog>(
    LifeLog("Morning Reflection", "Started the day with a cold brew...", "😊", "Personal"),
    LifeLog("Lunch at the park", "Had a great salad. Weather was perfect.", "🤩", "Event")
)