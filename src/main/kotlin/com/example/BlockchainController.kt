package com.example

import com.example.Constants.ASK_THIRD_NODE_ROUTING
import com.example.Constants.BASE_URL
import com.example.Constants.BLOCK_INSERTED_ROUTING
import com.example.Constants.GENERATION_TERMINATION_CONDITION
import com.example.Constants.VALIDATE_BLOCKCHAIN_ROUTING
import com.example.Config.currentNodePort
import com.example.Config.firstNodePort
import com.example.Config.secondNodePort
import com.example.model.Block
import com.example.model.Blockchain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BlockchainController {

    private val firstUrl = BASE_URL + firstNodePort
    private val secondUrl = BASE_URL + secondNodePort

    suspend fun validateBlockchain(): Boolean {
        val requestedBlockChain = BlockchainRepository.get().validateBlockChain(firstUrl + VALIDATE_BLOCKCHAIN_ROUTING)
        return if (requestedBlockChain == Blockchain.blockChain) {
            println("Node is ACTUAL")
            true
        } else {
            Blockchain.blockChain = requestedBlockChain
            println("Node is in MINORITY")
            false
        }
    }

    suspend fun generateBlock(): Block? {
        var gotBlock = false
        val prevBlock = Blockchain.blockChain.lastOrNull()
        if (prevBlock == null) {
            BlockchainRepository.get().validateBlockChain(firstUrl + VALIDATE_BLOCKCHAIN_ROUTING)
            return null
        }
        val currentBlock = Block(
            Blockchain.blockChain.size,
            prevBlock.hash,
            "",
            generateRandomData(),
            0
        )
        while (!gotBlock) {
            val hashOutput =
                sha256Hash("${currentBlock.index}${currentBlock.prevHash}${currentBlock.data}${currentBlock.nonce}")

            if (hashOutput.takeLast(6) == GENERATION_TERMINATION_CONDITION) {
                gotBlock = true
                currentBlock.hash = hashOutput
                println("Successfully generated block with $hashOutput. Inserting it")
                handleReceivedBlock(currentBlock, currentNodePort)

                withContext(Dispatchers.IO) {
                    try {
                        BlockchainRepository.get().notifyBlockInserted(currentBlock, firstUrl + BLOCK_INSERTED_ROUTING)
                        BlockchainRepository.get().notifyBlockInserted(currentBlock, secondUrl + BLOCK_INSERTED_ROUTING)
                    } catch (_: Exception) {}
                }
            } else {
                currentBlock.nonce++
            }
        }
        return currentBlock
    }

    suspend fun handleReceivedBlock(
        block: Block,
        senderNodePort: String,
        notificationReceivedCallback: NotificationReceivedCallback? = null
    ) {
        val lastBlock = Blockchain.blockChain.last()
        if (block.index == lastBlock.index + 1 && block.prevHash == lastBlock.hash) {
            BlockchainRepository.get().insertBlock(block)
            notificationReceivedCallback?.onNotificationReceived()
        } else if (senderNodePort != currentNodePort) {
            val askUrl = if (senderNodePort == firstNodePort) secondUrl + ASK_THIRD_NODE_ROUTING else firstUrl + ASK_THIRD_NODE_ROUTING
            val thirdNodeBlock = BlockchainRepository.get().askThirdNode(askUrl)
            if (thirdNodeBlock.hash == block.hash) {
                val validateUrl = if (senderNodePort == firstNodePort) secondUrl + VALIDATE_BLOCKCHAIN_ROUTING else firstUrl + VALIDATE_BLOCKCHAIN_ROUTING
                BlockchainRepository.get().validateBlockChain(validateUrl)
            }
        }
    }
}

interface NotificationReceivedCallback {
    fun onNotificationReceived()
}