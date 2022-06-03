package com.example

import com.example.server.ResponseStatus
import com.example.server.ServerBuilder
import com.example.server.enums.ServerStatus
import com.example.database.Chats
import com.example.database.Users
import com.example.extensions.botScope
import com.example.extensions.validate
import com.example.extensions.withAccess
import com.example.extensions.withServerStatus
import com.example.extensions.withSingleArgument
import com.example.server.Access
import com.example.telegram.TelegramUtils
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun main() {
    val serverBuilder = ServerBuilder("sh", "start.sh", System.out)

    Database.connect(
        "jdbc:mysql://localhost:3306/telegram_bot",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = System.getenv("SQL_PASSWORD")
    )

    Users.setOnNewUserCallback {
        serverBuilder.whitelist(it)
    }

    transaction {
        SchemaUtils.create(Users, Chats)
    }

    val bot = bot {
        token = System.getenv("TELEGRAM_TOKEN")
        dispatch {

            text {
                botScope.launch {
                    if (Chats.contains(message.chat.id)) {
                        serverBuilder.say("${message.from?.username}: ${message.text}")
                    }
                }
            }

            command("start") {
                validate(withAccess(Access.OPERATOR), withServerStatus(ServerStatus.OFF)) {
                    val id = ChatId.fromId(message.chat.id)

                    serverBuilder.startServer()
                    bot.sendMessage(id, ResponseStatus.OK)
                }
            }

            command("stop") {
                validate(withAccess(Access.ADMIN), withServerStatus(ServerStatus.ON)) {
                    val id = ChatId.fromId(message.chat.id)

                    serverBuilder.stopServer(args.getOrNull(0))
                    bot.sendMessage(id, ResponseStatus.OK)
                }
            }

            command("status") {
                validate(withAccess(Access.USER)) {
                    bot.sendMessage(
                        ChatId.fromId(message.chat.id),
                        serverBuilder.status()
                    )
                }
            }

            command("enable_messages") {
                validate(withAccess(Access.USER)) {
                    Chats.subscribeChat(
                        message.chat.title ?: message.from?.username ?: return@validate,
                        message.chat.id
                    )
                    bot.sendMessage(ChatId.fromId(message.chat.id), ResponseStatus.OK)
                }
            }

            command("disable_messages") {
                validate(withAccess(Access.USER)) {
                    Chats.unsubscribeChat(message.chat.id)
                    bot.sendMessage(ChatId.fromId(message.chat.id), ResponseStatus.OK)
                }
            }

            command("register") {
                validate(withSingleArgument(), withAccess(Access.USER)) {
                    val chatId = ChatId.fromId(message.chat.id)

                    if (args.size == 1) {
                        val name = args[0]

                        Users.registerUser(name, Access.USER)
                        bot.sendMessage(chatId, "$name registered")

                        return@validate
                    }

                    val role = args[0]
                    val names = args.subList(1, args.size)

                    if (!Access.contains(role)) {
                        bot.sendMessage(chatId, "Role $role not found")

                        return@validate
                    }

                    names.forEach { name ->
                        Users.registerUser(name, Access.getAccessLevelByLabel(role))
                    }

                    bot.sendMessage(chatId, "${names.joinToString(" ")} registered with role $role")
                }
            }

            command("ban") {
                validate(withAccess(Access.ADMIN), withSingleArgument()) {
                    val name = args[0]

                    serverBuilder.block(name)
                    Users.banUser(name)
                }
            }

            command("bind") {
                validate(withAccess(Access.USER), withSingleArgument()) {
                    val chatId = ChatId.fromId(message.chat.id)
                    val requesterName = message.from!!.username!!
                    val minecraftName = args[0]

                    Users.bindUser(requesterName, minecraftName)
                    bot.sendMessage(chatId, "$requesterName bound to $minecraftName")
                }
            }

            command("run") {
                validate(withAccess(Access.ADMIN), withSingleArgument(), withServerStatus(ServerStatus.ON)) {
                    val chatId = ChatId.fromId(message.chat.id)

                    serverBuilder.writeToServer("/" + args.joinToString(" "))
                    bot.sendMessage(chatId, ResponseStatus.OK)
                }
            }
        }
    }

    TelegramUtils.bindBot(bot)

    Scopes.botScope.launch {
        for (text in serverBuilder.events) {
            TelegramUtils.sendToAlLChats(text)
        }
    }

    bot.startPolling()
}
