package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SurvivalPolicyTest {
	@Test
	void eatsAtThresholdOnly() {
		assertTrue(SurvivalPolicy.shouldEat(6, 6, true, true));
		assertTrue(SurvivalPolicy.shouldEat(0, 6, true, true));
		assertFalse(SurvivalPolicy.shouldEat(7, 6, true, true));
		assertFalse(SurvivalPolicy.shouldEat(3, 6, false, true));
		assertFalse(SurvivalPolicy.shouldEat(3, 6, true, false));
	}

	@Test
	void sleepsOnlyAtNightWhenABedIsNearby() {
		assertTrue(SurvivalPolicy.shouldSleep(true, true, 18000));
		assertTrue(SurvivalPolicy.shouldSleep(true, true, 12542));
		assertTrue(SurvivalPolicy.shouldSleep(true, true, 23459));
		assertFalse(SurvivalPolicy.shouldSleep(true, true, 12541));
		assertFalse(SurvivalPolicy.shouldSleep(true, true, 23460));
		assertFalse(SurvivalPolicy.shouldSleep(true, true, 1000));
		assertFalse(SurvivalPolicy.shouldSleep(true, true, 24000 + 1000));
		assertTrue(SurvivalPolicy.shouldSleep(true, true, 24000 + 18000));
		assertFalse(SurvivalPolicy.shouldSleep(true, false, 18000));
		assertFalse(SurvivalPolicy.shouldSleep(false, true, 18000));
	}
}
