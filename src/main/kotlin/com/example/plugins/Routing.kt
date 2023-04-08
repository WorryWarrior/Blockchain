package com.example.plugins

import com.example.BlockchainController
import com.example.BlockchainRepository
import com.example.Constants
import com.example.Constants.GENERATION_SUCCESS_VALUE
import com.example.NotificationReceivedCallback
import com.example.model.Block
import com.example.model.Blockchain
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*

fun Application.configureRouting() {
    routing {
        get("/") {
            println(Blockchain.blockChain.map { listOf(it.index, it.prevHash, it.hash).joinToString(separator = " ") })
            call.respond(Blockchain.blockChain.map { listOf(it.index, it.prevHash, it.hash).joinToString(separator = " ") })
        }
    }
}

fun Application.configureAskThirdNodeReceiverRouting() {
    routing {
        get(Constants.ASK_THIRD_NODE_ROUTING) {
            call.respond(BlockchainRepository.get().getLastBlock())
        }
    }
}

fun Application.configureValidationReceiverRouting() {
    routing {
        get(Constants.VALIDATE_BLOCKCHAIN_ROUTING) {
            call.respond(Blockchain.blockChain)
        }
    }
}

fun Application.configureNotificationReceiverRouting(notificationReceivedCallback: NotificationReceivedCallback) {
    routing {
        post(Constants.BLOCK_INSERTED_ROUTING) {
            val block = call.receive(Block::class)
            val senderNodePort = call.request.headers[Constants.PORT]
            if (senderNodePort != null) {
                BlockchainController.handleReceivedBlock(block, senderNodePort, notificationReceivedCallback)
            }
            call.respond(GENERATION_SUCCESS_VALUE)
        }
    }
}
