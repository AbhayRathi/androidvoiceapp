package com.androidvoiceapp.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidvoiceapp.data.room.ChunkEntity
import com.androidvoiceapp.service.RecordingService
import com.androidvoiceapp.ui.viewmodels.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(meetingId) {
        viewModel.setMeetingId(meetingId)
    }
    
    val meeting by viewModel.meeting.collectAsStateWithLifecycle()
    val chunks by viewModel.chunks.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    
    val isRecording = sessionState?.isRecording == true
    val isPaused = sessionState?.isPaused == true
    val status = sessionState?.status ?: meeting?.status ?: "stopped"
    
    // Calculate elapsed time
    val elapsedTime = remember(sessionState) {
        sessionState?.let {
            val now = System.currentTimeMillis()
            val elapsed = if (it.isPaused) {
                (it.pausedTime ?: now) - it.recordingStartTime
            } else {
                now - it.recordingStartTime
            }
            elapsed / 1000
        } ?: 0L
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meeting?.title ?: "Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer
            Text(
                text = formatTime(elapsedTime),
                style = MaterialTheme.typography.displayLarge,
                color = if (isRecording && !isPaused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    status.contains("Recording") -> MaterialTheme.colorScheme.primary
                    status.contains("Paused") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recording controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    // Pause/Resume button
                    if (isPaused) {
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_RESUME
                                }
                                context.startService(intent)
                            },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                    } else {
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_PAUSE
                                }
                                context.startService(intent)
                            }
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                    }
                    
                    // Stop button
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, RecordingService::class.java).apply {
                                action = RecordingService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Chunks list
            Text(
                text = "Audio Chunks (${chunks.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chunks, key = { it.id }) { chunk ->
                    ChunkCard(chunk)
                }
            }
            
            // View summary button (if meeting is completed)
            if (meeting?.status == "completed") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToSummary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Summary")
                }
            }
        }
    }
}

@Composable
fun ChunkCard(chunk: ChunkEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chunk ${chunk.sequenceNumber}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${chunk.duration / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val statusColor = when (chunk.status) {
                "recording" -> MaterialTheme.colorScheme.error
                "finalized" -> MaterialTheme.colorScheme.tertiary
                "transcribing" -> MaterialTheme.colorScheme.secondary
                "transcribed" -> MaterialTheme.colorScheme.primary
                "failed" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = chunk.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
