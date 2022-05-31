package com.example.commands

object LogParser {

    fun shouldLog(query: String): Pair<String?, LogType> {
        val checkers =
            listOf(
                LogParser::isPlayerInfoLog,
                LogParser::isStartedLog,
                LogParser::isStoppedLog,
                LogParser::isError,
                LogParser::isPlayerMessage,
                LogParser::isWhitelistOK,
                LogParser::isWhitelistError
            )
        for (i in checkers.indices) {
            val response = checkers[i].call(query)

            if (response.first != null) {
                return response
            }
        }

        return null to LogType.NOISE
    }

    fun isStartedLog(query: String): Pair<String?, LogType> {
        if (!query.contains("\\[Server thread/INFO].*: Done".toRegex())) {
            return null to LogType.NOISE
        }

        val output = query.split(":").lastOrNull()
            ?: throw IllegalStateException("Cannot parse log (STARTED): $query")
        val bracketsRegex = "\\(.*\\)".toRegex()

        return ("Server stared. Build time: " + bracketsRegex.find(output)?.value) to LogType.BUILD_FINISHED
    }

    fun isStoppedLog(query: String): Pair<String?, LogType> {
        if (!query.contains("\\[Server Shutdown Thread/INFO].*: Stopping server".toRegex())) {
            return null to LogType.NOISE;
        }

        return "Server stopped" to LogType.SERVER_STOP
    }

    fun isPlayerInfoLog(query: String): Pair<String?, LogType> {
        if (!query.contains("(joined the game)|(left the game)".toRegex())) {
            return null to LogType.NOISE
        }

        return (query.split(":").lastOrNull()
            ?: throw IllegalStateException("Cannot parse log (PLAYER_ENTER): $query")) to LogType.PLAYER_CONNECTION_STATUS
    }

    fun isPlayerMessage(query: String): Pair<String?, LogType> {
        if (!query.contains("<.*> .*".toRegex())) {
            return null to LogType.NOISE
        }

        val textRegex = "<.*> .*".toRegex()

        return textRegex.find(query)?.value?.replace("<", "")?.replace('>', ':') to LogType.CHAT_EVENT
    }

    fun isError(query: String): Pair<String?, LogType> {
        if (!query.contains("(A problem occurred)|(Exception in thread)".toRegex())) {
            return null to LogType.NOISE
        }

        return query to LogType.ERROR
    }

    fun isWhitelistOK(query: String): Pair<String?, LogType> {
        if (!query.contains("Added .* to the whitelist".toRegex())) {
            return null to LogType.NOISE
        }

        val nameRegex = "Added .* ".toRegex()

        return (nameRegex.find(query)?.value?.split(" ")?.get(1)
            ?: throw IllegalStateException("Cannot parse log (WHITELIST OK): $query")) to LogType.WHITELIST_OK
    }

    fun isWhitelistError(query: String): Pair<String?, LogType> {
        if (!query.contains("Could not add .* to the whitelist".toRegex())) {
            return null to LogType.NOISE
        }

        val nameRegex = "add .* ".toRegex()

        return (nameRegex.find(query)?.value?.split(" ")?.get(1)
            ?: throw IllegalStateException("Cannot parse log (WHITELIST ERROR): $query")) to LogType.WHITELIST_ERROR
    }

}