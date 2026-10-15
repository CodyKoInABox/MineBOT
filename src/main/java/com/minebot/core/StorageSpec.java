package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

/** Where mined items go, and which supplies stay in the inventory. */
public final class StorageSpec {
	public int searchRadius = 16;
	public BlockPos markedChest;
	public List<String> depositIds = new ArrayList<>();
	public List<String> keepIds = new ArrayList<>();

	public StorageSpec() {}

	public static StorageSpec withDefaults() {
		StorageSpec spec = new StorageSpec();
		spec.keepIds.add("minecraft:torch");
		spec.keepIds.add("minecraft:soul_torch");
		spec.keepIds.addAll(Ids.FOOD);
		spec.keepIds.addAll(Ids.WOOD);
		spec.keepIds.addAll(Ids.PLANKS);
		spec.keepIds.add("minecraft:stick");
		spec.keepIds.addAll(Ids.IRON);
		spec.keepIds.addAll(ToolItems.all());
		spec.keepIds.add("minecraft:crafting_table");
		spec.keepIds.add("minecraft:netherite_ingot");
		spec.keepIds.add("minecraft:netherite_upgrade_smithing_template");
		return spec;
	}

	public StorageSpec copy() {
		StorageSpec spec = new StorageSpec();
		spec.searchRadius = searchRadius;
		spec.markedChest = markedChest == null ? null : markedChest.copy();
		spec.depositIds = new ArrayList<>(depositIds);
		spec.keepIds = new ArrayList<>(keepIds);
		return spec;
	}
}
