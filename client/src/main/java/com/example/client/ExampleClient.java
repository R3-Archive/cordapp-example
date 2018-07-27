package com.example.client;

import com.example.state.IOUState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * stream some state data back from the node.
 */
public class ExampleClient {
    private static final Logger logger = LoggerFactory.getLogger(ExampleClient.class);

    private static void logState(StateAndRef<IOUState> state) {
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleClient <node address>");
        }

        // Create an RPC connection to the node.
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        // Grab all existing and future IOU states in the vault.
        final DataFeed<Vault.Page<IOUState>, Vault.Update<IOUState>> dataFeed = proxy.vaultTrack(IOUState.class);
        final Vault.Page<IOUState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<IOUState>> updates = dataFeed.getUpdates();

        // Log the 'placed' IOUs and listen for new ones.
        snapshot.getStates().forEach(ExampleClient::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(ExampleClient::logState));
    }
}