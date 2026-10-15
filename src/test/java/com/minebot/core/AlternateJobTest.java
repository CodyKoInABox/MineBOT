package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlternateJobTest {
	@Test
	void differentSizeAndDepthModes() {
		TaskConfig config = new TaskConfig();
		config.area.setFirst(new BlockPos(0, 40, 0));
		config.area.setSecond(new BlockPos(9, 40, 5));
		config.depth = DepthSpec.layers(4);
		config.stair = false;
		config.mine = MineFilter.only(List.of("iron_ore"), false);

		assertEquals(10, config.area.sizeX());
		assertEquals(6, config.area.sizeZ());
		assertEquals(DepthSpec.Kind.LAYERS, config.depth.kind);
		assertEquals(4, config.depth.layers);
		assertEquals(37, config.depth.floorInclusive(40, -64));
		assertEquals(10 * 6 * 4, QuarryVolume.countMineable(config, -64));

		config.depth = DepthSpec.untilY(30);
		assertEquals(DepthSpec.Kind.UNTIL_Y, config.depth.kind);
		assertEquals(30, config.depth.floorInclusive(40, -64));
		assertEquals(10 * 6 * 11, QuarryVolume.countMineable(config, -64));

		config.depth = DepthSpec.untilY(-80);
		assertEquals(-63, config.depth.floorInclusive(40, -64));

		config.depth = DepthSpec.bedrock();
		assertEquals(-63, config.depth.floorInclusive(40, -64));

		assertFalse(config.mine.everything);
		assertFalse(config.mine.digObstructions);
		assertTrue(config.mine.shouldMine("minecraft:iron_ore", false));
		assertFalse(config.mine.shouldMine("minecraft:stone", false));
		assertFalse(config.mine.shouldMine("minecraft:stone", true));
		assertFalse(config.mine.shouldMine("minecraft:bedrock", true));
		assertFalse(config.mine.shouldMine("minecraft:air", true));

		config.mine.digObstructions = true;
		assertTrue(config.mine.shouldMine("minecraft:stone", true));
		assertTrue(config.mine.shouldMine("minecraft:iron_ore", false));
		assertFalse(config.mine.shouldMine("minecraft:bedrock", true));
	}
}
