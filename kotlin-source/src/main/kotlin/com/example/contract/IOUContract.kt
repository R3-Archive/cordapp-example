package com.example.contract

import com.example.state.CordaGameState
import com.example.state.IOUState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.games.GameMove
import net.corda.games.GameRules

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
            "No inputs should be consumed when issuing an IOU." by (tx.inputs.isEmpty())
            "Only one output state should be created." by (tx.outputs.size == 1)
            val out = tx.outputs.single() as IOUState
            "The sender and the recipient cannot be the same entity." by (out.sender != out.recipient)
            "All of the participants must be signers." by (command.signers.containsAll(out.participants))

            // IOU-specific constraints.
            "The IOU's value must be non-negative." by (out.iou.value > 0)
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

//open class CordaGameMoveCommand(val gameMove: GameMove)

data class CordaGameRules(val gameRules: GameRules)

open class CordaGameContract(val cordaGameRules: CordaGameRules) : Contract {
    override val legalContractReference: SecureHash = SecureHash.sha256("Game Rules contract template and params")

    interface GameCommands : CommandData {
        class Create : GameCommands
        class Move(val gameMove: GameMove) : GameCommands
    }

    override fun verify(tx: TransactionForContract) {
        val command = tx.commands.single().value as GameCommands
        if (command is GameCommands.Create) { return }
        if (command is GameCommands.Move) {
            val inbound = tx.inputs.single() as CordaGameState
            val outbound = tx.outputs.single() as CordaGameState
            requireThat {
                "Game follows game rules" by cordaGameRules.gameRules.verify(inbound.board, command.gameMove, outbound.board)
            }
        }
    }
}