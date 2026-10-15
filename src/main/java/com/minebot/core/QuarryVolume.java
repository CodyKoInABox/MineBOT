package com.minebot.core;

import java.util.List;
import java.util.Map;

/** Inclusive dig volume. Stair columns and the bedrock layer are left in place. */
public final class QuarryVolume {
	private QuarryVolume() {}

	public static int countMineable(TaskConfig config, int bedrockY) {
		int surface = config.area.surfaceY();
		int floor = config.depth.floorInclusive(surface, bedrockY);
		Map<Long, Integer> tops = stairs(config, surface, floor);
		int count = 0;
		for (int y = surface; y >= floor; y--) {
			for (int z = config.area.minZ(); z <= config.area.maxZ(); z++) {
				for (int x = config.area.minX(); x <= config.area.maxX(); x++) {
					if (!SpiralStair.protectedColumn(tops, x, y, z, bedrockY)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	public static Map<Long, Integer> stairs(TaskConfig config, int surface, int floor) {
		if (!config.stair) {
			return Map.of();
		}
		List<StairTread> treads = SpiralStair.plan(config.area, surface, floor);
		return SpiralStair.columnTops(treads);
	}
}
