package com.minebot.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replacement pick from whatever is actually in the inventory.
 * Logs become planks, planks become sticks and a crafting table, then the pick.
 * Netherite also needs a diamond pick, an ingot, and a netherite upgrade template.
 */
public final class CraftPlan {
	private CraftPlan() {}

	public static List<CraftStep> plan(Map<String, Integer> items) {
		Map<String, Integer> bag = copy(items);
		PickTier target = bestCraftable(bag);
		if (target == null) {
			return List.of();
		}
		PickTier held = bestHeld(bag);
		if (held != null && held.ordinal() >= target.ordinal()) {
			return List.of();
		}
		List<CraftStep> steps = new ArrayList<>();
		if (target == PickTier.NETHERITE) {
			if (count(bag, PickTier.DIAMOND.itemId) <= 0) {
				craftPick(bag, steps, PickTier.DIAMOND);
			}
			smithNetherite(bag, steps);
			return steps;
		}
		craftPick(bag, steps, target);
		return steps;
	}

	private static PickTier bestCraftable(Map<String, Integer> bag) {
		for (int i = PickTier.values().length - 1; i >= 0; i--) {
			PickTier tier = PickTier.values()[i];
			if (canCraft(bag, tier)) {
				return tier;
			}
		}
		return null;
	}

	private static boolean canCraft(Map<String, Integer> bag, PickTier tier) {
		try {
			Map<String, Integer> copy = copy(bag);
			List<CraftStep> ignored = new ArrayList<>();
			if (tier == PickTier.NETHERITE) {
				if (count(copy, "minecraft:netherite_ingot") <= 0
						|| count(copy, "minecraft:netherite_upgrade_smithing_template") <= 0) {
					return false;
				}
				if (count(copy, PickTier.DIAMOND.itemId) <= 0) {
					craftPick(copy, ignored, PickTier.DIAMOND);
				}
				smithNetherite(copy, ignored);
				return true;
			}
			craftPick(copy, ignored, tier);
			return true;
		} catch (CannotCraft ignored) {
			return false;
		}
	}

	private static void craftPick(Map<String, Integer> bag, List<CraftStep> steps, PickTier tier) {
		ensureTable(bag, steps);
		ensureSticks(bag, steps, 2);
		String material = switch (tier) {
			case WOOD -> takePlanks(bag, steps, 3);
			case STONE -> takeStone(bag, 3);
			case IRON -> takeExact(bag, "minecraft:iron_ingot", 3);
			case DIAMOND -> takeExact(bag, "minecraft:diamond", 3);
			case NETHERITE -> throw new CannotCraft();
		};
		consume(bag, "minecraft:stick", 2);
		add(bag, tier.itemId, 1);
		Map<String, Integer> ingredients = new LinkedHashMap<>();
		ingredients.put(material, 3);
		ingredients.put("minecraft:stick", 2);
		steps.add(new CraftStep(tier.itemId, 1, ingredients, "crafting_table"));
	}

	private static void smithNetherite(Map<String, Integer> bag, List<CraftStep> steps) {
		takeExact(bag, "minecraft:netherite_upgrade_smithing_template", 1);
		takeExact(bag, "minecraft:diamond_pickaxe", 1);
		takeExact(bag, "minecraft:netherite_ingot", 1);
		add(bag, PickTier.NETHERITE.itemId, 1);
		Map<String, Integer> ingredients = new LinkedHashMap<>();
		ingredients.put("minecraft:netherite_upgrade_smithing_template", 1);
		ingredients.put("minecraft:diamond_pickaxe", 1);
		ingredients.put("minecraft:netherite_ingot", 1);
		steps.add(new CraftStep(PickTier.NETHERITE.itemId, 1, ingredients, "smithing_table"));
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

	private static PickTier bestHeld(Map<String, Integer> bag) {
		PickTier best = null;
		for (PickTier tier : PickTier.values()) {
			if (count(bag, tier.itemId) > 0) {
				best = tier;
			}
		}
		return best;
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
