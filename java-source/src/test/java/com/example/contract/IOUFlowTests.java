package com.example.contract;

import com.example.flow.ExampleFlow;
import com.example.model.IOU;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetwork.BasketOfNodes;
import net.corda.testing.node.MockNetwork.MockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;


public class IOUFlowTests {
    private MockNetwork net;
    private MockNode a;
    private MockNode b;
    private MockNode c;

    @Before
    public void setup() {
        net = new MockNetwork();
        BasketOfNodes nodes = net.createSomeNodes(3);
        a = nodes.getPartyNodes().get(0);
        b = nodes.getPartyNodes().get(1);
        c = nodes.getPartyNodes().get(2);
        net.runNetwork();
    }

    @After
    public void tearDown() {
        net.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidIOUs() throws Exception {
        // The IOUContract specifies that IOUs cannot have negative values.
        IOUState state = new IOUState(
                new IOU(-1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());

        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void flowRejectsInvalidIOUStates() throws Exception {
        // The IOUContract specifies that an IOU's sender and recipient cannot be the same.
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                a.info.getLegalIdentity(),
                new IOUContract());

        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignatures(b.getServices().getLegalIdentityKey().getPublic());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignatures(a.getServices().getLegalIdentityKey().getPublic());
    }

    @Test
    public void flowRejectsIOUsThatAreNotSignedByTheSender() throws Exception {
        IOUState state = new IOUState(
                new IOU(1),
                c.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());

        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void flowRejectsIOUsThatAreNotSignedByTheRecipient() throws Exception {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                c.info.getLegalIdentity(),
                new IOUContract());

        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesVaults() throws Exception {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();
        SignedTransaction signedTx = future.get();

        for (MockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.storage.getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputIOU() throws Exception {
        IOUState inputState = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(inputState, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();
        SignedTransaction signedTx = future.get();

        for (MockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.storage.getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert(txOutputs.size() == 1);

            IOUState recordedState = (IOUState) txOutputs.get(0).getData();
            assertEquals(recordedState.getIOU().getValue(), inputState.getIOU().getValue());
            assertEquals(recordedState.getSender(), inputState.getSender());
            assertEquals(recordedState.getRecipient(), inputState.getRecipient());
            assertEquals(recordedState.getLinearId(), inputState.getLinearId());
        }
    }
}