package com.vampus;

import aima.core.agent.Agent;
import aima.core.agent.impl.AbstractEnvironment;
import aima.core.environment.wumpusworld.AgentPosition;
import aima.core.environment.wumpusworld.EfficientHybridWumpusAgent;
import aima.core.environment.wumpusworld.Room;
import aima.core.environment.wumpusworld.WumpusAction;
import aima.core.environment.wumpusworld.WumpusCave;
import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.AID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WumpusEnvironment extends AbstractEnvironment<WumpusPercept, WumpusAction> {

	private WumpusCave cave;
	private boolean isWumpusAlive = true;
	private boolean isGoldGrabbed;

	private Map<AID, Agent> agents = new HashMap<>();
	private Map<AID, AgentPosition> agentPositions = new HashMap<>();
	private Set<AID> bumpedAgents = new HashSet<>();
	private Set<AID> agentsHavingArrow = new HashSet<>();
	private AID agentJustKillingWumpus;

	private int time = 1;
	private int agentsExecutedActions = 0;

	public WumpusEnvironment(WumpusCave cave) {
		this.cave = cave;
	}

	public void addAgent(AID aid) {
		Agent<? super WumpusPercept, ? extends WumpusAction> agent =
				new EfficientHybridWumpusAgent(cave.getCaveXDimension(), cave.getCaveYDimension(), cave.getStart());
		agents.put(aid, agent);
		agentPositions.put(aid, cave.getStart());
		agentsHavingArrow.add(aid);
		super.addAgent(agent);
	}

	public void execute(AID aid, WumpusAction action) {
		Agent<?, ?> agent = agents.get(aid);
		bumpedAgents.remove(aid);
		if (aid == agentJustKillingWumpus)
			agentJustKillingWumpus = null;
		AgentPosition pos = agentPositions.get(aid);
		switch (action) {
			case FORWARD:
				AgentPosition newPos = cave.moveForward(pos);
				agentPositions.put(aid, newPos);
				if (newPos.equals(pos)) {
					bumpedAgents.add(aid);
				} else if (cave.isPit(newPos.getRoom()) || newPos.getRoom().equals(cave.getWumpus()) && isWumpusAlive)
					agent.setAlive(false);
				break;
			case TURN_LEFT:
				agentPositions.put(aid, cave.turnLeft(pos));
				break;
			case TURN_RIGHT:
				agentPositions.put(aid, cave.turnRight(pos));
				break;
			case GRAB:
				if (!isGoldGrabbed && pos.getRoom().equals(cave.getGold()))
					isGoldGrabbed = true;
				break;
			case SHOOT:
				if (agentsHavingArrow.contains(aid) && isWumpusForward(pos)) {
					isWumpusAlive = false;
					agentsHavingArrow.remove(aid);
					agentJustKillingWumpus = aid;
				} else if (agentsHavingArrow.contains(aid) && !isWumpusForward(pos)) {
					agentsHavingArrow.remove(aid);
				}
				break;
			case CLIMB:
				agent.setAlive(false);
		}

		agentsExecutedActions++;
		if (agentsExecutedActions == agentPositions.size()) {
			time++;
			agentsExecutedActions = 0;
		}
	}

	private boolean isWumpusForward(AgentPosition pos) {
		Room wumpus = cave.getWumpus();
		switch (pos.getOrientation()) {
			case FACING_NORTH:
				return pos.getX() == wumpus.getX() && pos.getY() < wumpus.getY();
			case FACING_SOUTH:
				return pos.getX() == wumpus.getX() && pos.getY() > wumpus.getY();
			case FACING_EAST:
				return pos.getY() == wumpus.getY() && pos.getX() < wumpus.getX();
			case FACING_WEST:
				return pos.getY() == wumpus.getY() && pos.getX() > wumpus.getX();
		}
		return false;
	}

	public WumpusPercept getPerceptSeenBy(AID aid) {
		WumpusPercept result = new WumpusPercept();
		AgentPosition pos = agentPositions.get(aid);
		List<Room> neighborCaves = Arrays.asList(
				new Room(pos.getX() - 1, pos.getY()), new Room(pos.getX() + 1, pos.getY()),
				new Room(pos.getX(), pos.getY() - 1), new Room(pos.getX(), pos.getY() + 1)
		);
		for (Room r : neighborCaves) {
			if (r.equals(cave.getWumpus()))
				result.setStench();
			if (cave.isPit(r))
				result.setBreeze();
		}
		if (pos.getRoom().equals(cave.getGold()))
			result.setGlitter();
		if (bumpedAgents.contains(aid))
			result.setBump();
		if (agentJustKillingWumpus != null)
			result.setScream();
		return result;
	}

	public WumpusCave getCave() {
		return cave;
	}

	public boolean isWumpusAlive() {
		return isWumpusAlive;
	}

	public boolean isGoalGrabbed() {
		return isGoldGrabbed;
	}

	public AgentPosition getAgentPosition(AID aid) {
		return agentPositions.get(aid);
	}

	public int getTime() {
		return time;
	}

	@Override
	public void execute(Agent<?, ?> agent, WumpusAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public WumpusPercept getPerceptSeenBy(Agent<?, ?> agent) {
		// TODO Auto-generated method stub
		return null;
	}
}
