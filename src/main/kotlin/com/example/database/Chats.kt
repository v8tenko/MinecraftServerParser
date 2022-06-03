package com.example.database

import com.example.Scopes
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


object Chats : IntIdTable() {
    private val name: Column<String> = varchar("name", 100)
    private val chatId: Column<Long> = long("chat_id")

    suspend fun all() = Scopes.databaseScope.async {
        return@async transaction { Chats.selectAll().map { it[chatId] } }
    }.await()

    fun subscribeChat(title: String, id: Long) = Scopes.databaseScope.launch {
        transaction {
            Chats.insert {
                it[name] = title
                it[chatId] = id
            }
        }
    }

    fun unsubscribeChat(id: Long) = Scopes.databaseScope.launch {
        transaction {
            Chats.deleteWhere { chatId eq id }
        }
    }

    suspend fun contains(id: Long) = Scopes.databaseScope.async {
        transaction {
            Chats.select { chatId eq id }.toList().isNotEmpty()
        }
    }.await()
}