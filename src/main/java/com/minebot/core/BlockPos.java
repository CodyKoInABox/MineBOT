package com.minebot.core;

/** Integer block coordinate. No Minecraft types. */
public final class BlockPos {
	public int x;
	public int y;
	public int z;

	public BlockPos() {}

	public BlockPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockPos copy() {
		return new BlockPos(x, y, z);
	}

	public int manhattan(BlockPos other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BlockPos other)) {
			return false;
		}
		return x == other.x && y == other.y && z == other.z;
	}

	@Override
	public int hashCode() {
		return (x * 73428767) ^ (y * 912931) ^ z;
	}

	@Override
	public String toString() {
		return x + "," + y + "," + z;
	}
}
