package com.example.server

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

object Access {
    // everyone
    const val NOT_REGISTERED = -1

    // can get STATUS, write to chat
    const val USER = 0

    // can start/stop server
    const val OPERATOR = 1

    // can run commands directly to server
    const val ADMIN = 2

    private val ROLES = Access::class.memberProperties.map { it.name.lowercase() }.filter { it != "roles" }

    fun contains(label: String) = ROLES.contains(label)

    fun getAccessLevelByLabel(label: String): Int {
        val field = Access::class.memberProperties.first { it.name.lowercase() == label.lowercase() }

        return field.javaField!!.getInt(this)
    }
}