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

		int ticks = 1;
		while (ticks < 40) {
			BotAction action = machine.tick(input);
			if (action.type != ActionType.BREAK) {
				break;
			}
			assertFalse(action.breakStart);
			ticks++;
		}
		assertEquals(expected, ticks);
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
