package com.example.contract;

import com.example.flow.ExampleFlow;
import com.example.model.IOU;
import com.example.state.IOUState;
import com.google.common.util.concurrent.ListenableFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.TestConstants;
import net.corda.testing.node.MockNetwork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.node.utilities.DatabaseSupportKt.databaseTransaction;
import static org.junit.Assert.*;

public class IOUFlowTests {
    private MockNetwork net;
    private MockNetwork.MockNode a;
    private MockNetwork.MockNode b;
    private MockNetwork.MockNode c;

    @Before
    public void setup() {
        net = new MockNetwork();
        MockNetwork.BasketOfNodes nodes = net.createSomeNodes(3, MockNetwork.DefaultFactory.INSTANCE, TestConstants.getDUMMY_NOTARY_KEY());
        a = nodes.getPartyNodes().get(0);
        b = nodes.getPartyNodes().get(1);
        c = nodes.getPartyNodes().get(2);
        net.runNetwork(-1);
    }

    @After
    public void tearDown() {
        net.stopNodes();
    }

    @Test
    public void flowRejectsInvalidIOUs() throws InterruptedException {
        IOUState state = new IOUState(
                new IOU(-1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork(-1);

        // The IOUContract specifies that IOUs cannot have negative values.
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TransactionVerificationException.ContractRejection);
        }
    }

    @Test
    public void flowRejectsInvalidIOUStates() throws InterruptedException {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                a.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork(-1);

        // The IOUContract specifies that an IOU's sender and recipient cannot be the same.
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TransactionVerificationException.ContractRejection);
        }
    }
}