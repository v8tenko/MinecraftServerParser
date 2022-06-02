package com.example.commands

object Commands {
    const val STOP = "/stop"
    const val SAY = "/say"
    const val ADD_TO_WHITELIST = "/whitelist add"
    const val REMOVE_FROM_WHITELIST = "/whitelist remove"

    object Errors {
        const val SERVER_IS_ALREADY_RUNNING = "ServerError: Unable to start server: he is running"
        const val SERVER_IS_NOT_RUNNING = "ServerError: Unable to stop server: server is down"
        const val NEED_TO_REGISTER = "You need to enter server, to get it's status"


        fun minecraftNicknameIsMissing(command: String = "command") =
            "Illegal arguments: $command requires minecraft nickname"

        fun missingArguments(requires: Int, given: Int) =
            "Illegal arguments: command requires $requires, given $given"

        fun accessDenied(requires: Int) = "Access denied: requires level $requires"

    }
}