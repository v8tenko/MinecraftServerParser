package com.example.telegram

import com.example.Scopes
import com.example.database.Chats
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.launch

object TelegramUtils {
    private lateinit var bot: Bot

    fun bindBot(bot: Bot) {
        this.bot = bot
    }

    fun sendToAlLChats(content: String) =  Scopes.botScope.launch {
        val chats = Chats.all().map { ChatId.fromId(it) }

        chats.forEach {
            bot.sendMessage(it, content)
        }
    }
}