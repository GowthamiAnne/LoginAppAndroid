package com.dvt.login.repository

import kotlinx.coroutines.delay
import android.content.Context
import com.dvt.login.util.LoginErrors

open class AuthRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN = "auth_token"
    }


    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    open suspend fun login(username: String, password: String): Result<String> {
        //delay(300) // simulate network delay
        return if (username == "anne" && password == "gowthami") {
            Result.success("jwt_token_123")
        } else {
            Result.failure(Exception(LoginErrors.INVALID_CREDENTIALS))
        }
    }

    fun clearRememberedToken() {
        // Example using SharedPreferences
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    open suspend fun rememberToken(token: String) {

        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()

    }

    open suspend fun getRememberedToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)

    }

    suspend fun setLockoutTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong("lockout_time", timestamp).apply()
    }

    suspend fun getLockoutTimestamp(): Long? {
        val value = sharedPreferences.getLong("lockout_time", 0L)
        return if (value == 0L) null else value
    }

    suspend fun clearLockoutTimestamp() {
        sharedPreferences.edit().remove("lockout_time").apply()
    }
}
