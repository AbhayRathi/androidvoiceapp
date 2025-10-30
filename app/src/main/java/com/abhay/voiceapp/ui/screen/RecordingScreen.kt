package com.abhay.voiceapp.ui.screen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhay.voiceapp.R
import com.abhay.voiceapp.data.entity.Chunk
import com.abhay.voiceapp.service.RecordingState
import com.abhay.voiceapp.ui.viewmodel.RecordingViewModel
import com.abhay.voiceapp.util.TimeFormatter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val meeting by viewModel.meeting.collectAsState()
    val chunks by viewModel.chunks.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Request permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    )
    
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
            // Timer Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = TimeFormatter.formatDuration(elapsedTime),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = statusMessage.ifEmpty { stringResource(R.string.stopped) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (recordingState) {
                            is RecordingState.Recording -> MaterialTheme.colorScheme.error
                            is RecordingState.Paused -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (recordingState) {
                    is RecordingState.Stopped -> {
                        Button(
                            onClick = {
                                if (permissionsState.allPermissionsGranted) {
                                    viewModel.startRecording("Meeting ${System.currentTimeMillis()}")
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.record))
                        }
                    }
                    is RecordingState.Recording -> {
                        Button(
                            onClick = { viewModel.pauseRecording() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.pause))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { viewModel.stopRecording() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.stop))
                        }
                    }
                    is RecordingState.Paused -> {
                        Button(
                            onClick = { viewModel.resumeRecording() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.resume))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { viewModel.stopRecording() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.stop))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Chunks List
            if (chunks.isNotEmpty()) {
                Text(
                    text = "Audio Chunks (${chunks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chunks) { chunk ->
                        ChunkCard(chunk = chunk)
                    }
                }
            }
        }
    }
}

@Composable
fun ChunkCard(chunk: Chunk) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = "Chunk ${chunk.chunkIndex + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = TimeFormatter.formatDuration(chunk.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = chunk.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (chunk.status) {
                    com.abhay.voiceapp.data.entity.ChunkStatus.TRANSCRIBED -> 
                        MaterialTheme.colorScheme.primary
                    com.abhay.voiceapp.data.entity.ChunkStatus.FAILED -> 
                        MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}
