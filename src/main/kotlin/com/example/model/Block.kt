package com.example.model

data class Block(
    val index: Int,
    val prevHash: String,
    var hash: String,
    val data: String,
    var nonce: Int
)