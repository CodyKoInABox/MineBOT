package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuarryMachineTest {
	@Test
	void pausesWhenChestsAreFull() {
		TaskConfig config = Presets.exampleQuarry(new BlockPos(0, 64, 0));
		QuarryMachine machine = new QuarryMachine(config, new JobProgress());
		machine.start(-64);

		TickInput input = new TickInput();
		input.hunger = 20;
		input.dayTime = 1000;
		input.inventory = new LinkedHashMap<>();
		input.inventory.put("minecraft:cobblestone", 64);
		input.chestCanAcceptDeposit = false;
		input.chestPos = new BlockPos(0, 64, 0);
		input.toolId = "minecraft:iron_pickaxe";

		BotAction action = machine.tick(input);
		assertEquals(ActionType.PAUSE, action.type);
		assertEquals(Status.PAUSED_CHESTS_FULL, action.status);
		assertTrue(machine.progress().running);
	}

	@Test
	void breakLastsTheVanillaNumberOfTicks() {
		TaskConfig config = new TaskConfig();
		config.area.setFirst(new BlockPos(0, 10, 0));
		config.area.setSecond(new BlockPos(0, 10, 0));
		config.depth = DepthSpec.layers(1);
		config.stair = false;
		config.mine = MineFilter.everything();
		config.survival.eat = false;
		config.survival.sleep = false;
		config.survival.autoCraftPicks = false;
		config.survival.placeTorches = false;
		config.storage.depositIds.clear();

		QuarryMachine machine = new QuarryMachine(config, new JobProgress());
		machine.start(-64);

		TickInput input = new TickInput();
		input.playerX = 2;
		input.playerY = 11;
		input.playerZ = 0;
		input.toolId = "minecraft:diamond_pickaxe";
		input.world = WorldQuery.stoneFill(10);
		input.hunger = 20;
		input.dayTime = 1000;

		int expected = BreakTimes.ticks(1.5f, 8f, true);
		assertTrue(expected > 1);

		BotAction first = machine.tick(input);
		assertEquals(ActionType.BREAK, first.type);
		assertTrue(first.breakStart);
		assertEquals(0, first.x);
		assertEquals(10, first.y);
		assertEquals(0, first.z);
		assertEquals("minecraft:diamond_pickaxe", first.itemId);

		int ticks = 1;
		while (ticks < 40) {
			BotAction action = machine.tick(input);
			if (action.type != ActionType.BREAK) {
				break;
			}
			assertFalse(action.breakStart);
			assertEquals(first.x, action.x);
			assertEquals(first.y, action.y);
			assertEquals(first.z, action.z);
			assertEquals(first.itemId, action.itemId);
			ticks++;
		}
		assertEquals(expected, ticks);
		assertFalse(machine.progress().breaking);
	}

	@Test
	void usesShovelOnDirtAndPickOnStoneUntilTheBreakFinishes() {
		TaskConfig dirtJob = oneBlock(10);
		dirtJob.survival.autoCraftPicks = false;
		QuarryMachine dirt = new QuarryMachine(dirtJob, new JobProgress());
		dirt.start(-64);
		TickInput dirtIn = standing();
		dirtIn.world = blockFill(10, "minecraft:dirt", 0.5f);
		dirtIn.inventory.put("minecraft:diamond_pickaxe", 1);
		dirtIn.inventory.put("minecraft:stone_shovel", 1);
		dirtIn.toolId = "minecraft:diamond_pickaxe";

		int shovelTicks = BreakTimes.ticks(0.5f, 4f, true);
		BotAction started = dirt.tick(dirtIn);
		assertEquals(ActionType.BREAK, started.type);
		assertEquals("minecraft:stone_shovel", started.itemId);
		assertEquals(0, started.x);
		int seen = 1;
		while (seen < 20) {
			BotAction action = dirt.tick(dirtIn);
			if (action.type != ActionType.BREAK) {
				break;
			}
			assertEquals(0, action.x);
			assertEquals(10, action.y);
			assertEquals(0, action.z);
			assertEquals("minecraft:stone_shovel", action.itemId);
			seen++;
		}
		assertEquals(shovelTicks, seen);

		TaskConfig stoneJob = oneBlock(10);
		stoneJob.survival.autoCraftPicks = false;
		QuarryMachine stone = new QuarryMachine(stoneJob, new JobProgress());
		stone.start(-64);
		TickInput stoneIn = standing();
		stoneIn.world = WorldQuery.stoneFill(10);
		stoneIn.inventory.put("minecraft:diamond_shovel", 1);
		stoneIn.inventory.put("minecraft:iron_pickaxe", 1);
		BotAction pick = stone.tick(stoneIn);
		assertEquals("minecraft:iron_pickaxe", pick.itemId);
		assertEquals(0, pick.x);
		assertEquals(10, pick.y);
	}

	private static TaskConfig oneBlock(int y) {
		TaskConfig config = new TaskConfig();
		config.area.setFirst(new BlockPos(0, y, 0));
		config.area.setSecond(new BlockPos(0, y, 0));
		config.depth = DepthSpec.layers(1);
		config.stair = false;
		config.mine = MineFilter.everything();
		config.survival.eat = false;
		config.survival.sleep = false;
		config.survival.autoCraftPicks = false;
		config.survival.placeTorches = false;
		config.storage.depositIds.clear();
		return config;
	}

	private static TickInput standing() {
		TickInput input = new TickInput();
		input.playerX = 2;
		input.playerY = 11;
		input.playerZ = 0;
		input.hunger = 20;
		input.dayTime = 1000;
		input.inventory = new LinkedHashMap<>();
		return input;
	}

	private static WorldQuery blockFill(int fillY, String id, float hardness) {
		return new WorldQuery() {
			@Override
			public String blockId(int x, int y, int z) {
				return y <= fillY ? id : "minecraft:air";
			}

			@Override
			public float hardness(int x, int y, int z) {
				return y <= fillY ? hardness : 0f;
			}

			@Override
			public boolean solid(int x, int y, int z) {
				return y <= fillY;
			}
		};
	}

	@Test
	void pathfinderWalksAndJumps() {
		Pathfinder.Collision flat = (x, y, z) -> y <= 0 && x >= 0 && x <= 5 && z == 0;
		List<Pathfinder.Move> walk = Pathfinder.find(0, 1, 0, 4, 1, 0, flat, 64);
		assertEquals(4, walk.size());
		assertEquals(Pathfinder.Kind.WALK, walk.get(0).kind());
		assertEquals(4, walk.get(3).x());

		Pathfinder.Collision step = (x, y, z) -> (x == 0 && y <= 0 && z == 0) || (x == 1 && y <= 1 && z == 0);
		List<Pathfinder.Move> jump = Pathfinder.find(0, 1, 0, 1, 2, 0, step, 64);
		assertEquals(1, jump.size());
		assertEquals(Pathfinder.Kind.JUMP, jump.get(0).kind());
		assertEquals(1, jump.get(0).x());
		assertEquals(2, jump.get(0).y());
	}
}
