package com.example.state

import com.example.contract.CordaGameContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party
import net.corda.games.GameBoard
import java.security.PublicKey


data class CordaGameState(val me: Party,
                          val you: Party,
                          val board: GameBoard,
                          override val contract: CordaGameContract,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean = true

    override val participants: List<CompositeKey> get() = listOf(me, you).map { it.owningKey }

}
