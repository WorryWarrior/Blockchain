package com.example

import com.example.Config.currentNodePort
import com.example.Config.firstNodePort
import com.example.Config.isMain
import com.example.Config.secondNodePort
import com.example.Constants.BLOCK_INSERTED_ROUTING
import com.example.Constants.GENERATION_SUCCESS_VALUE
import com.example.Constants.GENERATION_TERMINATION_CONDITION
import com.example.model.Block
import com.example.model.Blockchain
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.utils.io.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class ApplicationTest {

    @Test
    fun testGenesisValidation() {
        initMainNode()
        isMain = false
        assertEquals(false, Blockchain.generateGenesis())
        isMain = true
        assertEquals(true, Blockchain.generateGenesis())
        val genesis = Blockchain.blockChain[0]
        assertEquals(GENERATION_TERMINATION_CONDITION, genesis.hash.takeLast(6))
        assertEquals("Genesis", genesis.data)
    }

    @Test
    fun testBlockchainManualValidation() {
        initMainNode()
        Blockchain.generateGenesis()
        val genesis = Blockchain.blockChain[0]
        val secondBlock: Block?
        val thirdBlock: Block?
        val fourthBlock: Block?
        runBlocking {
            secondBlock = BlockchainController.generateBlock()
            thirdBlock = BlockchainController.generateBlock()
            fourthBlock = BlockchainController.generateBlock()
        }
        assertEquals(genesis.hash, secondBlock?.prevHash)
        assertEquals(secondBlock?.hash, thirdBlock?.prevHash)
        assertEquals(thirdBlock?.hash, fourthBlock?.prevHash)
        assertEquals(4, Blockchain.blockChain.size)
        assertEquals(GENERATION_TERMINATION_CONDITION, genesis.hash.takeLast(6))
        assertEquals(GENERATION_TERMINATION_CONDITION, secondBlock?.hash?.takeLast(6))
        assertEquals(GENERATION_TERMINATION_CONDITION, thirdBlock?.hash?.takeLast(6))
        assertEquals(GENERATION_TERMINATION_CONDITION, fourthBlock?.hash?.takeLast(6))

        assertEquals(0, genesis.index)
        assertEquals(1, secondBlock?.index)
        assertEquals(2, thirdBlock?.index)
        assertEquals(3, fourthBlock?.index)
    }

    @Test
    fun testBlockNotificationInsertion() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel("$GENERATION_SUCCESS_VALUE"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = BlockchainRepository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                true,
                client.notifyBlockInserted(Blockchain.blockChain.last(), BLOCK_INSERTED_ROUTING + firstNodePort)
            )
        }
    }

    @Test
    fun testBlockInsertedNotificationFailed() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel("0"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = BlockchainRepository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                false,
                client.notifyBlockInserted(Blockchain.blockChain.last(), BLOCK_INSERTED_ROUTING + firstNodePort)
            )
        }
    }

    @Test
    fun testThirdNode() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val mockEngine = MockEngine {
            GsonBuilder().create().toJson(Blockchain.blockChain.last())
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(Blockchain.blockChain.last())),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = BlockchainRepository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                Blockchain.blockChain.last(),
                client.askThirdNode(BLOCK_INSERTED_ROUTING + secondNodePort)
            )
        }
    }

    @Test
    fun testLastBlockSending() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        assertEquals(Blockchain.blockChain.last(), Blockchain.blockChain.last())
    }

    @Test
    fun testBlockchainValidation() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(Blockchain.blockChain)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = BlockchainRepository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                Blockchain.blockChain,
                client.validateBlockChain(BLOCK_INSERTED_ROUTING + firstNodePort)
            )
        }
    }

    @Test
    fun testBlockchainValidationUnsuccessful() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(listOf(Blockchain.blockChain.first()))),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = BlockchainRepository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertNotEquals(
                Blockchain.blockChain,
                client.validateBlockChain(BLOCK_INSERTED_ROUTING + firstNodePort)
            )
        }
    }

    @Test
    fun testBlockInsertion() {
        initMainNode()
        Blockchain.generateGenesis()
        val blockToInsert: Block?
        runBlocking {
            blockToInsert = BlockchainController.generateBlock()
        }
        Blockchain.blockChain.dropLast(1)
        if (blockToInsert != null) {
            BlockchainRepository.get().insertBlock(blockToInsert)
        }
        assertEquals(blockToInsert, Blockchain.blockChain.last())
    }

    @Test
    fun testBlockReception() {
        initMainNode()
        Blockchain.generateGenesis()
        runBlocking {
            BlockchainController.generateBlock()
        }
        val receivedBlock = Block(
            Blockchain.blockChain.size,
            Blockchain.blockChain.last().hash,
            sha256Hash("Received block").dropLast(6).plus(GENERATION_TERMINATION_CONDITION),
            "receivedBlock", 0)

        runBlocking {
            BlockchainController.handleReceivedBlock(receivedBlock, firstNodePort)
        }
        assertEquals(receivedBlock, Blockchain.blockChain.last())
    }

    private fun initMainNode() {
        Blockchain.blockChain.clear()
        currentNodePort = "8081"
        firstNodePort = "8082"
        secondNodePort = "8083"
        isMain = true
    }
}
