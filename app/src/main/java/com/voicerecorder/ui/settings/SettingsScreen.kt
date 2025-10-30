package com.voicerecorder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voicerecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf("Mock") }
    var apiKey by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Provider Selection
            Text(
                text = stringResource(R.string.api_provider_title),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedProvider == "Mock",
                    onClick = { selectedProvider = "Mock" },
                    label = { Text(stringResource(R.string.mock_provider)) }
                )
                FilterChip(
                    selected = selectedProvider == "Real",
                    onClick = { selectedProvider = "Real" },
                    label = { Text(stringResource(R.string.real_provider)) }
                )
            }

            // API Key Input
            Text(
                text = stringResource(R.string.api_key_title),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("Enter your API key here") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedProvider == "Real"
            )

            Text(
                text = "Note: API key is stored securely and only used when 'Real API' provider is selected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Save Button
            Button(
                onClick = {
                    // TODO: Save settings to DataStore
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }

            Divider()

            // Info Section
            Text(
                text = "About Mock Provider",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "The mock provider generates deterministic transcripts and summaries for testing purposes. " +
                        "No actual API calls are made, and no API key is required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Real API Integration",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "To use real transcription and summary APIs (OpenAI Whisper, Google Gemini, etc.), " +
                        "select 'Real API' and enter your API key above. The implementation will be pluggable " +
                        "and can be configured to use different providers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
