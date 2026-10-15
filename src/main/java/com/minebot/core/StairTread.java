package com.minebot.core;

/** One step of the clockwise stair. The column under the tread stays solid down to bedrock. */
public final class StairTread {
	public final int x;
	public final int y;
	public final int z;

	public StairTread(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public int headroomY1() {
		return y + 1;
	}

	public int headroomY2() {
		return y + 2;
	}

	/** Solid from the tread down to, but not including, the bedrock layer. */
	public boolean keepsSolid(int blockY, int bedrockY) {
		return blockY <= y && blockY > bedrockY;
	}
}
