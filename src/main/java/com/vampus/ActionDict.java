package com.vampus;

import aima.core.environment.wumpusworld.WumpusAction;

import java.util.Locale;

public enum ActionDict {

	FORWARD("Forward", WumpusAction.FORWARD),
	TURNLEFT("Turn left", WumpusAction.TURN_LEFT),
	TURNRIGHT("Turn right", WumpusAction.TURN_RIGHT),
	GRAB("Grab", WumpusAction.GRAB),
	CLIMB("Climb", WumpusAction.CLIMB),
	SHOOT("Shoot", WumpusAction.SHOOT);

	private final String name;
	private final WumpusAction action;

	ActionDict(String name, WumpusAction action) {
		this.name = name;
		this.action = action;
	}

	public static ActionDict getInstance(String operationName) {
		for(ActionDict dict : ActionDict.values()) {
			if (dict.getName().compareToIgnoreCase(operationName) == 0) {
				return dict;
			}
		}
		return null;
	}

	public static ActionDict getInstanceByContains(String operationName) {
		for(ActionDict dict : ActionDict.values()) {
			if (dict.getName().toLowerCase(Locale.ROOT).contains(operationName.toLowerCase(Locale.ROOT))) {
				return dict;
			}
		}
		return null;
	}


	public WumpusAction getAction() {
		return action;
	}

	public String getName() {
		return name;
	}
}
