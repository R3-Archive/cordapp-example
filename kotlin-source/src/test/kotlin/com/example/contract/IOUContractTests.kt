package com.example.contract

import com.example.model.IOU
import com.example.state.IOUState
import net.corda.testing.*
import org.junit.Test

class IOUContractTests {
    @Test
    fun `transaction must include Create command`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                fails()
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val iou = IOU(1)
        ledger {
            transaction {
                input { IOUState(iou, MINI_CORP, MEGA_CORP) }
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `sender must sign transaction`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `recipient must sign transaction`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `sender is not recipient`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MEGA_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The sender and the recipient cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        val iou = IOU(-1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}