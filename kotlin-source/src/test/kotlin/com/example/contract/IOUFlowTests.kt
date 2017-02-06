package com.example.contract

import com.example.flow.ExampleFlow
import com.example.flow.ExampleFlowResult
import com.example.model.IOU
import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.Party
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
    lateinit var notary: Party

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes()
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        notary = nodes.notaryNode.info.notaryIdentity
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    @Test
    fun `IOU is positively valued`() {
        val state = IOUState(
                IOU(-1),
                a.info.legalIdentity,
                b.info.legalIdentity,
                IOUContract())
        val flow = ExampleFlow.Initiator(state, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val result = future.getOrThrow()
        assertEquals(
                result.toString(),
                "Failure(java.lang.IllegalArgumentException: Failed requirement: The IOU's value must be non-negative.)")
    }
}
