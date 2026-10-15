package com.minebot.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replacement tool from whatever is actually in the inventory.
 * Logs become planks, planks become sticks and a crafting table, then the tool.
 * Netherite also needs the diamond tool of the same kind, an ingot, and an upgrade template.
 * A healthy tool of the required kind is left alone. A low or missing one is replaced.
 */
public final class CraftPlan {
	private CraftPlan() {}

	public static List<CraftStep> plan(Map<String, Integer> items) {
		return plan(items, ToolKind.PICKAXE, ToolTier.WOOD, Map.of());
	}

	public static List<CraftStep> plan(
			Map<String, Integer> items,
			ToolKind kind,
			ToolTier minimum,
			Map<String, Integer> remainingUses
	) {
		if (kind == null) {
			return List.of();
		}
		ToolTier floor = minimum == null ? ToolTier.WOOD : minimum;
		Map<String, Integer> bag = copy(items);
		if (hasHealthy(bag, kind, floor, remainingUses)) {
			return List.of();
		}
		ToolTier target = bestCraftable(bag, kind, floor);
		if (target == null) {
			return List.of();
		}
		List<CraftStep> steps = new ArrayList<>();
		if (target == ToolTier.NETHERITE) {
			if (count(bag, ToolItems.diamondOf(kind)) <= 0) {
				craftTool(bag, steps, kind, ToolTier.DIAMOND);
			}
			smithNetherite(bag, steps, kind);
			return steps;
		}
		craftTool(bag, steps, kind, target);
		return steps;
	}

