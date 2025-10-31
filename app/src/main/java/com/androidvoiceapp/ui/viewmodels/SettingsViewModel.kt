package com.androidvoiceapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvoiceapp.util.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Manages API provider selection and secure API key storage
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences
) : ViewModel() {
    
    private val _selectedProvider = MutableStateFlow("Mock")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()
    
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _selectedProvider.value = securePreferences.getSelectedProvider()
            _apiKey.value = securePreferences.getApiKey()
        }
    }
    
    fun updateProvider(provider: String) {
        _selectedProvider.value = provider
    }
    
    fun updateApiKey(key: String) {
        _apiKey.value = key
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            securePreferences.saveSettings(
                provider = _selectedProvider.value,
                apiKey = _apiKey.value
            )
            _saveSuccess.value = true
        }
    }
    
    fun clearSettings() {
        viewModelScope.launch {
            securePreferences.clearSettings()
            _selectedProvider.value = "Mock"
            _apiKey.value = ""
        }
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
