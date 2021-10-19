package com.agents;

import com.BookSellerGui;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;

public class BookSellerAgent extends Agent {

	private Hashtable<String, Integer> catalogue;

	private BookSellerGui myGui;

	@Override
	protected void setup() {
		catalogue = new Hashtable<>();
		myGui = new BookSellerGui(this);
		myGui.showGui();

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription  sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		addBehaviour(new OfferRequestsServer());

		addBehaviour(new PurchaseOrdersServer());
	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		myGui.dispose();
		System.out.println("Seller agent " + getAID().getName() + " was terminated");
	}

	public void updateCatalogue(String title, int price) {
		addBehaviour(new OneShotBehaviour() {
			@Override
			public void action() {
				catalogue.put(title, price);
			}
		});
	}

	class OfferRequestsServer extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage message = myAgent.receive(mt);
			if (message != null) {
				System.out.println("Seller recieved msg " + message.getReplyWith() + " price = " + catalogue.get(message.getContent()));
				String title = message.getContent();
				ACLMessage reply = message.createReply();

				int price = catalogue.getOrDefault(title, -1);
				if (price > 0) {
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price));
				} else {
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	}

	class PurchaseOrdersServer extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage message = myAgent.receive(mt);
			if (message != null) {
				String title = message.getContent();
				ACLMessage reply = message.createReply();

				int price = catalogue.remove(title);
				if (price > 0) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title + " sold to agent " + message.getSender().getName());
				} else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	}
}
