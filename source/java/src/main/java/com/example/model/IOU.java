package com.example.model;

/**
 * A simple class representing an IOU.
 *
 * This is the data structure that the parties will reach agreement over. These data structures can be arbitrarily
 * complex. See https://github.com/corda/corda/blob/master/samples/irs-demo/src/main/kotlin/net/corda/irs/contract/IRS.kt
 * for a more complicated example.
 *
 * @param value the IOU's value.
 */
public class IOU {
    private int value;

    public int getValue() { return value; }

    public IOU(int value) {
        this.value = value;
    }

    // Dummy constructor used by the create-iou API endpoint.
    public IOU() {}

    @Override public String toString() {
        return String.format("IOU(value=%d)", value);
    }
}