package com.androidvoiceapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for securely storing and retrieving API keys and settings
 * Uses EncryptedSharedPreferences for secure storage
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecurePreferences"
        private const val PREFS_FILENAME = "secure_app_prefs"
        
        // Keys
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER = "provider"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to standard", e)
            // Fallback to standard SharedPreferences if encryption fails
            context.getSharedPreferences(PREFS_FILENAME + "_fallback", Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Get the selected API provider
     * @return Provider name (e.g., "Mock", "OpenAI", "Gemini")
     */
    fun getSelectedProvider(): String {
        return encryptedPrefs.getString(KEY_PROVIDER, "Mock") ?: "Mock"
    }
    
    /**
     * Get the stored API key
     * @return API key or empty string if not set
     */
    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
    }
    
    /**
     * Check if a valid API key is stored
     * @return true if API key exists and is not empty
     */
    fun hasApiKey(): Boolean {
        return getApiKey().isNotEmpty()
    }
    
    /**
     * Save API provider and key
     * @param provider Provider name (e.g., "OpenAI", "Gemini")
     * @param apiKey API key to store (will be encrypted)
     */
    fun saveSettings(provider: String, apiKey: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_PROVIDER, provider)
                .putString(KEY_API_KEY, apiKey)
                .apply()
            Log.d(TAG, "Settings saved successfully: provider=$provider")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
        }
    }
    
    /**
     * Clear stored API key and provider
     */
    fun clearSettings() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_PROVIDER)
                .remove(KEY_API_KEY)
                .apply()
            Log.d(TAG, "Settings cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear settings", e)
        }
    }
    
    /**
     * Get provider display name for UI
     */
    fun getProviderDisplayName(): String {
        return when (getSelectedProvider()) {
            "OpenAI" -> "OpenAI Whisper"
            "Gemini" -> "Google Gemini"
            else -> "Mock (Testing)"
        }
    }
}
