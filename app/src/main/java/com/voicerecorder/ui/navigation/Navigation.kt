package com.voicerecorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voicerecorder.ui.dashboard.DashboardScreen
import com.voicerecorder.ui.meeting.MeetingScreen
import com.voicerecorder.ui.settings.SettingsScreen
import com.voicerecorder.ui.summary.SummaryScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Meeting : Screen("meeting/{meetingId}") {
        fun createRoute(meetingId: Long) = "meeting/$meetingId"
    }
    object Summary : Screen("summary/{meetingId}") {
        fun createRoute(meetingId: Long) = "summary/$meetingId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToMeeting = { meetingId ->
                    navController.navigate(Screen.Meeting.createRoute(meetingId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Meeting.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: return@composable
            MeetingScreen(
                meetingId = meetingId,
                onNavigateToSummary = { id ->
                    navController.navigate(Screen.Summary.createRoute(id))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: return@composable
            SummaryScreen(
                meetingId = meetingId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
