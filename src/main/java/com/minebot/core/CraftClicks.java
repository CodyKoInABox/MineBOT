package com.minebot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Expands one craft step into single clicks. Picks use the 3×3 grid; the rest fit in the 2×2. */
public final class CraftClicks {
	private CraftClicks() {}

	public static List<CraftClick> expand(CraftStep step) {
		List<CraftClick> clicks = new ArrayList<>();
		String output = step.outputId;
		if (output.equals("minecraft:netherite_pickaxe")) {
			clicks.add(CraftClick.grid("minecraft:netherite_upgrade_smithing_template", 0));
			clicks.add(CraftClick.grid("minecraft:diamond_pickaxe", 1));
			clicks.add(CraftClick.grid("minecraft:netherite_ingot", 2));
			clicks.add(CraftClick.takeResult());
			return clicks;
		}
		if (output.endsWith("_planks")) {
			String log = firstIngredient(step);
			clicks.add(CraftClick.grid(log, 0));
			clicks.add(CraftClick.takeResult());
			return clicks;
		}
		if (output.equals("minecraft:crafting_table")) {
			fill(clicks, firstIngredient(step), new int[] {0, 1, 2, 3});
			clicks.add(CraftClick.takeResult());
			return clicks;
		}
		if (output.equals("minecraft:stick")) {
			fill(clicks, firstIngredient(step), new int[] {0, 2});
			clicks.add(CraftClick.takeResult());
			return clicks;
		}
		if (output.endsWith("_pickaxe")) {
			String material = materialOf(step);
			clicks.add(CraftClick.grid(material, 0));
			clicks.add(CraftClick.grid(material, 1));
			clicks.add(CraftClick.grid(material, 2));
			clicks.add(CraftClick.grid("minecraft:stick", 4));
			clicks.add(CraftClick.grid("minecraft:stick", 7));
			clicks.add(CraftClick.takeResult());
			return clicks;
		}
		clicks.add(CraftClick.takeResult());
		return clicks;
	}

	private static void fill(List<CraftClick> clicks, String itemId, int[] slots) {
		for (int slot : slots) {
			clicks.add(CraftClick.grid(itemId, slot));
		}
	}

	private static String firstIngredient(CraftStep step) {
		for (Map.Entry<String, Integer> entry : step.ingredients.entrySet()) {
			return entry.getKey();
		}
		return "";
	}

	private static String materialOf(CraftStep step) {
		for (String id : step.ingredients.keySet()) {
			if (!id.equals("minecraft:stick")) {
				return id;
			}
		}
		return "";
	}
}
