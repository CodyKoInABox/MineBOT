package com.minebot.core;

/** Eat and sleep decisions. Night matches vanilla: tick 12542 through 23459 of the 24000-tick day. */
public final class SurvivalPolicy {
	public static final long DAY_LENGTH = 24000L;
	public static final long NIGHT_START = 12542L;
	public static final long NIGHT_END = 23460L;
	/** Holding use on food. 32 ticks is the vanilla eat duration. */
	public static final int EAT_TICKS = 32;

	private SurvivalPolicy() {}

	public static boolean shouldEat(int hunger, int hungerThreshold, boolean eatEnabled, boolean hasFood) {
		return eatEnabled && hasFood && hunger <= hungerThreshold;
	}

	public static boolean isNight(long dayTime) {
		long time = Math.floorMod(dayTime, DAY_LENGTH);
		return time >= NIGHT_START && time < NIGHT_END;
	}

	public static boolean shouldSleep(boolean sleepEnabled, boolean bedNearby, long dayTime) {
		return sleepEnabled && bedNearby && isNight(dayTime);
	}
}
