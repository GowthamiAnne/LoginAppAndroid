package com.dvt.login.repository

import kotlinx.coroutines.delay

open class AuthRepository {

    private var rememberedToken: String? = null

    open suspend fun login(username: String, password: String): Result<String> {
        //delay(300) // simulate network delay
        return if (username == "anne" && password == "anne") {
            Result.success("jwt_token_123")
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    open fun rememberToken(token: String) {
        rememberedToken = token
    }

    open fun getRememberedToken(): String? = rememberedToken
}
