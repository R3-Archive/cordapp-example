package com.example;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.node.services.ServiceInfo;
import net.corda.node.driver.NodeHandle;
import net.corda.node.services.User;
import net.corda.node.services.transactions.ValidatingNotaryService;

import static java.util.Collections.*;
import static net.corda.node.driver.Driver.driver;

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
public class Main {
    public static void main(String[] args) {
        // No permissions required as we are not invoking flows.
        final User user = new User("user1", "test", emptySet());
        driver(
                true,
                dsl -> {
                    dsl.startNode("Controller",
                            ImmutableSet.of(new ServiceInfo(ValidatingNotaryService.Companion.getType(), null)),
                            emptyList(),
                            emptyMap());

                    try {
                        NodeHandle nodeA = dsl.startNode("NodeA", emptySet(), ImmutableList.of(user), emptyMap()).get();
                        NodeHandle nodeB = dsl.startNode("NodeB", emptySet(), ImmutableList.of(user), emptyMap()).get();
                        NodeHandle nodeC = dsl.startNode("NodeC", emptySet(), ImmutableList.of(user), emptyMap()).get();

                        dsl.startWebserver(nodeA);
                        dsl.startWebserver(nodeB);
                        dsl.startWebserver(nodeC);

                        dsl.waitForAllNodesToFinish();
                    } catch (Throwable e) {
                        System.err.println("Encountered exception in node startup: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
        );
    }
}