package com.example.commands

import com.example.database.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStream

class ServerBuilder(private val command: String, private val args: String, outputStream: OutputStream) {
    private var process: Process? = null
    private var buildChannel = Channel<Boolean>()
    private var onlineList = mutableListOf<String>()
    private var processWriter: BufferedWriter? = null
    private var processReader: BufferedReader? = null

    private var systemWriter: BufferedWriter = outputStream.bufferedWriter()

    private val eventsChannel = Channel<String>()
    val events = eventsChannel as ReceiveChannel<String>

    var status = State.OFF
    private var serverScope = CoroutineScope(Dispatchers.IO)

    fun startServer() = serverScope.launch(Dispatchers.IO) launch@{
        if (status == State.ON) {
            eventsChannel.send(Commands.Errors.SERVER_IS_ALREADY_RUNNING)

            return@launch
        }

        val pb = ProcessBuilder(command, args).redirectErrorStream(true)

        process = pb.start()
        status = State.ON
        processReader = process!!.inputStream.bufferedReader()
        processWriter = process!!.outputStream.bufferedWriter()

        // start passing logs to channel
        serverScope.launch(Dispatchers.IO) {
            while (process?.isAlive == true) {
                val query = try {
                    processReader!!.readLine() ?: continue
                } catch (e: IOException) {
                    break
                }
                val (shouldLog, logType) = LogParser.shouldLog(query)
                if (shouldLog != null) {
                    if (!listOf(LogType.WHITELIST_OK, LogType.WHITELIST_ERROR).contains(logType)) {
                        eventsChannel.send(shouldLog)
                    }
                }

                when (logType) {
                    LogType.BUILD_FINISHED -> buildChannel.send(true)
                    LogType.WHITELIST_OK -> Users.updatePlayerWhitelistStatus(shouldLog!!)
                    LogType.PLAYER_JOINED -> onlineList.add(LogParser.nicknameFromQuery(shouldLog!!))
                    LogType.PLAYER_LEFT -> onlineList.remove(LogParser.nicknameFromQuery(shouldLog!!))
                    else -> {}
                }

                systemWriter.write(query)
                systemWriter.newLine()
                systemWriter.flush()
            }
            processReader!!.close()
            processReader = null

            processWriter!!.close()
            processWriter = null
        }

        // run lazy operations
        serverScope.launch {
            buildChannel.receive()
            Users.filterNewUsers().forEach(::whitelist)
        }
    }

    fun stopServer() = serverScope.launch {
        if (status == State.OFF) {
            eventsChannel.send(Commands.Errors.SERVER_IS_NOT_RUNNING)

            return@launch
        }
        onlineList.clear()
        writeToServer(Commands.STOP)
        delay(1000)
        process?.destroy()
        status = State.OFF
    }

    fun status(): String {
        if (status == State.OFF) {
            return "Server is down"
        }

        if (onlineList.isEmpty()) {
            return "Server is running. Current online: nobody"
        }

        return "Server is running. Current online: ${onlineList.joinToString(" ")}"
    }

    fun writeToServer(query: String) {
        processWriter ?: return
        processWriter!!.write(query)
        processWriter!!.newLine()
        processWriter!!.flush()
    }

    fun say(text: String) {
        writeToServer("${Commands.SAY} $text")
    }

    fun whitelist(name: String) {
        writeToServer("${Commands.ADD_TO_WHITELIST} $name")
    }

    fun block(name: String) {
        writeToServer("${Commands.REMOVE_FROM_WHITELIST} $name")
    }

    enum class State {
        ON, OFF;
    }
}