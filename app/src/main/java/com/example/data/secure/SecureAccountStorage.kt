package com.example.data.secure

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectedAccount(
    val email: String,
    val displayName: String,
    val isConnected: Boolean,
    val connectedAt: Long = 0L,
    val note: String = ""
)

class SecureAccountStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "alzuhra_secure_prefs",
        Context.MODE_PRIVATE
    )

    private val _accountFlow = MutableStateFlow(loadAccount())
    val accountFlow: Flow<ConnectedAccount> = _accountFlow.asStateFlow()

    private fun loadAccount(): ConnectedAccount {
        val isConnected = prefs.getBoolean(KEY_CONNECTED, true) // Default preset to configured academy email
        val email = prefs.getString(KEY_EMAIL, "alzuhraacademy@gmail.com") ?: "alzuhraacademy@gmail.com"
        val displayName = prefs.getString(KEY_NAME, "al ZUHRA Academy") ?: "al ZUHRA Academy"
        val connectedAt = prefs.getLong(KEY_CONNECTED_AT, System.currentTimeMillis())
        val note = prefs.getString(KEY_NOTE, "Google Account configured for automated Meet presence.") ?: ""
        return ConnectedAccount(email, displayName, isConnected, connectedAt, note)
    }

    fun setAccount(email: String, displayName: String) {
        prefs.edit()
            .putBoolean(KEY_CONNECTED, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName)
            .putLong(KEY_CONNECTED_AT, System.currentTimeMillis())
            .apply()
        _accountFlow.value = loadAccount()
    }

    fun disconnect() {
        prefs.edit()
            .putBoolean(KEY_CONNECTED, false)
            .apply()
        _accountFlow.value = loadAccount()
    }

    fun reconnect() {
        prefs.edit()
            .putBoolean(KEY_CONNECTED, true)
            .apply()
        _accountFlow.value = loadAccount()
    }

    fun getAccount(): ConnectedAccount = _accountFlow.value

    companion object {
        private const val KEY_CONNECTED = "account_connected"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_NAME = "account_name"
        private const val KEY_CONNECTED_AT = "account_connected_at"
        private const val KEY_NOTE = "account_note"
    }
}
