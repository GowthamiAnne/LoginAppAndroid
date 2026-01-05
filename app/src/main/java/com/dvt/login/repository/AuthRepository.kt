package com.dvt.login.repository

import kotlinx.coroutines.delay
import android.content.Context

open class AuthRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)


    open suspend fun login(username: String, password: String): Result<String> {
        //delay(300) // simulate network delay
        return if (username == "anne" && password == "anne") {
            Result.success("jwt_token_123")
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    open suspend fun rememberToken(token: String) {

        sharedPreferences.edit().putString("auth_token", token).apply()

    }

    open suspend fun getRememberedToken(): String? {
        return sharedPreferences.getString("auth_token", null)

    }
}
