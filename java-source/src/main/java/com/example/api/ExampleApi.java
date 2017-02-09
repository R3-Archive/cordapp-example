package com.example.api;

import com.example.contract.IOUContract;
import com.example.flow.ExampleFlow;
import com.example.model.IOU;
import com.example.state.IOUState;
import com.google.common.collect.Lists;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private final CordaRPCOps services;
    private final String myLegalName;
    private final List<String> notaryNames = Lists.newArrayList("Controller", "NetworkMapService");

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
    public Map<String, String> whoami() { return singletonMap("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        return singletonMap(
                "peers",
                services.networkMapUpdates().getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !(notaryNames.contains(name)))
                        .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getIOUs() { return services.vaultAndUpdates().getFirst(); }

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
    @Path("{party}/create-iou")
    public Response createIOU(IOU iou, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
        final Party otherParty = services.partyFromName(partyName);

        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final IOUState state = new IOUState(
                iou,
                services.nodeIdentity().getLegalIdentity(),
                otherParty,
                new IOUContract());

        Response.Status status;
        String msg;
        try {
            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = services
                    .startFlowDynamic(ExampleFlow.Initiator.class, state, otherParty)
                    .getReturnValue()
                    .get();

            status = Response.Status.CREATED;
            msg = String.format("Transaction id %s sent to counterparty.", result.getId());

        } catch (Throwable ex) {
            status = Response.Status.BAD_REQUEST;
            msg = "Transaction failed.";
            logger.error(ex.getMessage(), ex);
        }

        return Response
                .status(status)
                .entity(msg)
                .build();
    }
}