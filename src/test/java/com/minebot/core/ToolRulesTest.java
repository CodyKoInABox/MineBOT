package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRulesTest {
	@Test
	void picksTheRightKindAndIgnoresTheWrongTool() {
		Map<String, Integer> bag = new LinkedHashMap<>();
		bag.put("minecraft:diamond_pickaxe", 1);
		bag.put("minecraft:stone_shovel", 1);
		bag.put("minecraft:iron_axe", 1);
		bag.put("minecraft:wooden_hoe", 1);

		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:grass_block"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("dirt"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:sand"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:gravel"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:clay"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:soul_sand"));
		assertEquals(ToolKind.SHOVEL, ToolRules.kindFor("minecraft:snow"));
		assertEquals("minecraft:stone_shovel", ToolRules.choose("minecraft:dirt", bag, "minecraft:diamond_pickaxe"));

		assertEquals(ToolKind.PICKAXE, ToolRules.kindFor("minecraft:stone"));
		assertEquals(ToolKind.PICKAXE, ToolRules.kindFor("minecraft:cobblestone"));
		assertEquals(ToolKind.PICKAXE, ToolRules.kindFor("minecraft:deepslate"));
		assertEquals(ToolKind.PICKAXE, ToolRules.kindFor("minecraft:diamond_ore"));
		assertEquals("minecraft:diamond_pickaxe", ToolRules.choose("minecraft:stone", bag, "minecraft:stone_shovel"));
		assertEquals("", ToolRules.choose("minecraft:obsidian", Map.of("minecraft:wooden_pickaxe", 1), ""));

		assertEquals(ToolKind.AXE, ToolRules.kindFor("minecraft:oak_log"));
		assertEquals(ToolKind.AXE, ToolRules.kindFor("minecraft:oak_wood"));
		assertEquals(ToolKind.AXE, ToolRules.kindFor("minecraft:oak_planks"));
		assertEquals("minecraft:iron_axe", ToolRules.choose("minecraft:oak_log", bag, "minecraft:diamond_pickaxe"));

		assertEquals(ToolKind.HOE, ToolRules.kindFor("minecraft:oak_leaves"));
		assertEquals(ToolKind.HOE, ToolRules.kindFor("minecraft:hay_block"));
		assertEquals(ToolKind.HOE, ToolRules.kindFor("minecraft:nether_wart_block"));
		assertEquals("minecraft:wooden_hoe", ToolRules.choose("minecraft:oak_leaves", bag, "minecraft:diamond_pickaxe"));
	}

	@Test
	void highestTierThatCanHarvestAndHandWhenNoneFit() {
		Map<String, Integer> bag = new LinkedHashMap<>();
		bag.put("minecraft:wooden_pickaxe", 1);
		bag.put("minecraft:stone_pickaxe", 1);
		bag.put("minecraft:golden_pickaxe", 1);
		assertEquals("minecraft:stone_pickaxe", ToolRules.choose("minecraft:iron_ore", bag, "minecraft:golden_pickaxe"));
		assertEquals("", ToolRules.choose("minecraft:ancient_debris", bag, ""));
		assertEquals("minecraft:diamond_pickaxe", ToolRules.choose("minecraft:obsidian", Map.of("minecraft:diamond_pickaxe", 1), "minecraft:stone_shovel"));

		assertEquals(4f, ToolRules.speedAgainst("minecraft:stone_shovel", "minecraft:dirt"));
		assertEquals(1f, ToolRules.speedAgainst("minecraft:diamond_pickaxe", "minecraft:dirt"));
		assertEquals(8f, ToolRules.speedAgainst("minecraft:diamond_pickaxe", "minecraft:stone"));
		assertTrue(ToolRules.canHarvest("minecraft:stone_pickaxe", "minecraft:iron_ore"));
		assertFalse(ToolRules.canHarvest("minecraft:wooden_pickaxe", "minecraft:iron_ore"));
		assertTrue(ToolRules.canHarvest("", "minecraft:dirt"));
	}
}
