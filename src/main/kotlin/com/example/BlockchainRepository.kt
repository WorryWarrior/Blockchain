package com.example

import com.example.Constants.PORT
import com.example.model.Block
import com.example.model.Blockchain
import com.example.Config.currentNodePort
import com.example.Constants.GENERATION_SUCCESS_VALUE
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*

interface IBlockchainRepository {
    suspend fun notifyBlockInserted(block: Block, url: String): Boolean
    suspend fun askThirdNode(url: String): Block
    fun getLastBlock(): Block
    suspend fun validateBlockChain(url: String): MutableList<Block>
    fun insertBlock(block: Block)
}

class BlockchainRepository(private val client: HttpClient): IBlockchainRepository {
    override suspend fun notifyBlockInserted(block: Block, url: String): Boolean =
        client.post(url) {
            contentType(ContentType.Application.Json)
            header(PORT, currentNodePort)
            setBody(block)
        }.bodyAsText() == "$GENERATION_SUCCESS_VALUE"

    override suspend fun askThirdNode(url: String): Block =
        client.get(url) {
            contentType(ContentType.Application.Json)
        }.body()

    override fun getLastBlock(): Block =
        Blockchain.blockChain.last()

    override suspend fun validateBlockChain(url: String): MutableList<Block> =
        client.get(url) {
            contentType(ContentType.Application.Json)
        }.body()

    override fun insertBlock(block: Block) {
        Blockchain.blockChain.add(block)
        println("Blockchain size: ${Blockchain.blockChain.size}. Last hash: ${Blockchain.blockChain.last().hash}")
    }

    companion object {
        private var instance: IBlockchainRepository? = null

        fun get(): IBlockchainRepository {
            if (instance == null) {
                instance = BlockchainRepository(HttpClient(CIO) {
                    install(ContentNegotiation) {
                        gson()
                    }
                })
            }
            return instance!!
        }
    }
}