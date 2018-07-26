package com.example.server.controllers;

import com.example.server.NodeRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

public class Controller {
    private NodeRPCConnection rpc;
    private CordaRPCOps proxy;
    private static Logger logger = LoggerFactory.getLogger(RestController.class);

    public Controller(NodeRPCConnection rpc) {
        this.rpc = rpc;
        this.proxy = rpc.proxy;
    }

    // TODO("Write IOU API")

//    @GetMapping(value = "/status", produces = "text/plain")
//    private String status() {
//        return "200";
//    }
}
