package com.example.model

import com.example.Config.isMain
import com.example.Constants.GENERATION_TERMINATION_CONDITION
import com.example.sha256Hash

object Blockchain {
    var blockChain: MutableList<Block> = mutableListOf()

    fun generateGenesis(): Boolean {
        if (isMain) {
            blockChain.add(
                Block(0, "", sha256Hash("Genesis").dropLast(6).
                plus(GENERATION_TERMINATION_CONDITION), "Genesis", 0)
            )
        }
        return isMain
    }
}