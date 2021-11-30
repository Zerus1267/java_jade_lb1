package com.vampus;

import aima.core.environment.wumpusworld.AgentPosition;
import aima.core.environment.wumpusworld.EfficientHybridWumpusAgent;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Speleologist extends Agent {
	protected EfficientHybridWumpusAgent wumpusAgent;
	protected AID environmentAid;
	protected AID navigatorAid;
	protected WumpusPercept percept;
	protected WumpusAction offeredAction;

	final AgentPosition START_POS = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH);

	protected void setup() {
		environmentAid = getAgent("environment");
		navigatorAid = getAgent("navigator");

		wumpusAgent = new EfficientHybridWumpusAgent(4, 4, START_POS);
		addBehaviour(new SpeleologistBehaviour());
	}

	private AID getAgent(String agentType) {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(agentType);
		template.addServices(sd);
		AID agentAid = null;
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			if (result.length == 0) {
				System.out.println("There are no " + agentType + " found!");
			} else {
				System.out.println("Found the following " + agentType + " agent:");
				agentAid = result[0].getName();
				System.out.println(agentAid.getName());
				return agentAid;
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		return agentAid;
	}

	protected void takeDown() {
		System.out.println("Speleologist-agent " + getAID().getName() + " terminating.");
	}

	class SpeleologistBehaviour extends Behaviour {
		private int step = 0;
		private MessageTemplate mt;

		@Override
		public void action() {
			switch (step) {
				case 0:
					ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
					request.addReceiver(environmentAid);
					request.setConversationId("environment-current-state");
					request.setReplyWith("environment-request" + System.currentTimeMillis());
					myAgent.send(request);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("environment-current-state"),
							MessageTemplate.MatchInReplyTo(request.getReplyWith()));
					step = 1;

					break;
				case 1:
					ACLMessage envRequestReply = myAgent.receive(mt);
					if (envRequestReply != null) {
						if (envRequestReply.getPerformative() == ACLMessage.INFORM) {

							String[] params = envRequestReply.getContent().split(" ");
//							System.out.println(params);
							percept = new WumpusPercept();
							if (Objects.equals(params[0], "true"))
								percept.setStench();
							if (Objects.equals(params[1], "true"))
								percept.setBreeze();
							if (Objects.equals(params[2], "true"))
								percept.setGlitter();
							if (Objects.equals(params[3], "true"))
								percept.setBump();
							if (Objects.equals(params[4], "true"))
								percept.setScream();
							step = 2;
							System.out.println("Response from environment: " + String.join(" ", params));
						}
					} else {
						block();
					}
					break;
				case 2:
					String messageForNavigator = "";
					if (percept.isStench())
						messageForNavigator += PerceptionDict.STENCH.getPossibleMessages()[(int) (Math.random() * 4)] + ". ";
					if (percept.isBreeze())
						messageForNavigator += PerceptionDict.BREEZE.getPossibleMessages()[(int) (Math.random() * 4)] + ". ";
					if (percept.isGlitter())
						messageForNavigator += PerceptionDict.GLITTER.getPossibleMessages()[(int) (Math.random() * 4)] + ". ";
					if (percept.isBump())
						messageForNavigator += PerceptionDict.BUMP.getPossibleMessages()[(int) (Math.random() * 4)] + ". ";
					if (percept.isScream())
						messageForNavigator += PerceptionDict.SCREAM.getPossibleMessages()[(int) (Math.random() * 4)] + ". ";
					if (messageForNavigator.length() > 1) {
						messageForNavigator = messageForNavigator.substring(0, messageForNavigator.length() - 2);
					}

					ACLMessage navMessage = new ACLMessage(ACLMessage.REQUEST);
					navMessage.addReceiver(navigatorAid);
					navMessage.setContent(messageForNavigator);
					navMessage.setConversationId("navigator-decision");
					navMessage.setReplyWith("navigator-request" + System.currentTimeMillis());
					myAgent.send(navMessage);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("navigator-decision"),
							MessageTemplate.MatchInReplyTo(navMessage.getReplyWith()));
					step = 3;
					break;
				case 3:
					ACLMessage navigatorReply = myAgent.receive(mt);
					if (navigatorReply != null) {
						if (navigatorReply.getPerformative() == ACLMessage.INFORM) {
							Pattern pattern = Pattern.compile("forward|turn(.*)left|turn(.*)right|grab|shoot|climb", Pattern.CASE_INSENSITIVE);
							Matcher matcher = pattern.matcher(navigatorReply.getContent());

							while (matcher.find()) {
								String found = matcher.group().toLowerCase();
								offeredAction = ActionDict.getInstance(found).getAction();
							}
							step = 4;
							System.out.println("Response from navigator: " + offeredAction);
						}
					} else {
						block();
					}
					break;
				case 4:
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					cfp.addReceiver(environmentAid);
					cfp.setContent(String.valueOf(offeredAction));
					cfp.setConversationId("environment-change-state");
					cfp.setReplyWith("environment-cfp" + System.currentTimeMillis());
					myAgent.send(cfp);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("environment-change-state"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 5;
					break;
				case 5:
					ACLMessage envCfpReply = myAgent.receive(mt);
					if (envCfpReply != null) {
						step = 0;
						if (envCfpReply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
							if (Objects.equals(envCfpReply.getContent(), "OK")) {
								if (offeredAction == WumpusAction.CLIMB) {
									step = 6;
								}
							}
						}
					} else {
						block();
					}
					break;
				default:
					break;
			}
		}

		@Override
		public boolean done() {
			return (step == 6);
		}

	}
}
