package com.example.database

import com.example.Access
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

val databaseScope = CoroutineScope(Dispatchers.IO)

object Users : IntIdTable() {
    private val name: Column<String> = varchar("name", 50)
    private val minecraftName: Column<String> = varchar("minecraft_name", 50)
    private val rootLevel: Column<Int> = integer("root_level")
    private val isNew: Column<Boolean> = bool("is_new")

    private var onNewUser: ((name: String) -> Unit)? = null

    fun setOnNewUserCallback(cb: (name: String) -> Unit) {
        this.onNewUser = cb
    }

    fun registerUser(queryName: String, queryRootLevel: Int) =
        databaseScope.launch {
            transaction {
                val notHaveIt = Users.select { name eq queryName }.toList().isNotEmpty()
                if (notHaveIt) {
                    onNewUser?.invoke(queryName)
                    Users.update({ name eq queryName }) {
                        it[rootLevel] = queryRootLevel
                    }

                    return@transaction
                }
                Users.insert {
                    it[name] = queryName
                    it[minecraftName] = queryName
                    it[rootLevel] = queryRootLevel
                    it[isNew] = true
                }
            }
        }

    suspend fun getUserAccessStatus(userName: String) = databaseScope.async {
        transaction {
            Users.select { name eq userName }.map { it[rootLevel] }[0]
        }
    }.await()

    fun updatePlayerWhitelistStatus(userName: String, status: Boolean = false) = databaseScope.launch {
        transaction {
            Users.update({ name eq userName }) {
                it[isNew] = status
            }
        }
    }

    fun bindUser(userName: String, minecraftNameQuery: String) = databaseScope.launch {
        transaction {
            Users.update({ name eq userName }) {
                it[minecraftName] = minecraftNameQuery
            }
            onNewUser?.invoke(minecraftNameQuery)
        }
    }

    suspend fun filterNewUsers() = databaseScope.async {
        transaction {
            Users.select { isNew eq true }.map { it[minecraftName] }
        }
    }.await()
}
