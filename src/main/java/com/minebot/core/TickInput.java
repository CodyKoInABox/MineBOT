package com.minebot.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Everything the state machine may observe on a single tick. */
public final class TickInput {
	public int playerX;
	public int playerY;
	public int playerZ;
	public int hunger = 20;
	public boolean hasFood;
	public long dayTime = 1000L;
	public boolean bedNearby;
	public BlockPos bedPos;
	public Map<String, Integer> inventory = new LinkedHashMap<>();
	public boolean chestCanAcceptDeposit = true;
	public BlockPos chestPos;
	public boolean chestOpen;
	public String toolId = "";
	public int bedrockY = -64;
	public WorldQuery world = WorldQuery.AIR;
	public boolean inventoryScreenOpen;
	public boolean craftingScreenOpen;
	public boolean smithingScreenOpen;
	public BlockPos craftingTablePos;
	public BlockPos smithingTablePos;

	public TickInput() {}

	public boolean hasFood() {
		return hasFood || ChestPolicy.foodIn(inventory) != null;
	}

	public String tool() {
		if (toolId != null && Ids.isPickaxe(toolId)) {
			return Ids.normalize(toolId);
		}
		String held = ChestPolicy.heldPick(inventory);
		return held == null ? "" : held;
	}
}
