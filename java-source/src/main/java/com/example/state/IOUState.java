package com.example.state;

import com.example.contract.IOUContract;
import com.example.model.IOU;
import com.example.schema.IOUSchemaV1;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// TODO: Implement QueryableState and add ORM code (to match Kotlin example).

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
public class IOUState implements LinearState, QueryableState {
    private final IOU iou;
    private final Party sender;
    private final Party recipient;
    private final UniqueIdentifier linearId;
    private final IOUContract contract = new IOUContract();

    /**
     * @param iou details of the IOU.
     * @param sender the party issuing the IOU.
     * @param recipient the party receiving and approving the IOU.
     */
    public IOUState(IOU iou,
                    Party sender,
                    Party recipient)
    {
        this.iou = iou;
        this.sender = sender;
        this.recipient = recipient;
        this.linearId = new UniqueIdentifier();
    }

    public IOU getIOU() { return iou; }
    public Party getSender() { return sender; }
    public Party getRecipient() { return recipient; }
    @Override public IOUContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, recipient);
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final boolean iAmSender = ourKeys.contains(sender.getOwningKey());
        final boolean iAmRecipient = ourKeys.contains(recipient.getOwningKey());
        return iAmSender || iAmRecipient;
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof IOUSchemaV1) {
            return new IOUSchemaV1.PersistentIOU(
                    this.sender.getName().toString(),
                    this.recipient.getName().toString(),
                    this.iou.getValue());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new IOUSchemaV1());
    }
}