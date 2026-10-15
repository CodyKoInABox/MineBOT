package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpiralStairTest {
	@Test
	void clockwiseConnectedOuterFirstOneDownPerStep() {
		int minX = 0;
		int minZ = 0;
		int maxX = 4;
		int maxZ = 4;
		int surface = 20;
		int floor = 3;
		List<StairTread> treads = SpiralStair.plan(minX, minZ, maxX, maxZ, surface, floor);
		assertEquals(18, treads.size());

		StairTread first = treads.get(0);
		assertEquals(minX, first.x());
		assertEquals(minZ, first.z());
		assertEquals(surface, first.y());
		assertEquals(first.x() + 1, treads.get(1).x());
		assertEquals(first.z(), treads.get(1).z());
		assertEquals(surface - 1, treads.get(1).y());

		for (int i = 1; i < treads.size(); i++) {
			StairTread prev = treads.get(i - 1);
			StairTread next = treads.get(i);
			int dx = Math.abs(next.x() - prev.x());
			int dz = Math.abs(next.z() - prev.z());
			assertEquals(1, dx + dz, "steps stay 4-adjacent");
			assertEquals(-1, next.y() - prev.y());
			assertEquals(next.y() + 1, next.headroomY1());
			assertEquals(next.y() + 2, next.headroomY2());
			assertTrue(next.keepsSolid(next.y(), -64));
			assertTrue(next.keepsSolid(-63, -64));
			assertFalse(next.keepsSolid(-64, -64));
			assertFalse(next.keepsSolid(next.y() + 1, -64));
		}

		int outer = 0;
		for (int i = 0; i < treads.size(); i++) {
			StairTread tread = treads.get(i);
			boolean border = SpiralStair.onOuterBorder(tread.x(), tread.z(), minX, minZ, maxX, maxZ);
			if (i < 16) {
				assertTrue(border, "outer ring before the inner ring");
				outer++;
			}
		}
		assertEquals(16, outer);
		assertFalse(SpiralStair.onOuterBorder(treads.get(16).x(), treads.get(16).z(), minX, minZ, maxX, maxZ));
		assertEquals(1, treads.get(16).x());
		assertEquals(1, treads.get(16).z());
	}

	@Test
	void stairColumnsAreNotMined() {
		TaskConfig config = new TaskConfig();
		config.area.setFirst(new BlockPos(0, 5, 0));
		config.area.setSecond(new BlockPos(2, 5, 2));
		config.depth = DepthSpec.untilY(4);
		config.stair = true;
		// 3×3, two treads: (0,5,0) and (1,4,0). Protected cells inside y=5..4: 3.
		assertEquals(15, QuarryVolume.countMineable(config, -64));
	}
}
