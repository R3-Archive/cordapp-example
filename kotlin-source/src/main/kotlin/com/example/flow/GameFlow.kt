package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.CordaGameContract
import com.example.state.CordaGameState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.crypto.Party
import net.corda.core.crypto.composite
import net.corda.core.crypto.signWithECDSA
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap
import net.corda.flows.FinalityFlow

object GameFlow {
    class GameInitiator(val gameState: CordaGameState,
                    val otherParty: Party) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val keyPair = serviceHub.legalIdentityKey

            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

            val txCommand = Command(CordaGameContract.GameCommands.Create(), gameState.participants)

            val unsignedTx = TransactionType.General.Builder(notary).withItems(gameState, txCommand)

            unsignedTx.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            val partSignedTx = unsignedTx.signWith(keyPair).toSignedTransaction(checkSufficientSignatures = false)

            send(otherParty, partSignedTx)
            return waitForLedgerCommit(partSignedTx.id)
        }
    }

    class GameAcceptor(val otherParty: Party) : FlowLogic<Unit>() {
        override fun call() {
            val keyPair = serviceHub.legalIdentityKey
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
            val notaryPubKey = notary.owningKey
            val partSignedTx = receive<SignedTransaction>(otherParty).unwrap { partSignedTx ->
                val wireTx = partSignedTx.verifySignatures(keyPair.public.composite, notaryPubKey)
                wireTx.toLedgerTransaction(serviceHub).verify()
                partSignedTx
            }
            val mySig = keyPair.signWithECDSA(partSignedTx.id.bytes)
            val signedTx = partSignedTx + mySig
            println(partSignedTx)
            println(mySig)
            println(signedTx)
            println(setOf(serviceHub.myInfo.legalIdentity, otherParty))
            subFlow(FinalityFlow(signedTx, setOf(serviceHub.myInfo.legalIdentity, otherParty)))
        }
    }
}