package com.oritong.no_ultra_dash_please;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(NoUltraDashPlease.MOD_ID)
public final class NoUltraDashPlease {
    public static final String MOD_ID = "no_ultra_dash_please";
    public NoUltraDashPlease(IEventBus bus, ModContainer container) {
        DashBlocks.register(bus);
        DashSounds.register(bus);
        bus.addListener(DashNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, DashTuning.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, DashClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
