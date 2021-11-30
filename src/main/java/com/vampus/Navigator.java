package com.vampus;

import aima.core.environment.wumpusworld.AgentPosition;
import aima.core.environment.wumpusworld.EfficientHybridWumpusAgent;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusPercept;
import com.Utils;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Navigator extends Agent {

	private EfficientHybridWumpusAgent wumpusAgent;
	private String speleologistMessage;
	private ACLMessage reply;

	final AgentPosition START_POSITION = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH);

	@Override
	protected void setup() {
		DFAgentDescription agentDescription = new DFAgentDescription();
		agentDescription.setName(getAID());
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType("navigator");
		serviceDescription.setName("navigator");
		agentDescription.addServices(serviceDescription);
		try {
			DFService.register(this, agentDescription);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		wumpusAgent = new EfficientHybridWumpusAgent(4, 4, START_POSITION);
		addBehaviour(new MessageWaitingServer());
	}

	@Override
	protected void takeDown() {
		System.out.println("Navigator-agent " + getAID().getName() + " terminating.");
	}

	private class MessageWaitingServer extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				speleologistMessage = msg.getContent();
				reply = msg.createReply();

				addBehaviour(new ActionServer());
			} else {
				block();
			}
		}
	}

	private class ActionServer extends OneShotBehaviour {

		@Override
		public void action() {
			Pattern pattern = Pattern.compile("stench|breeze|glitter|bump|scream", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(speleologistMessage);

			WumpusPercept percept = new WumpusPercept();
			while (matcher.find()) {
				String found = matcher.group().toLowerCase();
				try {
					System.out.println("Request curent state");
					PerceptionDict.getWumpusPerceptByActionName(found, percept);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Optional<WumpusAction> action = wumpusAgent.act(percept);
			if (action.isPresent()) {
				reply.setPerformative(ACLMessage.INFORM);
				try {
					System.out.println("act = " + action.get().getSymbol());
					reply.setContent(Utils.searchEnum(ActionDict.class, action.get().getSymbol()).getName());
				} catch (Exception e) {
					reply.setContent(ActionDict.CLIMB.getName());
					e.printStackTrace();
				}
			} else {
				reply.setPerformative(ACLMessage.FAILURE);
				reply.setContent("navigator couldn`t help agent");
			}
			myAgent.send(reply);

			if (action.isPresent() && action.get() == WumpusAction.CLIMB) {
				myAgent.doDelete();
			}
		}
	}
}

