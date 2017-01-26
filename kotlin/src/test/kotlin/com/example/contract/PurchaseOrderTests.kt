package com.example.contract

import com.example.model.IOU
import com.example.state.IOUState
import net.corda.core.utilities.TEST_TX_TIME
import net.corda.testing.*
import org.junit.Test

class PurchaseOrderTests {
    @Test
    fun `transaction must include Create command`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP, IOUContract()) }
                timestamp(TEST_TX_TIME)
                fails()
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                verifies()
            }
        }
    }

    @Test
    fun `buyer must sign transaction`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP, IOUContract()) }
                timestamp(TEST_TX_TIME)
                command(MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `seller must sign transaction`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP, IOUContract()) }
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        val iou = IOU(-1)
        ledger {
            transaction {
                output { IOUState(iou, MINI_CORP, MEGA_CORP, IOUContract()) }
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}
