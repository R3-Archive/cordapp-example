package com.example.service

import com.example.flow.ExampleFlow
import com.example.flow.GameFlow
import net.corda.core.node.PluginServiceHub

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
object ExampleService {
    class Service(services: PluginServiceHub) {
        init {
            services.registerFlowInitiator(ExampleFlow.Initiator::class.java) { ExampleFlow.Acceptor(it) }
            services.registerFlowInitiator(GameFlow.GameInitiator::class.java) { GameFlow.GameAcceptor(it) }
        }
    }
}