	private static boolean hasHealthy(
			Map<String, Integer> bag,
			ToolKind kind,
			ToolTier minimum,
			Map<String, Integer> remainingUses
	) {
		for (Map.Entry<String, Integer> entry : bag.entrySet()) {
			if (entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			if (ToolItems.kindOf(entry.getKey()) != kind) {
				continue;
			}
			ToolTier tier = ToolItems.tierOf(entry.getKey());
			if (tier == null || tier.harvestLevel < minimum.harvestLevel) {
				continue;
			}
			if (ToolRules.healthy(entry.getKey(), remainingUses)) {
				return true;
			}
		}
		return false;
	}

	private static ToolTier bestCraftable(Map<String, Integer> bag, ToolKind kind, ToolTier minimum) {
		ToolTier[] tiers = ToolTier.values();
		for (int i = tiers.length - 1; i >= 0; i--) {
			ToolTier tier = tiers[i];
			if (!tier.canCraft() || tier.harvestLevel < minimum.harvestLevel) {
				continue;
			}
			if (canCraft(bag, kind, tier)) {
				return tier;
			}
		}
		return null;
	}

	private static boolean canCraft(Map<String, Integer> bag, ToolKind kind, ToolTier tier) {
		try {
			Map<String, Integer> copy = copy(bag);
			List<CraftStep> ignored = new ArrayList<>();
			if (tier == ToolTier.NETHERITE) {
				if (count(copy, "minecraft:netherite_ingot") <= 0
						|| count(copy, "minecraft:netherite_upgrade_smithing_template") <= 0) {
					return false;
				}
				if (count(copy, ToolItems.diamondOf(kind)) <= 0) {
					craftTool(copy, ignored, kind, ToolTier.DIAMOND);
				}
				smithNetherite(copy, ignored, kind);
				return true;
			}
			craftTool(copy, ignored, kind, tier);
			return true;
		} catch (CannotCraft ignored) {
			return false;
		}
	}

	private static void craftTool(Map<String, Integer> bag, List<CraftStep> steps, ToolKind kind, ToolTier tier) {
		ensureTable(bag, steps);
		ensureSticks(bag, steps, 2);
		String material = switch (tier) {
			case WOOD -> takePlanks(bag, steps, 3);
			case STONE -> takeStone(bag, 3);
			case IRON -> takeExact(bag, "minecraft:iron_ingot", 3);
			case DIAMOND -> takeExact(bag, "minecraft:diamond", 3);
			case GOLD, NETHERITE -> throw new CannotCraft();
		};
		consume(bag, "minecraft:stick", 2);
		String output = ToolItems.id(kind, tier);
		add(bag, output, 1);
		Map<String, Integer> ingredients = new LinkedHashMap<>();
		ingredients.put(material, 3);
		ingredients.put("minecraft:stick", 2);
		steps.add(new CraftStep(output, 1, ingredients, "crafting_table"));
	}

	private static void smithNetherite(Map<String, Integer> bag, List<CraftStep> steps, ToolKind kind) {
		String base = ToolItems.diamondOf(kind);
		String output = ToolItems.id(kind, ToolTier.NETHERITE);
		takeExact(bag, "minecraft:netherite_upgrade_smithing_template", 1);
		takeExact(bag, base, 1);
		takeExact(bag, "minecraft:netherite_ingot", 1);
		add(bag, output, 1);
		Map<String, Integer> ingredients = new LinkedHashMap<>();
		ingredients.put("minecraft:netherite_upgrade_smithing_template", 1);
		ingredients.put(base, 1);
		ingredients.put("minecraft:netherite_ingot", 1);
		steps.add(new CraftStep(output, 1, ingredients, "smithing_table"));
	}

	private static void ensureTable(Map<String, Integer> bag, List<CraftStep> steps) {
		if (count(bag, "minecraft:crafting_table") > 0) {
			return;
		}
		String plank = ensurePlanks(bag, steps, 4);
		consume(bag, plank, 4);
		add(bag, "minecraft:crafting_table", 1);
		steps.add(new CraftStep("minecraft:crafting_table", 1, Map.of(plank, 4), "inventory"));
	}

	private static void ensureSticks(Map<String, Integer> bag, List<CraftStep> steps, int needed) {
		while (count(bag, "minecraft:stick") < needed) {
			String plank = ensurePlanks(bag, steps, 2);
			consume(bag, plank, 2);
			add(bag, "minecraft:stick", 4);
			steps.add(new CraftStep("minecraft:stick", 4, Map.of(plank, 2), "inventory"));
		}
	}

	/** Returns the plank id that can satisfy {@code needed}, converting logs if required. */
	private static String ensurePlanks(Map<String, Integer> bag, List<CraftStep> steps, int needed) {
		while (plankCount(bag) < needed) {
			String log = firstLog(bag);
			if (log == null) {
				throw new CannotCraft();
			}
			String plank = Ids.planksFor(log);
			if (plank == null) {
				throw new CannotCraft();
			}
			consume(bag, log, 1);
			add(bag, plank, 4);
			steps.add(new CraftStep(plank, 4, Map.of(log, 1), "inventory"));
		}
		return firstPlanks(bag);
	}

	private static String takePlanks(Map<String, Integer> bag, List<CraftStep> steps, int needed) {
		while (true) {
			for (Map.Entry<String, Integer> entry : bag.entrySet()) {
				if (entry.getValue() >= needed && Ids.isPlanks(entry.getKey())) {
					String id = entry.getKey();
					consume(bag, id, needed);
					return id;
				}
			}
			String log = firstLog(bag);
			if (log == null) {
				throw new CannotCraft();
			}
			String plank = Ids.planksFor(log);
			if (plank == null) {
				throw new CannotCraft();
			}
			consume(bag, log, 1);
			add(bag, plank, 4);
			steps.add(new CraftStep(plank, 4, Map.of(log, 1), "inventory"));
		}
	}

	private static String takeStone(Map<String, Integer> bag, int count) {
		if (count(bag, "minecraft:cobblestone") >= count) {
			consume(bag, "minecraft:cobblestone", count);
			return "minecraft:cobblestone";
		}
		if (count(bag, "minecraft:cobbled_deepslate") >= count) {
			consume(bag, "minecraft:cobbled_deepslate", count);
			return "minecraft:cobbled_deepslate";
		}
		throw new CannotCraft();
	}

	private static String takeExact(Map<String, Integer> bag, String id, int count) {
		if (count(bag, id) < count) {
			throw new CannotCraft();
		}
		consume(bag, id, count);
		return id;
	}

	private static int plankCount(Map<String, Integer> bag) {
		int total = 0;
		for (Map.Entry<String, Integer> entry : bag.entrySet()) {
			if (Ids.isPlanks(entry.getKey())) {
				total += entry.getValue();
			}
		}
		return total;
	}

	private static String firstPlanks(Map<String, Integer> bag) {
		for (Map.Entry<String, Integer> entry : bag.entrySet()) {
			if (entry.getValue() > 0 && Ids.isPlanks(entry.getKey())) {
				return entry.getKey();
			}
		}
		return null;
	}

	private static String firstLog(Map<String, Integer> bag) {
		for (Map.Entry<String, Integer> entry : bag.entrySet()) {
			if (entry.getValue() > 0 && Ids.isLog(entry.getKey())) {
				return entry.getKey();
			}
		}
		return null;
	}

	private static int count(Map<String, Integer> bag, String id) {
		return bag.getOrDefault(Ids.normalize(id), 0);
	}

	private static void consume(Map<String, Integer> bag, String id, int count) {
		String key = Ids.normalize(id);
		int have = bag.getOrDefault(key, 0);
		if (have < count) {
			throw new CannotCraft();
		}
		if (have == count) {
			bag.remove(key);
		} else {
			bag.put(key, have - count);
		}
	}

	private static void add(Map<String, Integer> bag, String id, int count) {
		bag.merge(Ids.normalize(id), count, Integer::sum);
	}

	private static Map<String, Integer> copy(Map<String, Integer> items) {
		Map<String, Integer> bag = new LinkedHashMap<>();
		if (items == null) {
			return bag;
		}
		items.forEach((id, count) -> {
			if (count != null && count > 0) {
				bag.merge(Ids.normalize(id), count, Integer::sum);
			}
		});
		return bag;
	}

	private static final class CannotCraft extends RuntimeException {
		CannotCraft() {}
	}
}
