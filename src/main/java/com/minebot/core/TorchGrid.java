package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Torches on a grid of {@code spacing} blocks, including the far edges.
 * A cell is covered when its Chebyshev distance to a torch is within {@code spacing}.
 */
public final class TorchGrid {
	private TorchGrid() {}

	public static List<BlockPos> positions(int minX, int minZ, int maxX, int maxZ, int y, int spacing) {
		int step = Math.max(1, spacing);
		List<Integer> xs = axis(minX, maxX, step);
		List<Integer> zs = axis(minZ, maxZ, step);
		List<BlockPos> out = new ArrayList<>(xs.size() * zs.size());
		for (int x : xs) {
			for (int z : zs) {
				out.add(new BlockPos(x, y, z));
			}
		}
		return out;
	}

	public static boolean covers(List<BlockPos> torches, int x, int z, int spacing) {
		int limit = Math.max(1, spacing);
		for (BlockPos torch : torches) {
			int distance = Math.max(Math.abs(torch.x - x), Math.abs(torch.z - z));
			if (distance <= limit) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAnchor(int x, int z, int minX, int minZ, int maxX, int maxZ, int spacing) {
		for (BlockPos torch : positions(minX, minZ, maxX, maxZ, 0, spacing)) {
			if (torch.x == x && torch.z == z) {
				return true;
			}
		}
		return false;
	}

	private static List<Integer> axis(int min, int max, int spacing) {
		List<Integer> values = new ArrayList<>();
		for (int i = min; i <= max; i += spacing) {
			values.add(i);
		}
		if (values.isEmpty() || values.get(values.size() - 1) != max) {
			values.add(max);
		}
		return values;
	}
}
