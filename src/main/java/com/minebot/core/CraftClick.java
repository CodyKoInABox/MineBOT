package com.minebot.core;

/** One inventory click. The adapter performs exactly one of these per tick. */
public final class CraftClick {
	public enum Kind {
		/** Put one of {@link #itemId} into crafting-grid index {@link #gridIndex}. */
		GRID,
		TAKE_RESULT,
		OPEN_INVENTORY,
		PLACE_TABLE,
		OPEN_TABLE,
		OPEN_SMITHING
	}

	public Kind kind = Kind.GRID;
	public String itemId = "";
	public int gridIndex;
	public int x;
	public int y;
	public int z;

	public CraftClick() {}

	public CraftClick(Kind kind, String itemId, int gridIndex) {
		this.kind = kind;
		this.itemId = itemId == null ? "" : itemId;
		this.gridIndex = gridIndex;
	}

	public static CraftClick grid(String itemId, int gridIndex) {
		return new CraftClick(Kind.GRID, itemId, gridIndex);
	}

	public static CraftClick takeResult() {
		return new CraftClick(Kind.TAKE_RESULT, "", 0);
	}

	public static CraftClick openInventory() {
		return new CraftClick(Kind.OPEN_INVENTORY, "", 0);
	}

	public static CraftClick placeTable(int x, int y, int z) {
		CraftClick click = new CraftClick(Kind.PLACE_TABLE, "minecraft:crafting_table", 0);
		click.x = x;
		click.y = y;
		click.z = z;
		return click;
	}

	public static CraftClick openTable(int x, int y, int z) {
		CraftClick click = new CraftClick(Kind.OPEN_TABLE, "minecraft:crafting_table", 0);
		click.x = x;
		click.y = y;
		click.z = z;
		return click;
	}

	public static CraftClick openSmithing(int x, int y, int z) {
		CraftClick click = new CraftClick(Kind.OPEN_SMITHING, "minecraft:smithing_table", 0);
		click.x = x;
		click.y = y;
		click.z = z;
		return click;
	}

	public CraftClick copy() {
		CraftClick click = new CraftClick(kind, itemId, gridIndex);
		click.x = x;
		click.y = y;
		click.z = z;
		return click;
	}
}
