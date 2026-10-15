package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

/** Item ids for shovels, pickaxes, axes, and hoes. */
public final class ToolItems {
	private ToolItems() {}

	public static String id(ToolKind kind, ToolTier tier) {
		String material = switch (tier) {
			case WOOD -> "wooden";
			case GOLD -> "golden";
			default -> tier.name().toLowerCase(java.util.Locale.ROOT);
		};
		String tool = switch (kind) {
			case SHOVEL -> "shovel";
			case PICKAXE -> "pickaxe";
			case AXE -> "axe";
			case HOE -> "hoe";
		};
		return "minecraft:" + material + "_" + tool;
	}

	public static ToolKind kindOf(String itemId) {
		String id = Ids.normalize(itemId);
		if (id.endsWith("_shovel")) {
			return ToolKind.SHOVEL;
		}
		if (id.endsWith("_pickaxe")) {
			return ToolKind.PICKAXE;
		}
		if (id.endsWith("_axe") && !id.endsWith("_pickaxe")) {
			return ToolKind.AXE;
		}
		if (id.endsWith("_hoe")) {
			return ToolKind.HOE;
		}
		return null;
	}

	public static ToolTier tierOf(String itemId) {
		String id = Ids.normalize(itemId);
		if (kindOf(id) == null) {
			return null;
		}
		String name = id.substring("minecraft:".length(), id.lastIndexOf('_'));
		return switch (name) {
			case "wooden" -> ToolTier.WOOD;
			case "golden" -> ToolTier.GOLD;
			case "stone" -> ToolTier.STONE;
			case "iron" -> ToolTier.IRON;
			case "diamond" -> ToolTier.DIAMOND;
			case "netherite" -> ToolTier.NETHERITE;
			default -> null;
		};
	}

	public static boolean isTool(String itemId) {
		return kindOf(itemId) != null;
	}

	public static String diamondOf(ToolKind kind) {
		return id(kind, ToolTier.DIAMOND);
	}

	public static List<String> all() {
		List<String> ids = new ArrayList<>();
		for (ToolKind kind : ToolKind.values()) {
			for (ToolTier tier : ToolTier.values()) {
				ids.add(id(kind, tier));
			}
		}
		return ids;
	}
}
