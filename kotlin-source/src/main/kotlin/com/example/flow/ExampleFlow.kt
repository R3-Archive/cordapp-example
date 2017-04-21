package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.IOUContract
import com.example.flow.ExampleFlow.Acceptor
import com.example.flow.ExampleFlow.Initiator
import com.example.state.IOUState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.crypto.Party
import net.corda.core.crypto.signWithECDSA
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.flows.FinalityFlow

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object ExampleFlow {
    class Initiator(val iou: IOUState,
                    val otherParty: Party): FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object SENDING_TRANSACTION : ProgressTracker.Step("Sending proposed transaction to recipient for review.")

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    SENDING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Prep.
            // Obtain a reference to our key pair. Currently, the only key pair used is the one which is registered
            // with the NetWorkMapService. In a future milestone release we'll implement HD key generation so that
            // new keys can be generated for each transaction.
            val keyPair = serviceHub.legalIdentityKey
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val txCommand = Command(IOUContract.Commands.Create(), iou.participants)
            val unsignedTx = TransactionType.General.Builder(notary).withItems(iou, txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            unsignedTx.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = unsignedTx.signWith(keyPair).toSignedTransaction(checkSufficientSignatures = false)

            // Stage 4.
            progressTracker.currentStep = SENDING_TRANSACTION
            // Send the state across the wire to the designated counterparty.
            // -----------------------
            // Flow jumps to Acceptor.
            // -----------------------
            send(otherParty, partSignedTx)

            return waitForLedgerCommit(partSignedTx.id)
        }
    }

    class Acceptor(val otherParty: Party) : FlowLogic<Unit>() {
        companion object {
            object RECEIVING_TRANSACTION : ProgressTracker.Step("Receiving proposed transaction from sender.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying signatures and contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing proposed transaction with our private key.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")

            fun tracker() = ProgressTracker(
                    RECEIVING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call() {
            // Prep.
            // Obtain a reference to our key pair.
            val keyPair = serviceHub.legalIdentityKey
            // Obtain a reference to the notary we want to use and its public key.
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
            val notaryPubKey = notary.owningKey

            // Stage 5.
            progressTracker.currentStep = RECEIVING_TRANSACTION
            // All messages come off the wire as UntrustworthyData. You need to 'unwrap' them. This is where you
            // validate what you have just received.
            val partSignedTx = receive<SignedTransaction>(otherParty).unwrap { partSignedTx ->
                // Stage 6.
                progressTracker.currentStep = VERIFYING_TRANSACTION
                // Check that the signature of the other party is valid.
                // Our signature and the notary's signature are allowed to be omitted at this stage as this is only
                // a partially signed transaction.
                val wireTx = partSignedTx.verifySignatures(keyPair.public, notaryPubKey)
                // Run the contract's verify function.
                // We want to be sure that the agreed-upon IOU is valid under the rules of the contract.
                // To do this we need to run the contract's verify() function.
                wireTx.toLedgerTransaction(serviceHub).verify()
                // We've verified the signed transaction and return it.
                partSignedTx
            }

            // Stage 7.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction with our key pair and add it to the transaction.
            // We now have 'validation consensus'. We still require uniqueness consensus.
            // Technically validation consensus for this type of agreement implicitly provides uniqueness consensus.
            val mySig = keyPair.signWithECDSA(partSignedTx.id.bytes)
            // Add our signature to the transaction.
            val signedTx = partSignedTx + mySig

            // Stage 8.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // FinalityFlow() notarises the transaction and records it in each party's vault.
            subFlow(FinalityFlow(signedTx, setOf(serviceHub.myInfo.legalIdentity, otherParty)))
        }
    }
}