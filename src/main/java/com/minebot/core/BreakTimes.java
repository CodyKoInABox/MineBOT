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
		if (toolId == null) {
			return 1f;
		}
		return switch (Ids.normalize(toolId)) {
			case "minecraft:wooden_pickaxe" -> 2f;
			case "minecraft:stone_pickaxe" -> 4f;
			case "minecraft:iron_pickaxe" -> 6f;
			case "minecraft:diamond_pickaxe" -> 8f;
			case "minecraft:netherite_pickaxe" -> 9f;
			case "minecraft:golden_pickaxe" -> 12f;
			default -> 1f;
		};
	}
}
