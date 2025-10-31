package com.androidvoiceapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val API_PROVIDER = stringPreferencesKey("api_provider")
    }

    override fun getProvider(): Flow<String> {
        return dataStore.data.map {
            it[PreferencesKeys.API_PROVIDER] ?: "Gemini"
        }
    }

    override suspend fun setProvider(provider: String) {
        dataStore.edit {
            it[PreferencesKeys.API_PROVIDER] = provider
        }
    }
}
