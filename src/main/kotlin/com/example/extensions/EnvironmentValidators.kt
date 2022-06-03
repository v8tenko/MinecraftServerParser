package com.example.extensions

import com.example.server.Access
import com.example.server.ResponseStatus
import com.example.server.ServerBuilder
import com.example.server.enums.ServerStatus
import com.example.database.Users
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val botScope = CoroutineScope(Dispatchers.Default)

sealed class CheckUser {

    abstract suspend fun validate(message: Message, args: List<String>): String?

    class WithAccess(private val accessLevel: Int) : CheckUser() {
        override suspend fun validate(message: Message, args: List<String>): String? {
            val requesterLevel = Users.getUserAccessStatus(message.from!!.username!!)

            if (requesterLevel >= accessLevel) {
                return null
            }

            return if (requesterLevel == Access.NOT_REGISTERED) {
                ResponseStatus.NEED_TO_REGISTER
            } else {
                ResponseStatus.accessDenied(accessLevel)
            }
        }
    }

    class WithArguments(private val count: Int) : CheckUser() {
        override suspend fun validate(message: Message, args: List<String>): String? {
            if (args.size < count) {
                return ResponseStatus.missingArguments(count, args.size)
            }

            return null
        }
    }

    class WithServerStatus(private val state: ServerStatus) : CheckUser() {
        override suspend fun validate(message: Message, args: List<String>): String? {
            if (state == ServerBuilder.STATUS) {
                return null
            }

            if (ServerBuilder.STATUS == ServerStatus.ON) {
                return ResponseStatus.SERVER_IS_RUNNING
            }

            return ResponseStatus.SERVER_IS_NOT_RUNNING
        }
    }
}

fun withAccess(accessLevel: Int) = CheckUser.WithAccess(accessLevel)
fun withArgumentsCount(count: Int) = CheckUser.WithArguments(count)
fun withSingleArgument() = CheckUser.WithArguments(1)
fun withServerStatus(state: ServerStatus) = CheckUser.WithServerStatus(state)


fun CommandHandlerEnvironment.validate(vararg checks: CheckUser, callback: suspend () -> Unit) = botScope.launch {
    val chatId = ChatId.fromId(message.chat.id)

    val errors = checks.mapNotNull { it.validate(message, args) }

    if (errors.isNotEmpty()) {
        bot.sendMessage(chatId, errors.first())

        return@launch
    }

    callback()
    bot.sendMessage(chatId, ResponseStatus.OK)
}