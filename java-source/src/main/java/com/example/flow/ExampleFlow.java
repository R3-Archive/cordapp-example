package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import kotlin.Triple;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;

import java.util.List;

public class ExampleFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<Void> {
        private final Party otherParty;

        public Initiator(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            FlowSession otherPartySession = initiateFlow(otherParty);
            List<FlowSession> sessions = ImmutableList.of(otherPartySession);
           // sessions.forEach( it -> sendAMessage(it, new Triple("a", "b", "c")));
            /**
             * please find the link for more info:
             * https://stackoverflow.com/questions/50638968/in-corda-unexpected-task-state-when-running-a-flow?rq=1
             */
            for(FlowSession session : sessions){
                sendAMessage(session, new Triple("a", "b", "c"));
            }
            return null;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<Void> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            otherPartyFlow.receive(Triple.class).unwrap(it -> { return it; });
            return null;
        }
    }

    @Suspendable
    private static Void sendAMessage(FlowSession session, Triple t) {
        session.send(t);
        return null;
    }
}
