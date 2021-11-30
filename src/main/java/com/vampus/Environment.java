package com.vampus;

import aima.core.environment.wumpusworld.AgentPosition;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusCave;
import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Environment extends Agent {

	private WumpusEnvironment wumpusEnvironment;
	private HashMap<AID, Boolean> registeredAgents = new HashMap<>();

	@Override
	protected void setup() {
		String configString = ""
				+ ". . . P "
				+ "W G P . "
				+ ". . . . "
				+ "S . P . "; // 4 x 4
		WumpusCave cave = new WumpusCave(4, 4, configString);
		wumpusEnvironment = new WumpusEnvironment(cave);

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("environment");
		sd.setName("environment");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new MassageHandlerServer());
	}

	@Override
	protected void takeDown() {
		System.out.println("Environment-agent " + getAID().getName() + " terminating.");
	}

	private class MassageHandlerServer extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mtRequest = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mtCfp = MessageTemplate.MatchPerformative(ACLMessage.CFP);

			ACLMessage requestMsg = myAgent.receive(mtRequest);
			ACLMessage cfpMsg = myAgent.receive(mtCfp);

			if (requestMsg != null) {
				myAgent.addBehaviour(new RequestCurrentStateServer(requestMsg));
			}
			if (cfpMsg != null) {
				myAgent.addBehaviour(new ExecuteActionServer(cfpMsg));
			}
			if (requestMsg == null && cfpMsg == null) {
				block();
			}
		}
	}

	private class RequestCurrentStateServer extends OneShotBehaviour {

		private ACLMessage msg;

		public RequestCurrentStateServer(ACLMessage message) {
			super();
			this.msg = message;
		}

		@Override
		public void action() {
			if (msg != null) {
				AID agentAid = msg.getSender();
				if (!registeredAgents.containsKey(agentAid)) {
					wumpusEnvironment.addAgent(agentAid);
					registeredAgents.put(agentAid, true);
				}

				WumpusPercept percept = wumpusEnvironment.getPerceptSeenBy(agentAid);
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				String answer = Stream.of(
						percept.isStench(), percept.isBreeze(), percept.isGlitter(), percept.isBump(), percept.isScream())
						.map(bool -> Boolean.toString(bool)).collect(Collectors.joining(" "));
				System.out.println(answer);
				reply.setContent(answer);
				myAgent.send(reply);
			}
		}
	}

	private class ExecuteActionServer extends OneShotBehaviour {

		private ACLMessage msg;

		public ExecuteActionServer(ACLMessage message) {
			super();
			this.msg = message;
		}

		@Override
		public void action() {
			if (msg != null) {
				WumpusAction action = WumpusAction.valueOf(msg.getContent());
				ACLMessage reply = msg.createReply();

				if (action != null) {
					wumpusEnvironment.execute(msg.getSender(), action);

					AgentPosition newPos = wumpusEnvironment.getAgentPosition(msg.getSender());
					System.out.println(newPos);

					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					reply.setContent("OK");
				} else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("invalid action");
				}
				myAgent.send(reply);

				if (action == WumpusAction.CLIMB) {
					myAgent.doDelete();
				}
			}
		}
	}
}
