package com.minebot.core;

public final class ChestDecision {
	public enum Type {
		NONE,
		DEPOSIT,
		PAUSE
	}

	public final Type type;
	public final String itemId;
	public final int count;
	public final String status;

	private ChestDecision(Type type, String itemId, int count, String status) {
		this.type = type;
		this.itemId = itemId;
		this.count = count;
		this.status = status;
	}

	public static ChestDecision none() {
		return new ChestDecision(Type.NONE, "", 0, "");
	}

	public static ChestDecision deposit(String itemId, int count) {
		return new ChestDecision(Type.DEPOSIT, itemId, count, Status.DEPOSITING);
	}

	public static ChestDecision pause() {
		return new ChestDecision(Type.PAUSE, "", 0, Status.PAUSED_CHESTS_FULL);
	}
}
