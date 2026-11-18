package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edutech_mindset_prefs")

data class LocalAppPreferences(
    val darkMode: Boolean = false,
    val language: String = "AR",
    val autoBackup: Boolean = true,
)

class PreferencesStore(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
    }

    val preferences: Flow<LocalAppPreferences> = store.data.map { prefs ->
        LocalAppPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "AR",
            autoBackup = prefs[Keys.AUTO_BACKUP] ?: true,
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        store.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        store.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        store.edit { it[Keys.AUTO_BACKUP] = enabled }
    }
}
