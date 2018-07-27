package com.example.server;

import com.example.flow.IOUFlow;
import com.example.schema.IOUSchemaV1;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@RestController
@RequestMapping("/")
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;
    private final String myOrganisation;
    private final List<String> serviceNames = ImmutableList.of("Notary");
    private final static Logger logger = LoggerFactory.getLogger(RestController.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = proxy.nodeInfo().getLegalIdentities().get(0).getName();
        this.myOrganisation = myLegalName.getOrganisation();
    }

    /**
     * Returns the node's name.
     */
    @GetMapping(value = "/me", produces = "application/json")
    private Map<String, String> status() {
        return ImmutableMap.of("me", myOrganisation);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = "/peers", produces = "application/json")
    public Map<String, List<String>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = proxy.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName().getOrganisation())
                .filter(name -> !name.equals(myOrganisation) && !serviceNames.contains(name))
                .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GetMapping(value = "/ious", produces = "application/json")
    public List<Map<String, String>> getIOUs() {
        List<StateAndRef<IOUState>> ious = proxy.vaultQuery(IOUState.class).getStates();
        return ious.stream().map(iou -> ImmutableMap.of(
                "value", Integer.toString(iou.getState().getData().getValue()),
                "lender", iou.getState().getData().getLender().getName().getOrganisation(),
                "borrower", iou.getState().getData().getBorrower().getName().getOrganisation())
        ).collect(Collectors.toList());
    }

    /**
     * Displays the IOU states in the vault created by this node.
     */
    @GetMapping(value = "/my-ious", produces = "application/json")
    public List<Map<String, String>> getMyIOUs() throws NoSuchFieldException {
        QueryCriteria allStatesCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);

        Field lender = IOUSchemaV1.PersistentIOU.class.getDeclaredField("lenderName");
        CriteriaExpression lenderIndex = Builder.equal(lender, myLegalName.toString());
        QueryCriteria lenderCriteria = new QueryCriteria.VaultCustomQueryCriteria(lenderIndex);

        QueryCriteria combinedCriteria = allStatesCriteria.and(lenderCriteria);

        List<StateAndRef<IOUState>> ious = proxy.vaultQueryByCriteria(combinedCriteria, IOUState.class).getStates();
        return ious.stream().map(iou -> ImmutableMap.of(
                "value", Integer.toString(iou.getState().getData().getValue()),
                "lender", iou.getState().getData().getLender().getName().getOrganisation(),
                "borrower", iou.getState().getData().getBorrower().getName().getOrganisation())
        ).collect(Collectors.toList());
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PutMapping(value = "/create-iou")
    public Response createIOU(@QueryParam("iouValue") int iouValue, @QueryParam("partyName") String partyName) {
        if (iouValue <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build();
        }
        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        final Set<Party> otherParties = proxy.partiesFromName(partyName, false);
        if (otherParties.size() != 1) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }
        final Party otherParty = otherParties.iterator().next();

        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(IOUFlow.Initiator.class, iouValue, otherParty)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
}
