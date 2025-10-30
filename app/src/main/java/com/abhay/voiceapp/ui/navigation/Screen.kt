package com.abhay.voiceapp.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Recording : Screen("recording/{meetingId}") {
        fun createRoute(meetingId: Long) = "recording/$meetingId"
    }
    object Summary : Screen("summary/{meetingId}") {
        fun createRoute(meetingId: Long) = "summary/$meetingId"
    }
    object Settings : Screen("settings")
}
