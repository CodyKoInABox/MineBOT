package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

/** Cursor so a job can resume after death or a client restart. */
public final class JobProgress {
	public boolean running;
	public boolean initialized;
	public int x;
	public int y;
	public int z;
	public boolean breaking;
	public boolean breakStartPending = true;
	public int breakTicksLeft;
	public int breakX;
	public int breakY;
	public int breakZ;
	public String breakItem = "";
	public int eatTicksLeft;
	public int sleepCooldown;
	public boolean placeTorchNext;
	public int torchX;
	public int torchY;
	public int torchZ;
	public String status = Status.STOPPED;
	public List<CraftClick> craftClicks = new ArrayList<>();
	public int craftIndex;

	public JobProgress() {}

	public JobProgress copy() {
		JobProgress copy = new JobProgress();
		copy.running = running;
		copy.initialized = initialized;
		copy.x = x;
		copy.y = y;
		copy.z = z;
		copy.breaking = breaking;
		copy.breakStartPending = breakStartPending;
		copy.breakTicksLeft = breakTicksLeft;
		copy.breakX = breakX;
		copy.breakY = breakY;
		copy.breakZ = breakZ;
		copy.breakItem = breakItem;
		copy.eatTicksLeft = eatTicksLeft;
		copy.sleepCooldown = sleepCooldown;
		copy.placeTorchNext = placeTorchNext;
		copy.torchX = torchX;
		copy.torchY = torchY;
		copy.torchZ = torchZ;
		copy.status = status;
		copy.craftClicks = new ArrayList<>();
		for (CraftClick click : craftClicks) {
			copy.craftClicks.add(click.copy());
		}
		copy.craftIndex = craftIndex;
		return copy;
	}
}
