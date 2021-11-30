package com.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BookBuyerAgent extends Agent {

	public String targetBookTitle;

	private AID[] sellerAgenst = {new AID("seller1", AID.ISLOCALNAME), new AID("seller2", AID.ISLOCALNAME)};

	@Override
	protected void setup() {
		System.out.println("Hello, i'm a BookBuyerAgent");
		if (getArguments().length == 0) {
			System.out.println("Client has nothing to prefer!");
			doDelete();
		} else {
			List<String> args = Arrays.stream(getArguments()).map(Object::toString).collect(Collectors.toList());
			targetBookTitle = args.get(0);
			addBehaviour(new TickerBehaviour(this, 20000) {
				@Override
				protected void onTick() {
					System.out.println("Trying to buy " + targetBookTitle);
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription description = new ServiceDescription();
					description.setType("book-selling");
					template.addServices(description);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						sellerAgenst = new AID[result.length];
						for (int i = 0; i < result.length; i++) {
							sellerAgenst[i] = result[i].getName();
							System.out.println("adding seller = " + sellerAgenst[i]);
						}
					} catch (FIPAException e) {
						e.printStackTrace();
					}
					myAgent.addBehaviour(new RequestPerformer());
				}
			});
		}
	}

	@Override
	protected void takeDown() {
		System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
	}

	class RequestPerformer extends Behaviour {

		private AID bestSeller;
		private int bestPrice;
		private int repliesCount = 0;
		private MessageTemplate mt;
		private int step = 0;

		@Override
		public void action() {
			switch (step) {
				case 0:
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for (AID aid : sellerAgenst) {
						cfp.addReceiver(aid);
					}
					cfp.setContent(targetBookTitle);
					cfp.setConversationId("book-trade");
					cfp.setReplyWith("cfp" + System.currentTimeMillis());
					System.out.println("generating setReplyWith" + cfp.getReplyWith());
					myAgent.send(cfp);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
					break;
				case 1:
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) {
						System.out.println("case 1 with reply = " + reply.getReplyWith());
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							int price = Integer.parseInt(reply.getContent());
							if (bestSeller == null || price < bestPrice) {
								bestSeller = reply.getSender();
								bestPrice = price;
							}
						}
					}
					repliesCount++;
					if (repliesCount >= sellerAgenst.length) {
						step = 2;
					} else {
						block();
						break;
					}
				case 2:
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(bestSeller);
					order.setContent(targetBookTitle);
					order.setConversationId("book-trade");
					order.setReplyWith("order" + System.currentTimeMillis());
					myAgent.send(order);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
					block();
					break;
				case 3:
					ACLMessage purchaseReply = myAgent.receive(mt);
					if (purchaseReply != null) {
						if (purchaseReply.getPerformative() == ACLMessage.INFORM) {
							System.out.println(targetBookTitle + " successfully purchased from agent " + purchaseReply.getSender().getName());
							System.out.println("Price = " + bestPrice);
							myAgent.doDelete();
						} else {
							System.out.println("Attempt failed: requested book already sold.");
						}
						step = 4;
					} else {
						block();
					}
					break;
			}
		}

		@Override
		public boolean done() {
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}
}
