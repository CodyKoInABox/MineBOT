package com.minebot.core;

/**
 * Material order used when choosing and crafting tools.
 * Gold harvests like wood and is never crafted; a wooden tool is preferred when both are held.
 */
public enum ToolTier {
	GOLD(12f, 0, 0),
	WOOD(2f, 0, 1),
	STONE(4f, 1, 2),
	IRON(6f, 2, 3),
	DIAMOND(8f, 3, 4),
	NETHERITE(9f, 4, 5);

	public final float speed;
	public final int harvestLevel;
	/** Higher wins when several tools can harvest the block. */
	public final int rank;

	ToolTier(float speed, int harvestLevel, int rank) {
		this.speed = speed;
		this.harvestLevel = harvestLevel;
		this.rank = rank;
	}

	public boolean canCraft() {
		return this != GOLD;
	}
}
