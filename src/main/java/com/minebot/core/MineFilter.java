package com.minebot.core;

import java.util.ArrayList;
import java.util.List;

/** What the job breaks. Bedrock and air are never targets. */
public final class MineFilter {
	public boolean everything = true;
	public List<String> blockIds = new ArrayList<>();
	/** When the filter is a list, also break other blocks that occupy the dig. */
	public boolean digObstructions = true;

	public MineFilter() {}

	public static MineFilter everything() {
		MineFilter filter = new MineFilter();
		filter.everything = true;
		filter.digObstructions = true;
		return filter;
	}

	public static MineFilter only(List<String> ids, boolean digObstructions) {
		MineFilter filter = new MineFilter();
		filter.everything = false;
		filter.digObstructions = digObstructions;
		for (String id : ids) {
			String normalized = Ids.normalize(id);
			if (!normalized.isEmpty()) {
				filter.blockIds.add(normalized);
			}
		}
		return filter;
	}

	public boolean shouldMine(String blockId, boolean obstructing) {
		if (Ids.isAir(blockId) || Ids.isBedrock(blockId)) {
			return false;
		}
		if (everything) {
			return true;
		}
		if (lists(blockId)) {
			return true;
		}
		return obstructing && digObstructions;
	}

	public boolean lists(String blockId) {
		return blockIds.contains(Ids.normalize(blockId));
	}

	public MineFilter copy() {
		MineFilter filter = new MineFilter();
		filter.everything = everything;
		filter.digObstructions = digObstructions;
		filter.blockIds = new ArrayList<>(blockIds);
		return filter;
	}
}
