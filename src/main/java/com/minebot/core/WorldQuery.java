package com.minebot.core;

/** Read-only world the state machine can ask about. The adapter fills this from the client world. */
public interface WorldQuery extends Pathfinder.Collision {
	String blockId(int x, int y, int z);

	float hardness(int x, int y, int z);

	boolean solid(int x, int y, int z);

	default boolean canHarvest(String toolId, int x, int y, int z) {
		return ToolRules.canHarvest(toolId, blockId(x, y, z));
	}

	default float toolSpeed(String toolId) {
		return BreakTimes.speedFor(toolId);
	}

	default float toolSpeed(String toolId, int x, int y, int z) {
		return ToolRules.speedAgainst(toolId, blockId(x, y, z));
	}

	WorldQuery AIR = new WorldQuery() {
		@Override
		public String blockId(int x, int y, int z) {
			return "minecraft:air";
		}

		@Override
		public float hardness(int x, int y, int z) {
			return 0f;
		}

		@Override
		public boolean solid(int x, int y, int z) {
			return false;
		}
	};

	/** Flat stone floor under y, stone occupying exactly {@code fillY}. */
	static WorldQuery stoneFill(int fillY) {
		return new WorldQuery() {
			@Override
			public String blockId(int x, int y, int z) {
				if (y == fillY) {
					return "minecraft:stone";
				}
				if (y < fillY) {
					return "minecraft:stone";
				}
				return "minecraft:air";
			}

			@Override
			public float hardness(int x, int y, int z) {
				return y <= fillY ? 1.5f : 0f;
			}

			@Override
			public boolean solid(int x, int y, int z) {
				return y <= fillY;
			}
		};
	}
}
