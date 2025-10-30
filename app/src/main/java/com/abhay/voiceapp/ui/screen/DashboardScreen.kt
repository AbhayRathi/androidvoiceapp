package com.abhay.voiceapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhay.voiceapp.R
import com.abhay.voiceapp.data.entity.Meeting
import com.abhay.voiceapp.data.entity.MeetingStatus
import com.abhay.voiceapp.ui.viewmodel.DashboardViewModel
import com.abhay.voiceapp.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToRecording: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartNewRecording: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val meetings by viewModel.meetings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartNewRecording) {
                Icon(Icons.Default.Add, contentDescription = "Start Recording")
            }
        }
    ) { padding ->
        if (meetings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_meetings_yet),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onStartNewRecording) {
                        Text(stringResource(R.string.start_recording))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(meetings) { meeting ->
                    MeetingCard(
                        meeting = meeting,
                        onClick = {
                            when (meeting.status) {
                                MeetingStatus.COMPLETED -> onNavigateToSummary(meeting.id)
                                else -> onNavigateToRecording(meeting.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MeetingCard(
    meeting: Meeting,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeFormatter.formatTimestamp(meeting.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                StatusChip(status = meeting.status)
            }
            
            meeting.endTime?.let { endTime ->
                val duration = endTime - meeting.startTime
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration: ${TimeFormatter.formatDuration(duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: MeetingStatus) {
    val (text, color) = when (status) {
        MeetingStatus.RECORDING -> "Recording" to MaterialTheme.colorScheme.error
        MeetingStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.tertiary
        MeetingStatus.STOPPED -> "Stopped" to MaterialTheme.colorScheme.secondary
        MeetingStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.primary
        MeetingStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        MeetingStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
