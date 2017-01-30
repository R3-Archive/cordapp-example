package com.example.contract;

import com.example.model.IOU;
import com.example.state.IOUState;
import net.corda.core.crypto.CompositeKey;
import org.junit.Test;

import static net.corda.testing.CoreTestUtils.*;

public class IOUTests {
    @Test
    public void transactionMustIncludeCreateCommand() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, getMINI_CORP(), getMEGA_CORP(), new IOUContract()));
                txDSL.fails();
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void buyerMustSignTransaction() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, getMINI_CORP(), getMEGA_CORP(), new IOUContract()));
                CompositeKey[] keys = new CompositeKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void sellerMustSignTransaction() {
        IOU iou = new IOU(1);
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(iou, getMINI_CORP(), getMEGA_CORP(), new IOUContract()));
                CompositeKey[] keys = new CompositeKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
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
                txDSL.output(new IOUState(iou, getMINI_CORP(), getMEGA_CORP(), new IOUContract()));
                CompositeKey[] keys = new CompositeKey[2];
                keys[0] = getMEGA_CORP_PUBKEY();
                keys[1] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }
}