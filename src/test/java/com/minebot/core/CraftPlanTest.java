package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CraftPlanTest {
	@Test
	void ironPickFromLogsThenPlanksSticksAndTable() {
		Map<String, Integer> items = new LinkedHashMap<>();
		items.put("minecraft:oak_log", 2);
		items.put("minecraft:iron_ingot", 3);

		List<CraftStep> steps = CraftPlan.plan(items);
		assertEquals("minecraft:iron_pickaxe", steps.get(steps.size() - 1).outputId);
		assertEquals(3, steps.get(steps.size() - 1).ingredients.get("minecraft:iron_ingot"));
		assertEquals(2, steps.get(steps.size() - 1).ingredients.get("minecraft:stick"));
		assertEquals("crafting_table", steps.get(steps.size() - 1).station);

		int planks = indexOf(steps, "minecraft:oak_planks");
		int sticks = indexOf(steps, "minecraft:stick");
		int table = indexOf(steps, "minecraft:crafting_table");
		int pick = indexOf(steps, "minecraft:iron_pickaxe");
		assertTrue(planks < sticks);
		assertTrue(planks < table);
		assertTrue(sticks < pick);
		assertTrue(table < pick);
	}

	@Test
	void highestTierActuallyCraftable() {
		Map<String, Integer> rich = new LinkedHashMap<>();
		rich.put("minecraft:diamond", 3);
		rich.put("minecraft:iron_ingot", 3);
		rich.put("minecraft:cobblestone", 3);
		rich.put("minecraft:stick", 2);
		rich.put("minecraft:crafting_table", 1);
		List<CraftStep> diamond = CraftPlan.plan(rich);
		assertEquals(1, diamond.size());
		assertEquals("minecraft:diamond_pickaxe", diamond.get(0).outputId);

		Map<String, Integer> shortDiamonds = new LinkedHashMap<>();
		shortDiamonds.put("minecraft:diamond", 2);
		shortDiamonds.put("minecraft:iron_ingot", 3);
		shortDiamonds.put("minecraft:stick", 2);
		shortDiamonds.put("minecraft:crafting_table", 1);
		List<CraftStep> iron = CraftPlan.plan(shortDiamonds);
		assertEquals("minecraft:iron_pickaxe", iron.get(iron.size() - 1).outputId);

		Map<String, Integer> logs = Map.of("minecraft:oak_log", 3);
		List<CraftStep> wood = CraftPlan.plan(logs);
		assertEquals("minecraft:wooden_pickaxe", wood.get(wood.size() - 1).outputId);
		assertTrue(indexOf(wood, "minecraft:oak_planks") >= 0);
		assertTrue(indexOf(wood, "minecraft:stick") >= 0);
		assertTrue(indexOf(wood, "minecraft:crafting_table") >= 0);

		Map<String, Integer> stone = new LinkedHashMap<>();
		stone.put("minecraft:cobblestone", 3);
		stone.put("minecraft:oak_planks", 6);
		assertEquals("minecraft:stone_pickaxe", CraftPlan.plan(stone).getLast().outputId);
	}

	@Test
	void skipsWhenThatTierIsAlreadyHeldAndUpgradesNetherite() {
		Map<String, Integer> holding = new LinkedHashMap<>();
		holding.put("minecraft:iron_pickaxe", 1);
		holding.put("minecraft:iron_ingot", 3);
		holding.put("minecraft:stick", 2);
		holding.put("minecraft:crafting_table", 1);
		assertTrue(CraftPlan.plan(holding).isEmpty());

		Map<String, Integer> healthy = new LinkedHashMap<>();
		healthy.put("minecraft:diamond_pickaxe", 1);
		healthy.put("minecraft:netherite_ingot", 1);
		healthy.put("minecraft:netherite_upgrade_smithing_template", 1);
		assertTrue(CraftPlan.plan(healthy).isEmpty());

		Map<String, Integer> upgrade = new LinkedHashMap<>();
		upgrade.put("minecraft:diamond", 3);
		upgrade.put("minecraft:stick", 2);
		upgrade.put("minecraft:crafting_table", 1);
		upgrade.put("minecraft:netherite_ingot", 1);
		upgrade.put("minecraft:netherite_upgrade_smithing_template", 1);
		List<CraftStep> steps = CraftPlan.plan(upgrade);
		assertEquals("minecraft:diamond_pickaxe", steps.get(0).outputId);
		assertEquals("minecraft:netherite_pickaxe", steps.get(steps.size() - 1).outputId);
		assertEquals("smithing_table", steps.get(steps.size() - 1).station);

		List<CraftClick> clicks = CraftClicks.expand(steps.get(steps.size() - 1));
		assertEquals(CraftClick.Kind.GRID, clicks.get(0).kind);
		assertEquals("minecraft:netherite_upgrade_smithing_template", clicks.get(0).itemId);
		assertEquals(CraftClick.Kind.TAKE_RESULT, clicks.get(clicks.size() - 1).kind);

		List<CraftClick> pick = CraftClicks.expand(new CraftStep(
				"minecraft:iron_pickaxe",
				1,
				Map.of("minecraft:iron_ingot", 3, "minecraft:stick", 2),
				"crafting_table"
		));
		assertEquals("minecraft:iron_ingot", pick.get(0).itemId);
		assertEquals(0, pick.get(0).gridIndex);
		assertEquals(1, pick.get(1).gridIndex);
		assertEquals(2, pick.get(2).gridIndex);
		assertEquals("minecraft:stick", pick.get(3).itemId);
		assertEquals(4, pick.get(3).gridIndex);
		assertEquals(7, pick.get(4).gridIndex);
	}

	@Test
	void replacesTheKindThatIsLowOrMissing() {
		Map<String, Integer> logs = new LinkedHashMap<>();
		logs.put("minecraft:oak_log", 3);
		logs.put("minecraft:iron_pickaxe", 1);
		List<CraftStep> shovel = CraftPlan.plan(logs, ToolKind.SHOVEL, ToolTier.WOOD, Map.of());
		assertEquals("minecraft:wooden_shovel", shovel.get(shovel.size() - 1).outputId);
		assertTrue(indexOf(shovel, "minecraft:iron_pickaxe") < 0);

		Map<String, Integer> low = new LinkedHashMap<>();
		low.put("minecraft:iron_axe", 1);
		low.put("minecraft:iron_ingot", 3);
		low.put("minecraft:stick", 2);
		low.put("minecraft:crafting_table", 1);
		assertTrue(CraftPlan.plan(low, ToolKind.AXE, ToolTier.WOOD, Map.of("minecraft:iron_axe", 100)).isEmpty());
		List<CraftStep> replacement = CraftPlan.plan(low, ToolKind.AXE, ToolTier.WOOD, Map.of("minecraft:iron_axe", 4));
		assertEquals("minecraft:iron_axe", replacement.get(replacement.size() - 1).outputId);

		Map<String, Integer> hoeMats = new LinkedHashMap<>();
		hoeMats.put("minecraft:diamond", 3);
		hoeMats.put("minecraft:iron_ingot", 3);
		hoeMats.put("minecraft:stick", 2);
		hoeMats.put("minecraft:crafting_table", 1);
		assertEquals("minecraft:diamond_hoe", CraftPlan.plan(hoeMats, ToolKind.HOE, ToolTier.WOOD, Map.of()).getLast().outputId);

		Map<String, Integer> cannot = Map.of("minecraft:wooden_pickaxe", 1);
		assertTrue(CraftPlan.plan(cannot, ToolKind.PICKAXE, ToolTier.DIAMOND, Map.of()).isEmpty());
	}

	private static int indexOf(List<CraftStep> steps, String output) {
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).outputId.equals(output)) {
				return i;
			}
		}
		return -1;
	}
}
