package com.minebot.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TorchGridTest {
	@Test
	void gridCoversExampleFootprint() {
		int spacing = 5;
		List<BlockPos> torches = TorchGrid.positions(0, 0, 24, 24, -63, spacing);
		assertTrue(torches.size() > 1);
		for (int x = 0; x <= 24; x++) {
			for (int z = 0; z <= 24; z++) {
				assertTrue(TorchGrid.covers(torches, x, z, spacing), x + "," + z);
			}
		}
	}

	@Test
	void farEdgeIsAnchoredWhenSpacingDoesNotDivide() {
		List<BlockPos> torches = TorchGrid.positions(0, 0, 10, 3, 4, 8);
		boolean sawFarX = false;
		boolean sawFarZ = false;
		for (BlockPos torch : torches) {
			if (torch.x == 10) {
				sawFarX = true;
			}
			if (torch.z == 3) {
				sawFarZ = true;
			}
		}
		assertTrue(sawFarX);
		assertTrue(sawFarZ);
		assertTrue(TorchGrid.covers(torches, 10, 3, 8));
	}
}
