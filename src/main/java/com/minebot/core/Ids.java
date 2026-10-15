package com.minebot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Block and item id helpers. Ids are namespaced strings such as {@code minecraft:cobblestone}. */
public final class Ids {
	public static final List<String> ORE_BLOCKS = List.of(
			"minecraft:coal_ore",
			"minecraft:deepslate_coal_ore",
			"minecraft:copper_ore",
			"minecraft:deepslate_copper_ore",
			"minecraft:gold_ore",
			"minecraft:deepslate_gold_ore",
			"minecraft:redstone_ore",
			"minecraft:deepslate_redstone_ore",
			"minecraft:emerald_ore",
			"minecraft:deepslate_emerald_ore",
			"minecraft:lapis_ore",
			"minecraft:deepslate_lapis_ore",
			"minecraft:diamond_ore",
			"minecraft:deepslate_diamond_ore",
			"minecraft:nether_gold_ore",
			"minecraft:nether_quartz_ore",
			"minecraft:ancient_debris"
	);

	public static final List<String> ORE_DROPS = List.of(
			"minecraft:coal",
			"minecraft:raw_copper",
			"minecraft:raw_gold",
			"minecraft:redstone",
			"minecraft:lapis_lazuli",
			"minecraft:diamond",
			"minecraft:emerald",
			"minecraft:quartz",
			"minecraft:ancient_debris"
	);

	public static final List<String> FOOD = List.of(
			"minecraft:bread",
			"minecraft:cooked_beef",
			"minecraft:cooked_porkchop",
			"minecraft:cooked_chicken",
			"minecraft:cooked_mutton",
			"minecraft:cooked_rabbit",
			"minecraft:cooked_cod",
			"minecraft:cooked_salmon",
			"minecraft:apple",
			"minecraft:golden_apple",
			"minecraft:enchanted_golden_apple",
			"minecraft:carrot",
			"minecraft:golden_carrot",
			"minecraft:baked_potato",
			"minecraft:potato",
			"minecraft:beetroot",
			"minecraft:beetroot_soup",
			"minecraft:mushroom_stew",
			"minecraft:rabbit_stew",
			"minecraft:cookie",
			"minecraft:pumpkin_pie",
			"minecraft:melon_slice",
			"minecraft:sweet_berries",
			"minecraft:glow_berries",
			"minecraft:dried_kelp",
			"minecraft:tropical_fish",
			"minecraft:spider_eye",
			"minecraft:rotten_flesh"
	);

	public static final List<String> WOOD = List.of(
			"minecraft:oak_log",
			"minecraft:spruce_log",
			"minecraft:birch_log",
			"minecraft:jungle_log",
			"minecraft:acacia_log",
			"minecraft:dark_oak_log",
			"minecraft:mangrove_log",
			"minecraft:cherry_log",
			"minecraft:pale_oak_log",
			"minecraft:crimson_stem",
			"minecraft:warped_stem",
			"minecraft:oak_wood",
			"minecraft:spruce_wood",
			"minecraft:birch_wood",
			"minecraft:jungle_wood",
			"minecraft:acacia_wood",
			"minecraft:dark_oak_wood",
			"minecraft:mangrove_wood",
			"minecraft:cherry_wood",
			"minecraft:pale_oak_wood",
			"minecraft:crimson_hyphae",
			"minecraft:warped_hyphae"
	);

	public static final List<String> PLANKS = List.of(
			"minecraft:oak_planks",
			"minecraft:spruce_planks",
			"minecraft:birch_planks",
			"minecraft:jungle_planks",
			"minecraft:acacia_planks",
			"minecraft:dark_oak_planks",
			"minecraft:mangrove_planks",
			"minecraft:cherry_planks",
			"minecraft:pale_oak_planks",
			"minecraft:bamboo_planks",
			"minecraft:crimson_planks",
			"minecraft:warped_planks"
	);

	public static final List<String> IRON = List.of(
			"minecraft:iron_ingot",
			"minecraft:iron_block",
			"minecraft:iron_nugget",
			"minecraft:raw_iron",
			"minecraft:iron_ore",
			"minecraft:deepslate_iron_ore"
	);

	public static final List<String> PICKAXES = List.of(
			"minecraft:wooden_pickaxe",
			"minecraft:stone_pickaxe",
			"minecraft:iron_pickaxe",
			"minecraft:diamond_pickaxe",
			"minecraft:netherite_pickaxe"
	);

	private Ids() {}

	public static String normalize(String id) {
		if (id == null) {
			return "";
		}
		String trimmed = id.trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			return "";
		}
		if (!trimmed.contains(":")) {
			return "minecraft:" + trimmed;
		}
		return trimmed;
	}

	public static List<String> parseList(String csv) {
		List<String> out = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return out;
		}
		for (String part : csv.split("[,\\s]+")) {
			String id = normalize(part);
			if (!id.isEmpty() && !out.contains(id)) {
				out.add(id);
			}
		}
		return out;
	}

	public static String join(List<String> ids) {
		return String.join(", ", ids);
	}

	public static boolean isFood(String id) {
		return FOOD.contains(normalize(id));
	}

	public static boolean isPickaxe(String id) {
		String normalized = normalize(id);
		return normalized.endsWith("_pickaxe") || PICKAXES.contains(normalized);
	}

	public static boolean isTool(String id) {
		return ToolItems.isTool(id);
	}

	public static boolean isLog(String id) {
		String normalized = normalize(id);
		return normalized.endsWith("_log")
				|| normalized.endsWith("_stem")
				|| normalized.endsWith("_hyphae")
				|| (normalized.endsWith("_wood") && !normalized.endsWith("_planks"));
	}

	public static boolean isPlanks(String id) {
		return normalize(id).endsWith("_planks");
	}

	/** Log, stem, hyphae, or wood item to the matching plank id. */
	public static String planksFor(String logId) {
		String id = normalize(logId);
		if (id.endsWith("_log")) {
			return id.substring(0, id.length() - 4) + "_planks";
		}
		if (id.endsWith("_stem")) {
			return id.substring(0, id.length() - 5) + "_planks";
		}
		if (id.endsWith("_hyphae")) {
			return id.substring(0, id.length() - 7) + "_planks";
		}
		if (id.endsWith("_wood")) {
			return id.substring(0, id.length() - 5) + "_planks";
		}
		return null;
	}

	public static boolean isAir(String id) {
		String normalized = normalize(id);
		return normalized.isEmpty()
				|| normalized.equals("minecraft:air")
				|| normalized.equals("minecraft:cave_air")
				|| normalized.equals("minecraft:void_air");
	}

	public static boolean isBedrock(String id) {
		return normalize(id).equals("minecraft:bedrock");
	}
}
