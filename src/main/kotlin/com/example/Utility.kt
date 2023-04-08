package com.example

import com.example.Constants.HASH_METHOD
import com.example.Constants.STRING_LENGTH
import com.example.Constants.charPool
import kotlin.random.Random
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter


object Constants {
    const val HASH_METHOD = "SHA-256"
    val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    const val STRING_LENGTH = 256

    const val GENERATION_TERMINATION_CONDITION = "000000"
    const val GENERATION_SUCCESS_VALUE = 1
    const val BASE_URL = "http://127.0.0.1:"
    const val BLOCK_INSERTED_ROUTING = "/notify_block_inserted"
    const val VALIDATE_BLOCKCHAIN_ROUTING = "/validate_blockchain"
    const val ASK_THIRD_NODE_ROUTING = "/ask_third_node"

    const val PORT = "PORT"
}

object Config {
    var isMain = false
    var firstNodePort = ""
    var secondNodePort = ""
    var currentNodePort = ""
}

fun generateRandomData(): String {
    return (1..STRING_LENGTH)
        .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
        .joinToString("")
}

fun sha256Hash(input: String): String {
    val bytes = MessageDigest
        .getInstance(HASH_METHOD)
        .digest(input.toByteArray())
    //println("Bytes: ${DatatypeConverter.printHexBinary(bytes).uppercase()}")
    return DatatypeConverter.printHexBinary(bytes).uppercase()
}