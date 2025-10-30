package com.voicerecorder.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicerecorder.R
import com.voicerecorder.data.room.SummaryStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val transcript by viewModel.transcript.collectAsState()

    LaunchedEffect(meetingId) {
        viewModel.setMeetingId(meetingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (summary?.status) {
            SummaryStatus.PENDING, SummaryStatus.IN_PROGRESS -> {
                LoadingSummary(
                    progress = summary?.progress ?: 0,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            SummaryStatus.ERROR -> {
                ErrorSummary(
                    errorMessage = summary?.errorMessage ?: "Unknown error",
                    onRetry = { viewModel.retrySummaryGeneration() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            SummaryStatus.COMPLETED -> {
                SummaryContent(
                    summary = summary,
                    transcript = transcript,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No summary available")
                }
            }
        }
    }
}

@Composable
fun LoadingSummary(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.generating_summary),
                style = MaterialTheme.typography.bodyLarge
            )
            if (progress > 0) {
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorSummary(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error: $errorMessage",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
fun SummaryContent(
    summary: com.voicerecorder.data.room.Summary?,
    transcript: List<com.voicerecorder.data.room.TranscriptSegment>,
    viewModel: SummaryViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.summary_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary?.title ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary?.summary ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Action Items
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_items_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val actionItems = viewModel.getActionItems(summary)
                    if (actionItems.isNotEmpty()) {
                        actionItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(item, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text(
                            text = "No action items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Key Points
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.key_points_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val keyPoints = viewModel.getKeyPoints(summary)
                    if (keyPoints.isNotEmpty()) {
                        keyPoints.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(point, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text(
                            text = "No key points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Transcript
        if (transcript.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.transcript_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(transcript) { segment ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
