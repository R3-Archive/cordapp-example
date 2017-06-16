package com.example.contract;

import com.example.model.IOU;
import com.example.state.IOUState;
import net.corda.core.identity.Party;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.PublicKey;

import static net.corda.testing.CoreTestUtils.*;

public class IOUContractTests {
    static private final Party miniCorp = getMINI_CORP();
    static private final Party megaCorp = getMEGA_CORP();
    static private final PublicKey[] keys = new PublicKey[2];

    {
        keys[0] = getMEGA_CORP_PUBKEY();
        keys[1] = getMINI_CORP_PUBKEY();
    }

    @Test
    public void transactionMustIncludeCreateCommand() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
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
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.input(new IOUState(iou, miniCorp, megaCorp));
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneOutput() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void senderMustSignTransaction() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
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
    public void recipientMustSignTransaction() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
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
    public void senderIsNotRecipient() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, megaCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The sender and the recipient cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {
        IOU iou = new IOU(-1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, miniCorp, megaCorp));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }
}