package com.example.contract

import com.example.state.IOUState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the sender and the recipient.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class IOUContract : Contract {
    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: TransactionForContract) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputs.single() as IOUState
            "The sender and the recipient cannot be the same entity." using (out.sender != out.recipient)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // IOU-specific constraints.
            "The IOU's value must be non-negative." using (out.iou.value > 0)
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("IOU contract template and params")
}
