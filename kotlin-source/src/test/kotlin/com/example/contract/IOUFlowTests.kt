package com.example.contract

import com.example.flow.ExampleFlow
import com.example.model.IOU
import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
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
}