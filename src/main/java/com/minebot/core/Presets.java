package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

public final class Presets {
	public static final String EXAMPLE_QUARRY = "Example quarry";

	private Presets() {}

	/**
	 * 25×25 from {@code origin} toward +X/+Z, down to bedrock, clockwise stair,
	 * mine everything, deposit cobble/deepslate/ores, keep torches/food/wood/iron.
	 */
	public static TaskConfig exampleQuarry(BlockPos origin) {
		BlockPos start = origin == null ? new BlockPos(0, 64, 0) : origin.copy();
		TaskConfig config = new TaskConfig();
		config.name = EXAMPLE_QUARRY;
		config.area.setFirst(start);
		config.area.setSecond(new BlockPos(start.x + 24, start.y, start.z + 24));
		config.depth = DepthSpec.bedrock();
		config.stair = true;
		config.mine = MineFilter.everything();
		config.storage = StorageSpec.withDefaults();
		config.storage.depositIds = exampleDeposits();
		config.survival = SurvivalSpec.defaults();
		return config;
	}

	public static List<String> exampleDeposits() {
		List<String> ids = new ArrayList<>();
		ids.add("minecraft:cobblestone");
		ids.add("minecraft:cobbled_deepslate");
		ids.add("minecraft:deepslate");
		ids.addAll(Ids.ORE_BLOCKS);
		ids.addAll(Ids.ORE_DROPS);
		return ids;
	}
}
