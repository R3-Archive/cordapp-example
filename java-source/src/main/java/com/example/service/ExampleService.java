package com.example.service;

import com.example.flow.ExampleFlow;
import net.corda.core.node.PluginServiceHub;

/**
 * This service registers a flow factory that is used when a initiating party attempts to communicate with us
 * using a particular flow. Registration is done against a marker class (in this case, [ExampleFlow.Initiator])
 * which is sent in the session handshake by the other party. If this marker class has been registered then the
 * corresponding factory will be used to create the flow which will communicate with the other side. If there is no
 * mapping, then the session attempt is rejected.
 *
 * In short, this bit of code is required for the recipient in this Example scenario to respond to the sender using the
 * [ExampleFlow.Acceptor] flow.
 */
public class ExampleService {
    public ExampleService(PluginServiceHub services) {
        services.registerFlowInitiator(ExampleFlow.Initiator.class, ExampleFlow.Acceptor::new);
    }
}