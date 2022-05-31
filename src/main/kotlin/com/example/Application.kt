package com.example

import com.example.commands.ServerBuilder
import com.example.database.Chats
import com.example.database.Users
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun main() {
    val serverBuilder = ServerBuilder("sh", "start.sh", System.out)
    val botScope = CoroutineScope(Dispatchers.Default)

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
                val id = ChatId.fromId(message.chat.id)

                botScope.launch {
                    if (Users.getUserAccessStatus(message.from!!.username!!) >= Access.ADMIN) {
                        serverBuilder.startServer()
                        bot.sendMessage(id, "OK")
                    } else {
                        bot.sendMessage(id, "Access denied: requires level 2")

                    }
                }
            }

            command("stop") {
                val id = ChatId.fromId(message.chat.id)

                botScope.launch {
                    if (Users.getUserAccessStatus(message.from!!.username!!) >= Access.ADMIN) {
                        serverBuilder.stopServer()
                        bot.sendMessage(id, "OK")
                    } else {
                        bot.sendMessage(id, "Access denied: requires level 2")

                    }
                }
            }

            command("status") {
                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    serverBuilder.status()
                )
            }

            command("enable_messages") {
                Chats.subscribeChat(message.chat.title ?: message.from?.username ?: return@command, message.chat.id)
                bot.sendMessage(ChatId.fromId(message.chat.id), "OK")
            }

            command("disable_messages") {
                Chats.unsubscribeChat(message.chat.id)
                bot.sendMessage(ChatId.fromId(message.chat.id), "OK")
            }

            command("register") {
                val chatId = ChatId.fromId(message.chat.id)
                val requesterName = message.from!!.username!!

                if (args.isEmpty()) {
                    bot.sendMessage(chatId, "Unable to register user: username is missing")
                }
                if (args.size == 1) {
                    val name = args[0]

                    Users.registerUser(name, Access.USER)
                    bot.sendMessage(chatId, "$name registered")
                } else {
                    val role = args[0]
                    val names = args.subList(1, args.size)

                    if (!Access.contains(role)) {
                        bot.sendMessage(chatId, "Role $role not found")

                        return@command
                    }

                    botScope.launch {
                        val userRole = Users.getUserAccessStatus(requesterName)
                        val roleAccessLevel = Access.getAccessLevelByLabel(role)
                        if (userRole < roleAccessLevel) {
                            bot.sendMessage(chatId, "Access denied: requires level $roleAccessLevel")

                            return@launch
                        }
                        names.forEach { name ->
                            Users.registerUser(name, userRole)
                        }
                        bot.sendMessage(chatId, "${names.joinToString(" ")} registered with role $role")
                    }
                }
            }

            command("bind") {
                val chatId = ChatId.fromId(message.chat.id)

                if (args.isEmpty()) {
                    bot.sendMessage(chatId, "Illegal arguments: bind requires minecraft nickname")

                    return@command
                }

                val requesterName = message.from!!.username!!
                val minecraftName = args[0]

                Users.bindUser(requesterName, minecraftName)
                bot.sendMessage(chatId, "$requesterName bound to $minecraftName")
            }

            command("run") {
                val chatId = ChatId.fromId(message.chat.id)
                val requesterName = message.from!!.username!!

                if (args.isEmpty()) {
                    bot.sendMessage(chatId, "Illegal arguments: run requires command")

                    return@command
                }

                botScope.launch {
                    val userRole = Users.getUserAccessStatus(requesterName)
                    if (userRole < Access.ADMIN) {
                        bot.sendMessage(chatId, "Access denied: requires level ${Access.ADMIN}")

                        return@launch
                    }

                    serverBuilder.writeToServer("/" + args.joinToString(" "))
                    bot.sendMessage(chatId, "OK")
                }
            }
        }
    }


    botScope.launch {
        for (text in serverBuilder.events) {
            val ids = Chats.all()
            ids.forEach { bot.sendMessage(ChatId.fromId(it), text) }
        }
    }

    bot.startPolling()
}
