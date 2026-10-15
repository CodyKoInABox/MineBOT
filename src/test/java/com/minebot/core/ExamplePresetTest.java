package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExamplePresetTest {
	@Test
	void exampleQuarryIsTwentyFiveToBedrockWithStairAndLists() {
		TaskConfig config = Presets.exampleQuarry(new BlockPos(10, 64, -4));
		assertEquals(Presets.EXAMPLE_QUARRY, config.name);
		assertEquals(25, config.area.sizeX());
		assertEquals(25, config.area.sizeZ());
		assertEquals(10, config.area.minX());
		assertEquals(34, config.area.maxX());
		assertEquals(-4, config.area.minZ());
		assertEquals(20, config.area.maxZ());
		assertEquals(64, config.area.surfaceY());
		assertEquals(DepthSpec.Kind.BEDROCK, config.depth.kind);
		assertEquals(-63, config.depth.floorInclusive(64, -64));
		assertTrue(config.stair);
		assertTrue(config.mine.everything);
		assertTrue(config.storage.depositIds.contains("minecraft:cobblestone"));
		assertTrue(config.storage.depositIds.contains("minecraft:cobbled_deepslate"));
		assertTrue(config.storage.depositIds.contains("minecraft:deepslate"));
		assertTrue(config.storage.depositIds.contains("minecraft:diamond_ore"));
		assertTrue(config.storage.depositIds.contains("minecraft:deepslate_iron_ore") == false);
		assertTrue(config.storage.keepIds.contains("minecraft:torch"));
		assertTrue(config.storage.keepIds.contains("minecraft:bread"));
		assertTrue(config.storage.keepIds.contains("minecraft:oak_log"));
		assertTrue(config.storage.keepIds.contains("minecraft:iron_ingot"));
		assertTrue(config.storage.keepIds.contains("minecraft:iron_pickaxe"));
		assertTrue(config.survival.eat);
		assertTrue(config.survival.sleep);
		assertTrue(config.survival.placeTorches);
		assertTrue(config.survival.autoCraftPicks);

		int surface = 64;
		int floor = -63;
		int bedrock = -64;
		List<StairTread> stairs = SpiralStair.plan(config.area, surface, floor);
		assertEquals(surface - floor + 1, stairs.size());
		int protectedCount = 0;
		for (StairTread tread : stairs) {
			for (int y = surface; y >= floor; y--) {
				if (y <= tread.y() && y > bedrock) {
					protectedCount++;
				}
			}
		}
		int expected = 25 * 25 * (surface - floor + 1) - protectedCount;
		assertEquals(expected, QuarryVolume.countMineable(config, bedrock));
		assertTrue(expected < 25 * 25 * (surface - floor + 1));
	}

	@Test
	void presetsRoundTripAndResume() {
		TaskConfig config = Presets.exampleQuarry(new BlockPos(0, 64, 0));
		PresetStore store = new PresetStore();
		store.save(Presets.EXAMPLE_QUARRY, config);
		TaskConfig loaded = store.load(Presets.EXAMPLE_QUARRY).orElseThrow();
		assertEquals(25, loaded.area.sizeX());
		assertEquals(25, loaded.area.sizeZ());
		assertTrue(loaded.stair);

		JobProgress progress = new JobProgress();
		progress.running = true;
		progress.initialized = true;
		progress.x = 4;
		progress.y = 12;
		progress.z = 9;
		progress.status = Status.RUNNING;
		store.rememberProgress(progress);
		store.rememberActive(config);

		JobProgress resumed = store.progress();
		assertTrue(resumed.running);
		assertEquals(4, resumed.x);
		assertEquals(12, resumed.y);
		assertEquals(9, resumed.z);
		assertEquals(25, store.active().area.sizeX());
	}
}
