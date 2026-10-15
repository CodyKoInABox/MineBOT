package com.minebot.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CraftStep {
	public final String outputId;
	public final int outputCount;
	public final Map<String, Integer> ingredients;
	/** {@code inventory}, {@code crafting_table}, or {@code smithing_table}. */
	public final String station;

	public CraftStep(String outputId, int outputCount, Map<String, Integer> ingredients, String station) {
		this.outputId = outputId;
		this.outputCount = outputCount;
		this.ingredients = Map.copyOf(new LinkedHashMap<>(ingredients));
		this.station = station;
	}
}
