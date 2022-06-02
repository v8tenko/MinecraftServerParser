package com.example.telegram

import com.example.Access
import com.example.commands.Commands
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
            if (Users.getUserAccessStatus(message.from!!.username!!) >= accessLevel) {
                return null

            }

            return if (accessLevel == Access.NOT_REGISTERED) {
                Commands.Errors.NEED_TO_REGISTER
            } else {
                Commands.Errors.accessDenied(accessLevel)
            }
        }
    }

    class WithArguments(private val count: Int) : CheckUser() {
        override suspend fun validate(message: Message, args: List<String>): String? {
            if (args.size < count) {
                return Commands.Errors.missingArguments(count, args.size)
            }

            return null
        }
    }
}

fun withAccess(accessLevel: Int) = CheckUser.WithAccess(accessLevel)
fun withArgumentsCount(count: Int) = CheckUser.WithArguments(count)


fun CommandHandlerEnvironment.validate(vararg checks: CheckUser, callback: suspend () -> Unit) = botScope.launch {
    val chatId = ChatId.fromId(message.chat.id)

    val errors = checks.mapNotNull { it.validate(message, args) }

    if (errors.isNotEmpty()) {
        bot.sendMessage(chatId, errors.first())

        return@launch
    }

    callback()
}