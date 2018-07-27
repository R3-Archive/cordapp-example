package com.example;

import com.example.flow.IOUFlow;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.junit.Test;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

public class DriverBasedTests {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));

    @Test
    public void nodeTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                // We get PartyA to run `IOUFlow.Initiator`.
                Party partyB = partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName());
                partyAHandle.getRpc().startFlowDynamic(IOUFlow.Initiator.class, 99, partyB).getReturnValue().get();

                Thread.sleep(1000);

                // We check that `IOUFlow.Initiator` has created a single IOU in both nodes' vaults.
                ImmutableList.of(partyAHandle, partyBHandle).forEach(
                        nodeHandle -> assertEquals(1, nodeHandle.getRpc().vaultQuery(IOUState.class).getStates().size()));

            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}