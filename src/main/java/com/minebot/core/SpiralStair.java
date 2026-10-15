package com.minebot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clockwise border spiral. Outer ring first, then the next ring inward.
 * Each 4-adjacent step is one block lower. Headroom is two blocks above the tread.
 */
public final class SpiralStair {
	private SpiralStair() {}

	public static List<StairTread> plan(int minX, int minZ, int maxX, int maxZ, int surfaceY, int floorY) {
		List<StairTread> treads = new ArrayList<>();
		if (minX > maxX || minZ > maxZ || surfaceY < floorY) {
			return treads;
		}
		int y = surfaceY;
		for (int[] cell : spiral(minX, minZ, maxX, maxZ)) {
			if (y < floorY) {
				break;
			}
			treads.add(new StairTread(cell[0], y, cell[1]));
			y--;
		}
		return treads;
	}

	public static List<StairTread> plan(Area area, int surfaceY, int floorY) {
		return plan(area.minX(), area.minZ(), area.maxX(), area.maxZ(), surfaceY, floorY);
	}

	/** Column key to the tread Y that must stay filled down to bedrock. */
	public static Map<Long, Integer> columnTops(List<StairTread> treads) {
		Map<Long, Integer> tops = new HashMap<>();
		for (StairTread tread : treads) {
			tops.put(key(tread.x, tread.z), tread.y);
		}
		return tops;
	}

	public static boolean protectedColumn(Map<Long, Integer> tops, int x, int y, int z, int bedrockY) {
		Integer top = tops.get(key(x, z));
		return top != null && y <= top && y > bedrockY;
	}

	public static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}

	public static boolean onOuterBorder(int x, int z, int minX, int minZ, int maxX, int maxZ) {
		return x == minX || x == maxX || z == minZ || z == maxZ;
	}

	/**
	 * Visits every cell of the rectangle once, 4-connected, clockwise, outer ring first.
	 * Each cell is {@code {x, z}}.
	 */
	public static List<int[]> spiral(int minX, int minZ, int maxX, int maxZ) {
		List<int[]> cells = new ArrayList<>();
		int top = minZ;
		int bottom = maxZ;
		int left = minX;
		int right = maxX;
		while (top <= bottom && left <= right) {
			for (int x = left; x <= right; x++) {
				cells.add(new int[] {x, top});
			}
			top++;
			for (int z = top; z <= bottom; z++) {
				cells.add(new int[] {right, z});
			}
			right--;
			if (top <= bottom) {
				for (int x = right; x >= left; x--) {
					cells.add(new int[] {x, bottom});
				}
				bottom--;
			}
			if (left <= right) {
				for (int z = bottom; z >= top; z--) {
					cells.add(new int[] {left, z});
				}
				left++;
			}
		}
		return cells;
	}
}
