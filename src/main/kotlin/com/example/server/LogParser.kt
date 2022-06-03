package com.example.server

import com.example.server.enums.LogType

object LogParser {

    fun shouldLog(query: String): Pair<String?, LogType> {
        val checkers =
            listOf(
                LogParser::isPlayerInfoLog,
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

    fun isPlayerInfoLog(query: String): Pair<String?, LogType> {
        if (!query.contains("(joined the game)|(left the game)".toRegex())) {
            return null to LogType.NOISE
        }

        val status = if (query.contains("joined")) LogType.PLAYER_JOINED else LogType.PLAYER_LEFT

        return (query.split(":").lastOrNull()
            ?: throw IllegalStateException("Cannot parse log (PLAYER_ENTER): $query")) to status
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

    fun nicknameFromQuery(query: String): String {
        val selectRegex = " .{1,20} ((joined)|(left))".toRegex()


        return (selectRegex.find(query)?.value
            ?: throw IllegalArgumentException("Error: Unable to get nickname from query $query")).split(" ")[2]
    }

}