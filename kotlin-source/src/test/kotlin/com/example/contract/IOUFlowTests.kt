package com.example.contract

import com.example.flow.ExampleFlow
import com.example.model.IOU
import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.Party
import net.corda.core.getOrThrow
import net.corda.node.utilities.databaseTransaction
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IOUFlowTests {
    lateinit var net: MockNetwork
    lateinit var a: MockNetwork.MockNode
    lateinit var b: MockNetwork.MockNode
    lateinit var c: MockNetwork.MockNode
    lateinit var notary: Party

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(3)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        c = nodes.partyNodes[2]
        notary = nodes.notaryNode.info.notaryIdentity
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    @Test
    fun `the flow rejects invalid IOUs`() {
        val state = IOUState(
                IOU(-1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> {future.getOrThrow()}
    }

    @Test
    fun `the flow rejects invalid IOU states`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                a.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        // The IOUContract specifies that an IOU's sender and recipient cannot be the same.
        assertFailsWith<TransactionVerificationException> {future.getOrThrow()}
    }

//    @Test
//    fun `the flow rejects IOUs that are not signed by the sender`() {
//        val state = IOUState(
//                IOU(1),
//                c.info.legalIdentity,
//                b.info.legalIdentity,
//                IOUContract())
//        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//
//        assertFails {future.getOrThrow()}
//    }
//
//    @Test
//    fun `the flow rejects IOUs that are not signed by the recipient`() {
//        val state = IOUState(
//                IOU(1),
//                a.info.legalIdentity,
//                b.info.legalIdentity,
//                IOUContract())
//        val flow = ExampleFlow.Initiator(state, c.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//
//        assertFailsWith<RuntimeException> {future.getOrThrow()}
//    }

    @Test
    fun `the flow records a transaction in both parties' vaults`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val signedTx = future.getOrThrow()

        databaseTransaction(a.database) {
            assertEquals(signedTx, a.storage.validatedTransactions.getTransaction(signedTx.id))
        }
        databaseTransaction(b.database) {
            assertEquals(signedTx, b.storage.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `the recorded transaction has no inputs and a single output, the input IOU`() {
        val inputState = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(inputState, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val signedTx = future.getOrThrow()

        databaseTransaction(a.database) {
            val recordedTx = a.storage.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as IOUState
            assertEquals(recordedState.iou, inputState.iou)
            assertEquals(recordedState.sender, inputState.sender)
            assertEquals(recordedState.recipient, inputState.recipient)
            assertEquals(recordedState.linearId, inputState.linearId)
        }
        databaseTransaction(b.database) {
            assertEquals(signedTx, b.storage.validatedTransactions.getTransaction(signedTx.id))
        }
    }
}