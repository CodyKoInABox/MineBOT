package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChestPolicyTest {
	@Test
	void depositsCobbleAndKeepsSupplies() {
		Map<String, Integer> inventory = new LinkedHashMap<>();
		inventory.put("minecraft:torch", 32);
		inventory.put("minecraft:cobblestone", 64);
		inventory.put("minecraft:iron_ingot", 3);
		inventory.put("minecraft:iron_pickaxe", 1);

		List<String> deposit = List.of("minecraft:cobblestone", "minecraft:torch", "minecraft:diamond");
		List<String> keep = List.of("minecraft:torch", "minecraft:iron_ingot");

		ChestDecision decision = ChestPolicy.evaluate(inventory, deposit, keep, true);
		assertEquals(ChestDecision.Type.DEPOSIT, decision.type);
		assertEquals("minecraft:cobblestone", decision.itemId);
		assertEquals(64, decision.count);

		inventory.remove("minecraft:cobblestone");
		ChestDecision kept = ChestPolicy.evaluate(inventory, deposit, keep, true);
		assertEquals(ChestDecision.Type.NONE, kept.type);
	}

	@Test
	void fullChestPausesInsteadOfDropping() {
		Map<String, Integer> inventory = new LinkedHashMap<>();
		inventory.put("cobblestone", 16);
		ChestDecision decision = ChestPolicy.evaluate(
				inventory,
				List.of("minecraft:cobblestone"),
				List.of("minecraft:torch"),
				false
		);
		assertEquals(ChestDecision.Type.PAUSE, decision.type);
		assertEquals(Status.PAUSED_CHESTS_FULL, decision.status);
	}
}
