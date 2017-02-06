package com.example.plugin

import com.esotericsoftware.kryo.Kryo
import com.example.api.ExampleApi
import com.example.contract.IOUContract
import com.example.state.IOUState
import com.example.flow.ExampleFlow
import com.example.model.IOU
import com.example.service.ExampleService
import net.corda.core.contracts.AuthenticatedObject
import net.corda.core.contracts.Timestamp
import net.corda.core.contracts.TransactionType
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import net.corda.core.transactions.SignedTransaction
import java.util.concurrent.ExecutionException
import java.util.function.Function

class ExamplePlugin : CordaPluginRegistry() {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::ExampleApi))

    /**
     * A list of flows required for this CorDapp. Any flow which is invoked from from the web API needs to be
     * registered as an entry into this map. The map takes the form:
     *
     * Name of the flow to be invoked -> Set of the parameter types passed into the flow.
     *
     * E.g. In the case of this CorDapp:
     *
     * "ExampleFlow.Initiator" -> Set(IOUState, Party)
     *
     * This map also acts as a white list. If a flow is invoked via the API and not registered correctly
     * here, then the flow state machine will _not_ invoke the flow. Instead, an exception will be raised.
     */
    override val requiredFlows: Map<String, Set<String>> = mapOf(
            ExampleFlow.Initiator::class.java.name to setOf(IOUState::class.java.name, Party::class.java.name)
    )

    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow. See the [ExampleService.Service] class for an implementation which sets up a
     */
    override val servicePlugins: List<Function<PluginServiceHub, out Any>> = listOf(Function(ExampleService::Service))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "example" to javaClass.classLoader.getResource("exampleWeb").toExternalForm()
    )

    /**
     * Register required types with Kryo (our serialisation framework).
     */
    override fun registerRPCKryoTypes(kryo: Kryo): Boolean {
        kryo.register(IOUState::class.java)
        kryo.register(IOUContract::class.java)
        kryo.register(IOU::class.java)
        kryo.register(TransactionVerificationException.ContractRejection::class.java)
        kryo.register(LedgerTransaction::class.java)
        kryo.register(AuthenticatedObject::class.java)
        kryo.register(IOUContract.Commands.Create::class.java)
        kryo.register(Timestamp::class.java)
        kryo.register(TransactionType.General::class.java)
        return true
    }
}