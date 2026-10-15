package com.minebot.core;

/** One client action. The adapter performs it and then waits for the next tick. */
public final class BotAction {
	public final ActionType type;
	public final int x;
	public final int y;
	public final int z;
	public final float yaw;
	public final float pitch;
	public final String itemId;
	public final String status;
	public final boolean breakStart;
	public final CraftClick craft;

	private BotAction(
			ActionType type,
			int x,
			int y,
			int z,
			float yaw,
			float pitch,
			String itemId,
			String status,
			boolean breakStart,
			CraftClick craft
	) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.itemId = itemId == null ? "" : itemId;
		this.status = status == null ? "" : status;
		this.breakStart = breakStart;
		this.craft = craft;
	}

	public static BotAction idle(String status) {
		return new BotAction(ActionType.IDLE, 0, 0, 0, 0, 0, "", status, false, null);
	}

	public static BotAction pause(String status) {
		return new BotAction(ActionType.PAUSE, 0, 0, 0, 0, 0, "", status, false, null);
	}

	public static BotAction done(String status) {
		return new BotAction(ActionType.DONE, 0, 0, 0, 0, 0, "", status, false, null);
	}

	public static BotAction look(float yaw, float pitch, String status) {
		return new BotAction(ActionType.LOOK, 0, 0, 0, yaw, pitch, "", status, false, null);
	}

	public static BotAction walk(int x, int y, int z, float yaw, float pitch, String status) {
		return new BotAction(ActionType.WALK, x, y, z, yaw, pitch, "", status, false, null);
	}

	public static BotAction jump(int x, int y, int z, float yaw, float pitch, String status) {
		return new BotAction(ActionType.JUMP, x, y, z, yaw, pitch, "", status, false, null);
	}

	public static BotAction breakBlock(int x, int y, int z, float yaw, float pitch, boolean start, String status) {
		return new BotAction(ActionType.BREAK, x, y, z, yaw, pitch, "", status, start, null);
	}

	public static BotAction place(int x, int y, int z, String itemId, String status) {
		return new BotAction(ActionType.PLACE, x, y, z, 0, 0, itemId, status, false, null);
	}

	public static BotAction use(int x, int y, int z, String itemId, String status) {
		return new BotAction(ActionType.USE, x, y, z, 0, 0, itemId, status, false, null);
	}

	public static BotAction clickChest(int x, int y, int z, String itemId, String status) {
		return new BotAction(ActionType.CLICK_CHEST, x, y, z, 0, 0, itemId, status, false, null);
	}

	public static BotAction eat(String itemId, String status) {
		return new BotAction(ActionType.EAT, 0, 0, 0, 0, 0, itemId, status, false, null);
	}

	public static BotAction sleep(int x, int y, int z, String status) {
		return new BotAction(ActionType.SLEEP, x, y, z, 0, 0, "minecraft:red_bed", status, false, null);
	}

	public static BotAction craft(CraftClick click, String status) {
		int x = click == null ? 0 : click.x;
		int y = click == null ? 0 : click.y;
		int z = click == null ? 0 : click.z;
		String item = click == null ? "" : click.itemId;
		return new BotAction(ActionType.CRAFT, x, y, z, 0, 0, item, status, false, click);
	}

	public BotAction withAim(float yaw, float pitch) {
		return new BotAction(type, x, y, z, yaw, pitch, itemId, status, breakStart, craft);
	}
}
