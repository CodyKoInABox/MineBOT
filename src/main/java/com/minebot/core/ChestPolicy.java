package com.minebot.core;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Deposit listed resources. Supplies stay in the inventory.
 * If a deposit cannot fit, the job pauses instead of spilling items.
 */
public final class ChestPolicy {
	private ChestPolicy() {}

	public static ChestDecision evaluate(
			Map<String, Integer> inventory,
			Collection<String> depositIds,
			Collection<String> keepIds,
			boolean canInsert
	) {
		String deposit = null;
		int count = 0;
		for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
			if (entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			String id = Ids.normalize(entry.getKey());
			if (kept(id, keepIds)) {
				continue;
			}
			if (listed(id, depositIds)) {
				deposit = id;
				count = entry.getValue();
				break;
			}
		}
		if (deposit == null) {
			return ChestDecision.none();
		}
		if (!canInsert) {
			return ChestDecision.pause();
		}
		return ChestDecision.deposit(deposit, count);
	}

	public static boolean kept(String itemId, Collection<String> keepIds) {
		String id = Ids.normalize(itemId);
		if (ToolItems.isTool(id)) {
			return true;
		}
		for (String keep : keepIds) {
			if (Ids.normalize(keep).equals(id)) {
				return true;
			}
		}
		return false;
	}

	public static boolean listed(String itemId, Collection<String> ids) {
		String id = Ids.normalize(itemId);
		for (String candidate : ids) {
			if (Ids.normalize(candidate).equals(id)) {
				return true;
			}
		}
		return false;
	}

	public static String foodIn(Map<String, Integer> inventory) {
		for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
			if (entry.getValue() != null && entry.getValue() > 0 && Ids.isFood(entry.getKey())) {
				return Ids.normalize(entry.getKey());
			}
		}
		return null;
	}

	public static boolean hasItem(Map<String, Integer> inventory, String itemId) {
		String id = Ids.normalize(itemId);
		for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
			if (entry.getValue() != null && entry.getValue() > 0 && Ids.normalize(entry.getKey()).equals(id)) {
				return true;
			}
		}
		return false;
	}

	public static String heldPick(Map<String, Integer> inventory) {
		String best = null;
		int bestTier = -1;
		for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
			if (entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			String id = Ids.normalize(entry.getKey());
			int tier = pickRank(id);
			if (tier > bestTier) {
				bestTier = tier;
				best = id;
			}
		}
		return best;
	}

	public static int pickRank(String itemId) {
		String id = Ids.normalize(itemId).toLowerCase(Locale.ROOT);
		return switch (id) {
			case "minecraft:wooden_pickaxe" -> 0;
			case "minecraft:stone_pickaxe" -> 1;
			case "minecraft:iron_pickaxe" -> 2;
			case "minecraft:diamond_pickaxe" -> 3;
			case "minecraft:netherite_pickaxe" -> 4;
			default -> -1;
		};
	}
}
