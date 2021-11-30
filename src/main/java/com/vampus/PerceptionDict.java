package com.vampus;

import aima.core.environment.wumpusworld.WumpusPercept;
import com.Utils;

import java.util.function.Function;

public enum PerceptionDict {

	STENCH("stench", WumpusPercept::setStench, "I feel stench here", "There is a stench", "It is a strong stench here", "Hey! Is it stecnh here?"),
	BREEZE("breeze", WumpusPercept::setBreeze, "I feel breeze here", "There is a breeze", "It is a cool breeze here", "Brr, it is breeze."),
	GLITTER("glitter", WumpusPercept::setGlitter, "I see glitter here", "There is a glitter", "It is a glitter here", "Finally! I found glitter"),
	BUMP("bump", WumpusPercept::setBump, "I feel bump here", "There is a bump", "It is a bump here", "I think it is a bump"),
	SCREAM("scream", WumpusPercept::setScream, "I hear scream here", "There is a scream", "It is a loud scream here", "I heard scary scream");

	private final String[] possibleMessages;
	private final String keyWorld;
	private final Function<WumpusPercept, WumpusPercept> setterFunc;

	PerceptionDict(String keyWorld, Function<WumpusPercept, WumpusPercept> setter, String... possibleMessages) {
		this.possibleMessages = possibleMessages;
		this.keyWorld = keyWorld;
		this.setterFunc = setter;
	}

	public String[] getPossibleMessages() {
		return possibleMessages;
	}

	public static WumpusPercept getWumpusPerceptByActionName(String name, WumpusPercept percept) throws Exception {
		PerceptionDict d = Utils.searchEnum(PerceptionDict.class, name);
		return d.setterFunc.apply(percept);
	}
}
