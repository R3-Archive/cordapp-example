package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * An IOUState schema.
 */
public class IOUSchemaV1 extends MappedSchema {
    public IOUSchemaV1() {
        super(IOUSchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "iou_states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "sender_name") private final String senderName;
        @Column(name = "recipient_name") private final String recipientName;
        @Column(name = "value") private final int value;


        public PersistentIOU(String senderName, String recipientName, int value) {
            this.senderName = senderName;
            this.recipientName = recipientName;
            this.value = value;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getRecipientName() {
            return recipientName;
        }

        public int getValue() {
            return value;
        }
    }
}