package com.oritong.no_ultra_dash_please;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DashClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue DASH_SHADER_INTENSITY = BUILDER
            .comment("Dash fullscreen glitch/shake strength in percent. 0 disables the effect; 100 is normal; 200 is double.",
                    "冲刺全屏故障/抖动强度（百分比）：0 关闭，100 默认，200 双倍。支持热重载。")
            .defineInRange("dashShaderIntensity", 100, 0, 200);
    static final ModConfigSpec SPEC = BUILDER.build();

    private DashClientConfig() { }
}
