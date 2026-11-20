package com.oritong.no_ultra_dash_please;

import net.neoforged.neoforge.common.ModConfigSpec;

final class DashTuning {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec.IntValue CHARGES = BUILDER.comment("Air dashes available before a refill.")
            .defineInRange("charges", 1, 1, 5);
    static final ModConfigSpec.DoubleValue SPEED = BUILDER.comment("Base speed in blocks per tick.")
            .defineInRange("speed", 1.0, 0.35, 2.5);
    static final ModConfigSpec.DoubleValue MAX_MOMENTUM = BUILDER.comment("Maximum carried horizontal speed, in blocks per tick.")
            .defineInRange("maxMomentum", 4.0, 1.0, 8.0);
    static final ModConfigSpec.IntValue DURATION = BUILDER.defineInRange("dashTicks", 3, 1, 10);
    static final ModConfigSpec.IntValue COOLDOWN = BUILDER.defineInRange("dashCooldownTicks", 4, 0, 20);
    static final ModConfigSpec.IntValue REFILL_DELAY = BUILDER.defineInRange("refillDelayTicks", 2, 0, 20);
    static final ModConfigSpec.IntValue GRACE = BUILDER.defineInRange("jumpGraceTicks", 2, 0, 6);
    static final ModConfigSpec.IntValue BUFFER = BUILDER.defineInRange("jumpBufferTicks", 2, 0, 6);
    static final ModConfigSpec.BooleanValue WALL_JUMPS = BUILDER.define("wallJumps", true);
    static final ModConfigSpec.BooleanValue WALL_CLIMBING = BUILDER.comment("Hold use with empty hands to grab and climb walls.")
            .define("wallClimbing", true);
    static final ModConfigSpec.BooleanValue GROUND_REFILL = BUILDER.define("groundRefill", true);
    static final ModConfigSpec SPEC = BUILDER.build();

    private DashTuning() {}
}
