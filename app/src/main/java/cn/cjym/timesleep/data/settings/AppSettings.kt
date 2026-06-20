package cn.cjym.timesleep.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    private object Keys {
        val isLoggedIn = booleanPreferencesKey("profile.isLoggedIn")
        val saveAudioClips = booleanPreferencesKey("settings.saveAudioClips")
        val sensitivity = doublePreferencesKey("settings.sensitivity")
        val agreementAccepted = booleanPreferencesKey("agreement_accepted")
        val phoneNumber = stringPreferencesKey("profile.phoneNumber")
    }

    val isLoggedIn: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.isLoggedIn] ?: false }
    val saveAudioClips: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.saveAudioClips] ?: true }
    val sensitivity: Flow<Double> = context.settingsDataStore.data.map { it[Keys.sensitivity] ?: 0.65 }
    val agreementAccepted: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.agreementAccepted] ?: false }
    val phoneNumber: Flow<String> = context.settingsDataStore.data.map { it[Keys.phoneNumber] ?: "" }

    suspend fun login(phone: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.phoneNumber] = phone
            prefs[Keys.isLoggedIn] = true
        }
    }

    suspend fun logout() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.isLoggedIn] = false
        }
    }

    /** 注销账号后清除登录态与手机号，对应 iOS `AppSettings.clearAccountData()`。 */
    suspend fun clearAccountData() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.isLoggedIn] = false
            prefs[Keys.phoneNumber] = ""
        }
    }

    suspend fun acceptAgreement() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.agreementAccepted] = true
        }
    }

    suspend fun setSaveAudioClips(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.saveAudioClips] = value
        }
    }

    suspend fun setSensitivity(value: Double) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.sensitivity] = value
        }
    }
}
