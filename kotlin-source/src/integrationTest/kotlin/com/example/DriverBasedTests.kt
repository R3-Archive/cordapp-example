package com.example

import com.example.flow.IOUFlow
import com.example.state.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import org.junit.Test
import kotlin.test.assertEquals

class DriverBasedTests {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `node test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts two nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (partyAHandle, partyBHandle) = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }

            // We get PartyA to run `IOUFlow.Initiator`.
            val partyB = partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!
            partyAHandle.rpc.startFlowDynamic(IOUFlow.Initiator::class.java, 99, partyB).returnValue.get()

            Thread.sleep(1000)

            // We check that `IOUFlow.Initiator` has created a single IOU in both nodes' vaults.
            listOf(partyAHandle, partyBHandle).forEach { nodeHandle ->
                assertEquals(1, nodeHandle.rpc.vaultQueryBy<IOUState>().states.size)
            }
        }
    }
}