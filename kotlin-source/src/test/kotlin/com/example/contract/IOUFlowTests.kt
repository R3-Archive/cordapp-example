package com.example.contract

import com.example.flow.ExampleFlow
import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.getOrThrow
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
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        nodes.partyNodes.forEach { it.registerInitiatedFlow(com.example.flow.ExampleFlow.Acceptor::class.java) }
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    @Test
    fun `flow rejects invalid IOUs`() {
        val flow = ExampleFlow.Initiator(-1, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> {future.getOrThrow()}
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val flow = ExampleFlow.Initiator(1, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignatures(b.services.legalIdentityKey)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val flow = ExampleFlow.Initiator(1, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignatures(a.services.legalIdentityKey)
    }

    @Test
    fun `flow records a transaction in both parties' vaults`() {
        val flow = ExampleFlow.Initiator(1, b.info.legalIdentity)
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
        val iouValue = 1
        val flow = ExampleFlow.Initiator(iouValue, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.storage.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as IOUState
            assertEquals(recordedState.iou.value, iouValue)
            assertEquals(recordedState.sender, a.info.legalIdentity)
            assertEquals(recordedState.recipient, b.info.legalIdentity)
        }
    }
}