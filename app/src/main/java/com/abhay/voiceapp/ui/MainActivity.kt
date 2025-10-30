package com.abhay.voiceapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abhay.voiceapp.ui.navigation.Screen
import com.abhay.voiceapp.ui.screen.*
import com.abhay.voiceapp.ui.theme.AndroidVoiceAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidVoiceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceAppNavigation()
                }
            }
        }
    }
}

@Composable
fun VoiceAppNavigation() {
    val navController = rememberNavController()
    
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
                },
                onStartNewRecording = {
                    // Create new meeting with ID -1 (will be created in RecordingScreen)
                    navController.navigate(Screen.Recording.createRoute(-1))
                }
            )
        }
        
        composable(
            route = Screen.Recording.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: -1
            RecordingScreen(
                meetingId = meetingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: -1
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
