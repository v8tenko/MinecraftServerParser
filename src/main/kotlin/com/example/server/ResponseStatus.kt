package com.example.server

object ResponseStatus {
    const val SERVER_IS_RUNNING = "ServerError: Unable to run command: server is running"
    const val SERVER_IS_NOT_RUNNING = "ServerError: Unable to run command: server is down"
    const val NEED_TO_REGISTER = "You need to join server, to run this command"
    const val OK = "OK"

    fun missingArguments(requires: Int, given: Int) =
        "Illegal arguments: command requires $requires, given $given"

    fun accessDenied(requires: Int) = "Access denied: requires level $requires"

}
