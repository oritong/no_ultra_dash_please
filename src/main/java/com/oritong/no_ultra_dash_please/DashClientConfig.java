package com.oritong.no_ultra_dash_please;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DashClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue DASH_SHADER_INTENSITY = BUILDER
            .comment("Prism dash refraction, peripheral light streaks and temporary FOV pulse, in percent. 0 disables; 100 normal; 200 double.",
                    "冲刺棱镜折射、外围流光和瞬时视野脉冲强度（百分比）：0 关闭，100 默认，200 双倍。支持热重载。")
            .defineInRange("dashShaderIntensity", 100, 0, 200);
    static final ModConfigSpec SPEC = BUILDER.build();

    private DashClientConfig() { }
}
