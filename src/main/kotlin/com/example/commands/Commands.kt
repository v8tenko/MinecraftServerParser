package com.example.commands

object Commands {
    const val STOP = "/stop"
    const val ONLINE = "/list"
    const val SAY = "/say"
    const val ADD_TO_WHITELIST = "/whitelist add"

    object Errors {
        const val SERVER_IS_ALREADY_RUNNING = "ServerError: Unable to start server: he is running"
        const val SERVER_IS_NOT_RUNNING = "ServerError: Unable to stop server: server id down"
    }
}