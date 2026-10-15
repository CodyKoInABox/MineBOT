package com.minebot.core;

/**
 * How far down a job digs.
 * Bedrock is never included: the floor is clamped to {@code bedrockY + 1}.
 */
public final class DepthSpec {
	public enum Kind {
		BEDROCK,
		UNTIL_Y,
		LAYERS
	}

	public Kind kind = Kind.BEDROCK;
	/** Inclusive target Y for {@link Kind#UNTIL_Y}. */
	public int y;
	/** Layer count for {@link Kind#LAYERS}, including the surface layer. */
	public int layers = 1;

	public DepthSpec() {}

	public static DepthSpec bedrock() {
		DepthSpec spec = new DepthSpec();
		spec.kind = Kind.BEDROCK;
		return spec;
	}

	public static DepthSpec untilY(int y) {
		DepthSpec spec = new DepthSpec();
		spec.kind = Kind.UNTIL_Y;
		spec.y = y;
		return spec;
	}

	public static DepthSpec layers(int layers) {
		DepthSpec spec = new DepthSpec();
		spec.kind = Kind.LAYERS;
		spec.layers = Math.max(1, layers);
		return spec;
	}

	/** Lowest Y the job may break, inclusive, never a bedrock layer. */
	public int floorInclusive(int surfaceY, int bedrockY) {
		int aboveBedrock = bedrockY + 1;
		int floor = switch (kind) {
			case BEDROCK -> aboveBedrock;
			case UNTIL_Y -> y;
			case LAYERS -> surfaceY - Math.max(1, layers) + 1;
		};
		if (floor < aboveBedrock) {
			floor = aboveBedrock;
		}
		if (floor > surfaceY) {
			floor = surfaceY;
		}
		return floor;
	}

	public DepthSpec copy() {
		DepthSpec spec = new DepthSpec();
		spec.kind = kind;
		spec.y = y;
		spec.layers = layers;
		return spec;
	}
}
