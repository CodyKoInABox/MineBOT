package com.minebot.core;

/** Yaw and pitch from an eye position toward a block center. Yaw matches vanilla: 0 looks +Z. */
public final class Aim {
	public final float yaw;
	public final float pitch;

	public Aim(float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public static Aim at(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
		double dx = (blockX + 0.5) - eyeX;
		double dy = (blockY + 0.5) - eyeY;
		double dz = (blockZ + 0.5) - eyeZ;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
		return new Aim(yaw, pitch);
	}

	public static Aim atFeet(int feetX, int feetY, int feetZ, int blockX, int blockY, int blockZ) {
		return at(feetX + 0.5, feetY + 1.62, feetZ + 0.5, blockX, blockY, blockZ);
	}
}
