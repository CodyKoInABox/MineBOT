package com.minebot.core;

/** Inclusive horizontal rectangle plus the surface Y of each corner. */
public final class Area {
	public BlockPos first;
	public BlockPos second;

	public Area() {}

	public void setFirst(BlockPos pos) {
		this.first = pos == null ? null : pos.copy();
	}

	public void setSecond(BlockPos pos) {
		this.second = pos == null ? null : pos.copy();
	}

	public boolean complete() {
		return first != null && second != null;
	}

	public int minX() {
		requireComplete();
		return Math.min(first.x, second.x);
	}

	public int maxX() {
		requireComplete();
		return Math.max(first.x, second.x);
	}

	public int minZ() {
		requireComplete();
		return Math.min(first.z, second.z);
	}

	public int maxZ() {
		requireComplete();
		return Math.max(first.z, second.z);
	}

	/** Highest corner. The quarry digs down from here. */
	public int surfaceY() {
		requireComplete();
		return Math.max(first.y, second.y);
	}

	public int sizeX() {
		return maxX() - minX() + 1;
	}

	public int sizeZ() {
		return maxZ() - minZ() + 1;
	}

	public Area copy() {
		Area area = new Area();
		area.first = first == null ? null : first.copy();
		area.second = second == null ? null : second.copy();
		return area;
	}

	private void requireComplete() {
		if (!complete()) {
			throw new IllegalStateException("Both corners are required");
		}
	}
}
