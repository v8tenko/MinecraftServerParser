package com.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

object Scopes {
    var serverScope = CoroutineScope(Dispatchers.Default)
    val botScope = CoroutineScope(Dispatchers.Default)
    val databaseScope = CoroutineScope(Dispatchers.IO)

    fun restartServerScope() {
        serverScope.cancel()
        serverScope = CoroutineScope(Dispatchers.Default)
    }
}