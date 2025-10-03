package de.janmeckelholt.stravasummary

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

class PreferencesDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(Constants.PREFERENCES_DATA_STORE_NAME)
    }

    fun getStringFlow(key: String): Flow<String?> {
        val k = stringPreferencesKey(key)
        return context.dataStore.data
            .map { preferences ->
                preferences[k]
            }
    }

    suspend fun getString(key: String): String? {
        val k = stringPreferencesKey(key)
        return context.dataStore.data.first()[k]

    }

    fun getBooleanFlow(key: String): Flow<Boolean?> {
        val k = booleanPreferencesKey(key)
        return context.dataStore.data
            .map { preferences ->
                preferences[k]
            }
    }

    suspend fun getBoolean(key: String): Boolean? {
        val k = booleanPreferencesKey(key)
        return context.dataStore.data.first()[k]
    }

    fun getLongFlow(key: String): Flow<Long?> {
        val k = longPreferencesKey(key)
        return context.dataStore.data
            .map { preferences ->
                preferences[k]
            }
    }

    suspend fun getLong(key: String): Long? {
        val k = longPreferencesKey(key)
        return context.dataStore.data.first()[k]

    }


    suspend fun saveString(key: String, value: String) {
        val k = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[k] = value
        }
    }

    suspend fun saveBoolean(key: String, value: Boolean) {
        val k = booleanPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[k] = value
        }
    }

    suspend fun saveLong(key: String, value: Long) {
        val k = longPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[k] = value
        }
    }


}