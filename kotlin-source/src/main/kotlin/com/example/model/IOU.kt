package com.example.model

import net.corda.core.serialization.CordaSerializable

/**
 * A simple class representing an IOU.
 *
 * This is the data structure that the parties will reach agreement over. These data structures can be arbitrarily
 * complex. See https://github.com/corda/corda/blob/master/samples/irs-demo/src/main/kotlin/net/corda/irs/contract/IRS.kt
 * for a more complicated example.
 *
 * @param value the IOU's value.
 */
@CordaSerializable
data class IOU(val value: Int)