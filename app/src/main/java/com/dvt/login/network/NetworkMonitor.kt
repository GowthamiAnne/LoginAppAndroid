package com.dvt.login.network

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

 open class NetworkMonitor(context: Context?) {

    // Fake network state for demo
    private val _isOnline = MutableStateFlow(true)
    open val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    open fun setOnline(value: Boolean) {
        _isOnline.value = value
    }
}
