package com.example.api;

import com.example.flow.*;
import com.google.common.collect.*;
import kotlin.*;
import net.corda.core.contracts.*;
import net.corda.core.identity.*;
import net.corda.core.messaging.*;
import net.corda.core.node.*;
import net.corda.core.node.services.*;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.*;
import org.bouncycastle.asn1.x500.*;
import org.slf4j.*;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.*;
import static net.corda.client.rpc.UtilsKt.*;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private final CordaRPCOps services;
    private final X500Name myLegalName;
    private final String notaryName = "CN=Controller,O=R3,OU=corda,L=London,C=UK";

    static private final Logger logger = LoggerFactory.getLogger(ExampleApi.class);

    public ExampleApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, X500Name> whoami() { return ImmutableMap.of("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<X500Name>> getPeers() {
        Pair<List<NodeInfo>, Observable<NetworkMapCache.MapChange>> nodeInfo = services.networkMapUpdates();
        notUsed(nodeInfo.getSecond());
        return ImmutableMap.of(
                "peers",
                nodeInfo.getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !(name.toString().equals(notaryName)))
                        .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getIOUs() {
        Vault.Page<ContractState> vaultStates = services.vaultQueryByCriteria(new QueryCriteria.VaultQueryCriteria());
        return vaultStates.getStates();
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the sender and the recipient will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-iou")
    public Response createIOU(@QueryParam("iouValue") int iouValue, @QueryParam("partyName") X500Name partyName) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromX500Name(partyName);

        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Response.Status status;
        String msg;
        try {
            FlowProgressHandle<SignedTransaction> flowHandle = services
                    .startTrackedFlowDynamic(ExampleFlow.Initiator.class, iouValue, otherParty);
            flowHandle.getProgress().subscribe(evt -> System.out.printf(">> %s\n", evt));

            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            status = Response.Status.CREATED;
            msg = String.format("Transaction id %s committed to ledger.", result.getId());

        } catch (Throwable ex) {
            status = Response.Status.BAD_REQUEST;
            msg = ex.getMessage();
            logger.error(msg, ex);
        }

        return Response
                .status(status)
                .entity(msg)
                .build();
    }
}