package com.oritong.no_ultra_dash_please;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared deterministic jitter: one connected floor stays joined and collision never moves. */
public final class CrumbleShake {
    private static final double PIXEL = 1.0 / 16;

    private CrumbleShake() { }

    public static Vec3 offset(long seed, double elapsedTicks) {
        if (elapsedTicks <= 0) return Vec3.ZERO;
        long step = (long) Math.floor(elapsedTicks);
        double blend = Mth.smoothstep(Math.min(1, (elapsedTicks - step) / 0.35));
        double progress = Math.min(1, elapsedTicks / CrumbleGroups.MAX_WARNING_TICKS);
        double amplitude = PIXEL * (0.5 + 0.5 * progress) * Math.min(1, elapsedTicks / 0.5);
        return sample(seed, step - 1).lerp(sample(seed, step), blend).multiply(amplitude, amplitude * 0.2, amplitude);
    }

    private static Vec3 sample(long seed, long step) {
        long value = seed ^ step * 0x9E3779B97F4A7C15L;
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return new Vec3(Math.floorMod(value, 3) - 1, Math.floorMod(value >>> 8, 3) - 1,
                Math.floorMod(value >>> 16, 3) - 1);
    }
}
