package com.example.contract;

import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.corda.core.identity.Party;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.PublicKey;

import static com.example.contract.IOUContract.IOU_CONTRACT_ID;
import static net.corda.testing.CoreTestUtils.*;
import static net.corda.testing.NodeTestUtils.ledger;

public class IOUContractTests {
    static private final Party miniCorp = getMINI_CORP();
    static private final Party megaCorp = getMEGA_CORP();
    static private final PublicKey[] keys = Iterables.toArray(ImmutableList.of(getMEGA_CORP_PUBKEY(), getMINI_CORP_PUBKEY()), PublicKey.class);

    @Before
    public void setup() {
        setCordappPackages("com.example.contract");
    }

    @After
    public void tearDown() {
        unsetCordappPackages();
    }

    @Test
    public void transactionMustIncludeCreateCommand() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                txDSL.fails();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveNoInputs() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.input(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneOutput() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void borrowerMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderIsNotBorrower() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, megaCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {
        Integer iou = -1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(IOU_CONTRACT_ID, () -> new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }
}