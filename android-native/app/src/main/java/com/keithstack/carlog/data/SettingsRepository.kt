package com.keithstack.carlog.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "carlog_settings")

enum class LogStyle { Keypad, Ledger, Plate }
enum class KeypadOrder { Phone, Calculator }

data class AppSettings(
    val style: LogStyle = LogStyle.Keypad,
    val order: KeypadOrder = KeypadOrder.Phone,
    val hints: Boolean = true,
    val vibrate: Boolean = true,
)

/** Local-only device preferences — not backed up to Firestore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val STYLE = stringPreferencesKey("style")
        val ORDER = stringPreferencesKey("order")
        val HINTS = booleanPreferencesKey("hints")
        val VIBRATE = booleanPreferencesKey("vibrate")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            style = prefs[Keys.STYLE]?.let { runCatching { LogStyle.valueOf(it) }.getOrNull() } ?: LogStyle.Keypad,
            order = prefs[Keys.ORDER]?.let { runCatching { KeypadOrder.valueOf(it) }.getOrNull() } ?: KeypadOrder.Phone,
            hints = prefs[Keys.HINTS] ?: true,
            vibrate = prefs[Keys.VIBRATE] ?: true,
        )
    }

    suspend fun setStyle(style: LogStyle) {
        context.settingsDataStore.edit { it[Keys.STYLE] = style.name }
    }

    suspend fun setOrder(order: KeypadOrder) {
        context.settingsDataStore.edit { it[Keys.ORDER] = order.name }
    }

    suspend fun setHints(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HINTS] = enabled }
    }

    suspend fun setVibrate(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATE] = enabled }
    }
}
