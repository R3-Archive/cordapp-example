package com.example.flow;

import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class IOUFlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("com.example.contract"));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.registerInitiatedFlow(ExampleFlow.Acceptor.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidIOUs() throws Exception {
        // The IOUContract specifies that IOUs cannot have negative values.
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        // The IOUContract specifies that IOUs cannot have negative values.
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }
}