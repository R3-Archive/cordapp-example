package com.example.server;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.example.server.Constants.*;

/**
 * Wraps a node RPC proxy.
 *
 * The RPC proxy is configured using command-line arguments.
 */
@Component
public class NodeRPCConnection implements AutoCloseable {
    @Value("${" + CORDA_NODE_HOST + "}")
    private String host;
    @Value("${" + CORDA_USER_NAME + "}")
    private String username;
    @Value("${" + CORDA_USER_PASSWORD + "}")
    private String password;
    @Value("${" + CORDA_RPC_PORT + "}")
    private int rpcPort;

    private CordaRPCConnection rpcConnection;
    CordaRPCOps proxy;

    @PostConstruct
    public void initialiseNodeRPCConnection() {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        CordaRPCConnection rpcConnection = rpcClient.start(username, password);
        proxy = rpcConnection.getProxy();
    }

    @PreDestroy
    public void close() {
        rpcConnection.notifyServerAndClose();
    }
}