package com.example

import com.google.common.util.concurrent.Futures
import net.corda.core.getOrThrow
import net.corda.core.node.services.ServiceInfo
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.testing.driver.driver
import org.bouncycastle.asn1.x500.X500Name

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Firstly, run the "Run Example CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports which should be output to the console for each node. They typically start at 5006, 5007,
 *    5008. The "Debug CorDapp" configuration runs with port 5007, which should be "NodeB". In any case, double check
 *    the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf())
    driver(isDebug = true) {
        startNode(X500Name("CN=Controller,O=R3,OU=corda,L=London,C=UK"), setOf(ServiceInfo(ValidatingNotaryService.type)))
        val (nodeA, nodeB, nodeC) = Futures.allAsList(
                startNode(X500Name("CN=NodeA,O=NodeA,L=London,C=UK"), rpcUsers = listOf(user)),
                startNode(X500Name("CN=NodeB,O=NodeB,L=New York,C=US"), rpcUsers = listOf(user)),
                startNode(X500Name("CN=NodeC,O=NodeC,L=Paris,C=FR"), rpcUsers = listOf(user))).getOrThrow()

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)

        waitForAllNodesToFinish()
    }
}