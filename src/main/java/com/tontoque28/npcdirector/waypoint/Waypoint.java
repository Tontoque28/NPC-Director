package com.tontoque28.npcdirector.waypoint;

import net.minecraft.util.math.BlockPos;

public class Waypoint {

	private final BlockPos position;
	private final int waitTicks;
	private final String onArriveQuestId;
	private final String onArriveState;
	private final String onDepartQuestId;
	private final String onDepartState;

	public Waypoint(BlockPos position, int waitTicks, String onArriveQuestId, String onArriveState,
			String onDepartQuestId, String onDepartState) {
		this.position = position;
		this.waitTicks = waitTicks;
		this.onArriveQuestId = onArriveQuestId;
		this.onArriveState = onArriveState;
		this.onDepartQuestId = onDepartQuestId;
		this.onDepartState = onDepartState;
	}

	public BlockPos getPosition() {
		return position;
	}

	public int getWaitTicks() {
		return waitTicks;
	}

	public String getOnArriveQuestId() {
		return onArriveQuestId;
	}

	public String getOnArriveState() {
		return onArriveState;
	}

	public String getOnDepartQuestId() {
		return onDepartQuestId;
	}

	public String getOnDepartState() {
		return onDepartState;
	}

	public boolean hasArriveTrigger() {
		return onArriveQuestId != null && onArriveState != null;
	}

	public boolean hasDepartTrigger() {
		return onDepartQuestId != null && onDepartState != null;
	}
}
