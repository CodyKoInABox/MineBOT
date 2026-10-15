package com.minebot.core;

public final class SurvivalSpec {
	public boolean eat = true;
	public boolean sleep = true;
	public boolean placeTorches = true;
	public boolean autoCraftPicks = true;
	public int torchSpacing = 8;
	/** Eat when hunger is at or below this. Vanilla hunger runs 0 through 20. */
	public int hungerThreshold = 6;

	public SurvivalSpec() {}

	public static SurvivalSpec defaults() {
		return new SurvivalSpec();
	}

	public SurvivalSpec copy() {
		SurvivalSpec spec = new SurvivalSpec();
		spec.eat = eat;
		spec.sleep = sleep;
		spec.placeTorches = placeTorches;
		spec.autoCraftPicks = autoCraftPicks;
		spec.torchSpacing = torchSpacing;
		spec.hungerThreshold = hungerThreshold;
		return spec;
	}
}
