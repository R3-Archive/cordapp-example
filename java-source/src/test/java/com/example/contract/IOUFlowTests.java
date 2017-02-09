package com.example.contract;

import com.example.flow.ExampleFlow;
import com.example.model.IOU;
import com.example.state.IOUState;
import com.google.common.util.concurrent.ListenableFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.transactions.SignedTransaction;
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
        MockNetwork.BasketOfNodes nodes = net.createSomeNodes(3);
        a = nodes.getPartyNodes().get(0);
        b = nodes.getPartyNodes().get(1);
        c = nodes.getPartyNodes().get(2);
        net.runNetwork();
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
        net.runNetwork();

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
        net.runNetwork();

        // The IOUContract specifies that an IOU's sender and recipient cannot be the same.
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TransactionVerificationException.ContractRejection);
        }
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
        signedTx.verifySignatures(CryptoUtilities.getComposite(a.getServices().getLegalIdentityKey().getPublic()));
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
        signedTx.verifySignatures(CryptoUtilities.getComposite(b.getServices().getLegalIdentityKey().getPublic()));
    }

    @Test
    public void flowRejectsIOUsThatAreNotSignedByTheSender() throws InterruptedException {
        IOUState state = new IOUState(
                new IOU(1),
                c.info.getLegalIdentity(),
                b.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        try {
            future.get();
            fail("Expecting flow result to throw an exception");
        } catch (ExecutionException e) {}
    }

    @Test
    public void flowRejectsIOUsThatAreNotSignedByTheRecipient() throws InterruptedException {
        IOUState state = new IOUState(
                new IOU(1),
                a.info.getLegalIdentity(),
                c.info.getLegalIdentity(),
                new IOUContract());
        ExampleFlow.Initiator flow = new ExampleFlow.Initiator(state, b.info.getLegalIdentity());
        ListenableFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        net.runNetwork();

        try {
            future.get();
            fail("Expecting flow result to throw an exception");
        } catch (ExecutionException e) {}
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

        databaseTransaction(a.database, it -> {
            assertEquals(signedTx, a.storage.getValidatedTransactions().getTransaction(signedTx.getId()));
            return null;
        });

        databaseTransaction(b.database, it -> {
            assertEquals(signedTx, b.storage.getValidatedTransactions().getTransaction(signedTx.getId()));
            return null;
        });
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

        databaseTransaction(a.database, it -> {
            SignedTransaction recordedTx = a.storage.getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert(txOutputs.size() == 1);

            IOUState recordedState = (IOUState) txOutputs.get(0).getData();
            assertEquals(recordedState.getIOU().getValue(), inputState.getIOU().getValue());
            assertEquals(recordedState.getSender(), inputState.getSender());
            assertEquals(recordedState.getRecipient(), inputState.getRecipient());
            assertEquals(recordedState.getLinearId(), inputState.getLinearId());
            return null;
        });

        databaseTransaction(b.database, it -> {
            SignedTransaction recordedTx = b.storage.getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert(txOutputs.size() == 1);

            IOUState recordedState = (IOUState) txOutputs.get(0).getData();
            assertEquals(recordedState.getIOU().getValue(), inputState.getIOU().getValue());
            assertEquals(recordedState.getSender(), inputState.getSender());
            assertEquals(recordedState.getRecipient(), inputState.getRecipient());
            assertEquals(recordedState.getLinearId(), inputState.getLinearId());
            return null;
        });
    }
}