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
    fun `invalid IOUs are rejected`() {
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
    fun `invalid IOU states are rejected`() {
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
//    fun `IOUs not signed by the sender are rejected`() {
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
//    fun `IOUs not signed by the recipient are rejected`() {
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
    fun `valid IOUs are recorded`() {
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
            assertEquals(signedTx, b.storage.validatedTransactions.getTransaction(signedTx.id))
        }
        databaseTransaction(b.database) {
            assertEquals(signedTx, b.storage.validatedTransactions.getTransaction(signedTx.id))
        }
    }
}