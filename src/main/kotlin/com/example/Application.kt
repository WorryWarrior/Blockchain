package com.example

import com.example.Config.currentNodePort
import com.example.Config.firstNodePort
import com.example.Config.isMain
import com.example.Config.secondNodePort
import com.example.model.Blockchain
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import java.net.ConnectException

fun main(args: Array<String>) {
    currentNodePort = args[0]
    firstNodePort = args[1]
    secondNodePort = args[2]
    isMain = args[3] == "1"
    embeddedServer(CIO, port = currentNodePort.toInt(), host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()

    if (isMain) {
        Blockchain.generateGenesis()
    }

    configureAskThirdNodeReceiverRouting()
    configureRouting()
    configureValidationReceiverRouting()
    var job = if (isMain) {
        startMining(this)
    } else { // if current node isn't main it just asks for actual blockChain every 0.5 second until gets it
        waitForMainNode(this)
    }
    configureNotificationReceiverRouting(object : NotificationReceivedCallback {
        override fun onNotificationReceived() {
            println("Notification received => restarting coroutine and mining")
            job.cancel()
            job = startMining(this@module)
        }

    })
}

private fun startMining(coroutineScope: CoroutineScope): Job {
    return coroutineScope.launch(Dispatchers.Default) {
        while (true) {
//            if (BlockChain.blockChain.size % 10 == 0 && !mainNode) { // validate blockchain every 10 blocks
//                BlockChainController.validateBlockChain()
//            }
            BlockchainController.generateBlock()
        }
    }
}

private fun waitForMainNode(coroutineScope: CoroutineScope): Job {
    return coroutineScope.launch(Dispatchers.IO) {
        while (Blockchain.blockChain.isEmpty()) {
            try {
                BlockchainController.validateBlockchain()
            } catch (_: ConnectException) {
            }
            delay(500)
        }
        cancel()
        startMining(coroutineScope)
    }
}
