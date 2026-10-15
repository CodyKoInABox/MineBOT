package com.minebot.core;

import java.util.Map;

/**
 * Which tool to hold for a block, and how fast that tool breaks it.
 * Sets match the vanilla 26.3 mineable tags.
 */
public final class ToolRules {
	/** Craft a replacement when the chosen tool has this many uses left, or fewer. */
	public static final int LOW_USES = 10;

	private ToolRules() {}

	public static ToolKind kindFor(String blockId) {
		String id = Ids.normalize(blockId);
		if (ToolCatalog.shovel(id)) {
			return ToolKind.SHOVEL;
		}
		if (ToolCatalog.hoe(id)) {
			return ToolKind.HOE;
		}
		if (ToolCatalog.axe(id)) {
			return ToolKind.AXE;
		}
		if (ToolCatalog.pickaxe(id)) {
			return ToolKind.PICKAXE;
		}
		return null;
	}

	/** Lowest tier that harvests {@code blockId}. Wood also covers blocks that drop for an empty hand. */
	public static ToolTier minimumTier(String blockId) {
		String id = Ids.normalize(blockId);
		if (ToolCatalog.needsDiamond(id)) {
			return ToolTier.DIAMOND;
		}
		if (ToolCatalog.needsIron(id)) {
			return ToolTier.IRON;
		}
		if (ToolCatalog.needsStone(id)) {
			return ToolTier.STONE;
		}
		return ToolTier.WOOD;
	}

	/**
	 * Best tool already in the inventory for this block.
	 * Empty string means use the empty hand: nothing of the right kind can harvest it.
	 */
	public static String choose(String blockId, Map<String, Integer> inventory, String heldId) {
		ToolKind kind = kindFor(blockId);
		if (kind == null) {
			return "";
		}
		ToolTier minimum = minimumTier(blockId);
		Choice held = consider("", -1, heldId, kind, minimum);
		String bestId = held.id;
		int bestRank = held.rank;
		if (inventory != null) {
			for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
				if (entry.getValue() == null || entry.getValue() <= 0) {
					continue;
				}
				Choice next = consider(bestId, bestRank, entry.getKey(), kind, minimum);
				bestId = next.id;
				bestRank = next.rank;
			}
		}
		return bestId;
	}

	public static float speedAgainst(String toolId, String blockId) {
		ToolKind need = kindFor(blockId);
		ToolKind have = ToolItems.kindOf(toolId);
		if (need == null || have != need) {
			return 1f;
		}
		ToolTier tier = ToolItems.tierOf(toolId);
		return tier == null ? 1f : tier.speed;
	}

	public static boolean canHarvest(String toolId, String blockId) {
		if (Ids.isBedrock(blockId)) {
			return false;
		}
		ToolKind need = kindFor(blockId);
		if (need == null) {
			return true;
		}
		ToolTier tier = ToolItems.tierOf(toolId);
		boolean rightKind = tier != null && ToolItems.kindOf(toolId) == need;
		if (rightKind && tier.harvestLevel >= minimumTier(blockId).harvestLevel) {
			return true;
		}
		// Shovel, axe, and hoe blocks still drop with an empty hand. Pickaxe blocks do not.
		return need != ToolKind.PICKAXE;
	}

	public static boolean healthy(String itemId, Map<String, Integer> remainingUses) {
		if (remainingUses == null || itemId == null || itemId.isEmpty()) {
			return true;
		}
		Integer left = remainingUses.get(Ids.normalize(itemId));
		if (left == null) {
			return true;
		}
		return left > LOW_USES;
	}

	private static Choice consider(String bestId, int bestRank, String candidate, ToolKind kind, ToolTier minimum) {
		ToolTier tier = ToolItems.tierOf(candidate);
		if (tier == null || ToolItems.kindOf(candidate) != kind) {
			return new Choice(bestId, bestRank);
		}
		if (tier.harvestLevel < minimum.harvestLevel) {
			return new Choice(bestId, bestRank);
		}
		if (tier.rank > bestRank) {
			return new Choice(Ids.normalize(candidate), tier.rank);
		}
		return new Choice(bestId, bestRank);
	}

	private record Choice(String id, int rank) {}
}
