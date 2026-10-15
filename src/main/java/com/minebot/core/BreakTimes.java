package com.minebot.core;

/**
 * Vanilla destroy time in ticks.
 * {@code perTick = toolSpeed / hardness / (canHarvest ? 30 : 100)}.
 * Negative hardness (bedrock) is unbreakable.
 */
public final class BreakTimes {
	private BreakTimes() {}

	public static int ticks(float hardness, float toolSpeed, boolean canHarvest) {
		if (hardness < 0f) {
			return -1;
		}
		if (hardness == 0f) {
			return 1;
		}
		float speed = toolSpeed <= 0f ? 1f : toolSpeed;
		float perTick = speed / hardness / (canHarvest ? 30f : 100f);
		if (perTick <= 0f) {
			return -1;
		}
		return (int) Math.ceil(1.0 / perTick);
	}

	public static float speedFor(String toolId) {
		ToolTier tier = ToolItems.tierOf(toolId);
		return tier == null ? 1f : tier.speed;
	}
}
