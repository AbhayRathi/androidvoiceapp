package com.androidvoiceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "API Provider",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Provider selection
            Column {
                ProviderOption(
                    title = "Mock (Default)",
                    description = "Uses mock APIs for testing",
                    selected = selectedProvider == "Mock",
                    onClick = { selectedProvider = "Mock" }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ProviderOption(
                    title = "OpenAI Whisper",
                    description = "Real transcription using OpenAI",
                    selected = selectedProvider == "OpenAI",
                    onClick = { selectedProvider = "OpenAI" }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ProviderOption(
                    title = "Google Gemini",
                    description = "Real transcription using Google Gemini",
                    selected = selectedProvider == "Gemini",
                    onClick = { selectedProvider = "Gemini" }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // API Key input
            Text(
                text = "API Key",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Enter your API key") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedProvider != "Mock"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "API key will be used when real provider is selected. For now, the app uses mock providers by default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    // TODO: Save settings
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Integration Instructions",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To use real API providers:\n\n" +
                                "1. Select your preferred provider above\n" +
                                "2. Enter your API key\n" +
                                "3. Save settings\n" +
                                "4. The app will use real APIs for transcription and summary generation",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}
