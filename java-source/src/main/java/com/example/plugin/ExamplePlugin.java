package com.example.plugin;

import com.example.api.ExampleApi;
import com.example.flow.ExampleFlow;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.PluginServiceHub;
import net.corda.webserver.services.WebServerPluginRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ExamplePlugin implements WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(ExampleApi::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the exampleWeb directory in resources to /web/example
            "example", getClass().getClassLoader().getResource("exampleWeb").toExternalForm()
    );

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }
}