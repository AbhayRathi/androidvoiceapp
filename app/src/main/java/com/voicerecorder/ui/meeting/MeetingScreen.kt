package com.voicerecorder.ui.meeting

import android.Manifest
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.voicerecorder.R
import com.voicerecorder.data.room.Chunk
import com.voicerecorder.data.room.MeetingStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MeetingScreen(
    meetingId: Long,
    onNavigateToSummary: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val meeting by viewModel.meeting.collectAsState()
    val chunks by viewModel.chunks.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    )

    LaunchedEffect(meetingId) {
        viewModel.setMeetingId(meetingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meeting?.title ?: stringResource(R.string.meeting_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (meeting?.status == MeetingStatus.COMPLETED) {
                        IconButton(onClick = { onNavigateToSummary(meetingId) }) {
                            Icon(Icons.Default.Article, contentDescription = "Summary")
                        }
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
            val elapsedTime = calculateElapsedTime(
                meeting?.startTime ?: 0,
                meeting?.endTime,
                sessionState?.totalPausedDuration ?: 0
            )
            Text(
                text = formatElapsedTime(elapsedTime),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            val status = sessionState?.statusMessage ?: meeting?.status?.name ?: "Stopped"
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isRecording = sessionState?.isRecording == true
                val isPaused = sessionState?.isPaused == true

                if (!isRecording) {
                    Button(
                        onClick = {
                            if (permissionsState.allPermissionsGranted) {
                                viewModel.startRecording(context, meetingId)
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.start_recording))
                    }
                } else {
                    if (isPaused) {
                        Button(onClick = { viewModel.resumeRecording(context) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.resume_recording))
                        }
                    } else {
                        Button(onClick = { viewModel.pauseRecording(context) }) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.pause_recording))
                        }
                    }

                    Button(
                        onClick = { viewModel.stopRecording(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.stop_recording))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Chunks list
            Text(
                text = stringResource(R.string.chunks_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chunks) { chunk ->
                    ChunkItem(chunk)
                }
            }
        }
    }
}

@Composable
fun ChunkItem(chunk: Chunk) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chunk ${chunk.chunkNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status: ${chunk.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Transcription: ${chunk.transcriptionStatus.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = when (chunk.transcriptionStatus) {
                    com.voicerecorder.data.room.TranscriptionStatus.COMPLETED -> Icons.Default.CheckCircle
                    com.voicerecorder.data.room.TranscriptionStatus.IN_PROGRESS -> Icons.Default.Pending
                    com.voicerecorder.data.room.TranscriptionStatus.FAILED -> Icons.Default.Error
                    else -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = when (chunk.transcriptionStatus) {
                    com.voicerecorder.data.room.TranscriptionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    com.voicerecorder.data.room.TranscriptionStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun calculateElapsedTime(startTime: Long, endTime: Long?, pausedDuration: Long): Long {
    if (startTime == 0L) return 0L
    val end = endTime ?: System.currentTimeMillis()
    return end - startTime - pausedDuration
}

private fun formatElapsedTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
