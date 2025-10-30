package com.androidvoiceapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.androidvoiceapp.ui.screens.DashboardScreen
import com.androidvoiceapp.ui.screens.RecordingScreen
import com.androidvoiceapp.ui.screens.SettingsScreen
import com.androidvoiceapp.ui.screens.SummaryScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecording = { meetingId ->
                    navController.navigate(Screen.Recording.createRoute(meetingId))
                },
                onNavigateToSummary = { meetingId ->
                    navController.navigate(Screen.Summary.createRoute(meetingId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.Recording.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: -1L
            RecordingScreen(
                meetingId = meetingId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { 
                    navController.navigate(Screen.Summary.createRoute(meetingId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }
        
        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: -1L
            SummaryScreen(
                meetingId = meetingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
