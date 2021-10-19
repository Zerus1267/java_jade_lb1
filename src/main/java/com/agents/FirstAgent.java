package com.agents;

import jade.core.Agent;

public class FirstAgent extends Agent {

	@Override
	protected void setup() {
		System.out.println("Hello myster. I'm your agent! " + getAID().getName() + " is ready!");
	}
}
