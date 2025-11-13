package com.guevara.diego.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ⚠️ MUY IMPORTANTE:
// NO AGREGAR import androidx.datastore.preferences.core.Preferences
// Android Studio lo pone automáticamente y rompe el operador []
// así que dejamos que DataStore maneje ese tipo internamente.

private const val DATASTORE_NAME = "crypto_prefs"

// Extensión de Context para DataStore
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object PreferencesManager {

    private val LAST_UPDATE_KEY = stringPreferencesKey("last_update")

    // Leer
    fun lastUpdateFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[LAST_UPDATE_KEY]    // aquí SÍ funciona el []
        }
    }

    // Guardar
    suspend fun saveLastUpdate(context: Context, timestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_UPDATE_KEY] = timestamp
        }
    }
}
