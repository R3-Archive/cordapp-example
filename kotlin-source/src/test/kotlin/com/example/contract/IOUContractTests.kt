package com.example.contract

import com.example.contract.IOUContract.Companion.IOU_CONTRACT_ID
import com.example.model.IOU
import com.example.state.IOUState
import net.corda.testing.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class IOUContractTests {

    @Before
    fun setup() {
        setCordappPackages("com.example.contract")
    }

    @After
    fun tearDown() {
        unsetCordappPackages()
    }

    @Test
    fun `transaction must include Create command`() {
        val iou = IOU(1)
        ledger {
            transaction {
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
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
                input(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
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
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
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
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
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
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
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
                output(IOU_CONTRACT_ID) { IOUState(iou, MEGA_CORP, MEGA_CORP) }
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
                output(IOU_CONTRACT_ID) { IOUState(iou, MINI_CORP, MEGA_CORP) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}