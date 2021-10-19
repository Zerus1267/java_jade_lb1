package com;

import jade.core.behaviours.Behaviour;

public class OverBearingBehaviour extends Behaviour {
	@Override
	public void action() {
		while (true) {
			System.out.println("Executing com.OverBearingBehaviour!");
		}
	}

	@Override
	public boolean done() {
		return true;
	}
}
