package com.minebot.core;

public enum PickTier {
	WOOD("minecraft:wooden_pickaxe"),
	STONE("minecraft:stone_pickaxe"),
	IRON("minecraft:iron_pickaxe"),
	DIAMOND("minecraft:diamond_pickaxe"),
	NETHERITE("minecraft:netherite_pickaxe");

	public final String itemId;

	PickTier(String itemId) {
		this.itemId = itemId;
	}

	public static PickTier fromItem(String itemId) {
		if (itemId == null) {
			return null;
		}
		for (PickTier tier : values()) {
			if (tier.itemId.equals(Ids.normalize(itemId))) {
				return tier;
			}
		}
		return null;
	}
}
