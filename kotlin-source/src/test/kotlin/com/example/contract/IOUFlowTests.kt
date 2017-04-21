package com.example.contract

import com.example.flow.ExampleFlow
import com.example.model.IOU
import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.getOrThrow
import net.corda.node.services.statemachine.FlowSessionException
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

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(3)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        c = nodes.partyNodes[2]
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    @Test
    fun `flow rejects invalid IOUs`() {
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
    fun `flow rejects invalid IOU states`() {
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

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignatures(b.services.legalIdentityKey.public)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignatures(a.services.legalIdentityKey.public)
    }

    @Test
    fun `flow rejects IOUs that are not signed by the sender`() {
        val state = IOUState(
                IOU(1),
                c.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        assertFailsWith<FlowSessionException> { future.getOrThrow() }
    }

    @Test
    fun `flow rejects IOUs that are not signed by the recipient`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                c.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        assertFailsWith<FlowSessionException> { future.getOrThrow() }
    }

    @Test
    fun `flow records a transaction in both parties' vaults`() {
        val state = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.storage.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input IOU`() {
        val inputState = IOUState(
                IOU(1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(inputState, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.storage.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as IOUState
            assertEquals(recordedState.iou, inputState.iou)
            assertEquals(recordedState.sender, inputState.sender)
            assertEquals(recordedState.recipient, inputState.recipient)
            assertEquals(recordedState.linearId, inputState.linearId)
        }
    }
}