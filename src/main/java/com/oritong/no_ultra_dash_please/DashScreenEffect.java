package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class DashScreenEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double DURATION_SECONDS = 0.40;
    private static long startedNanos;
    private static int dashSeed;
    private static PostChain chain;
    private static RenderTarget target;
    private static int width;
    private static int height;
    private static boolean failed;

    private DashScreenEffect() { }

    public static void start() {
        startedNanos = DashClientConfig.DASH_SHADER_INTENSITY.get() > 0 ? System.nanoTime() : 0;
        dashSeed = (dashSeed + 1) % 4096;
    }

    public static void stop() {
        startedNanos = 0;
    }

    @SubscribeEvent
    static void fieldOfView(ViewportEvent.ComputeFov event) {
        var minecraft = Minecraft.getInstance();
        if (!event.usedConfiguredFov() || !canRender(minecraft) || startedNanos == 0 || failed) {
            return;
        }
        double progress = progress();
        if (progress < 1) {
            double attack = 1 - Math.pow(1 - Math.min(1, progress / 0.12), 4);
            double recovery = Math.pow(1 - Math.max(0, (progress - 0.12) / 0.88), 2);
            event.setFOV(event.getFOV() + 10 * DashClientConfig.DASH_SHADER_INTENSITY.get() / 100.0 * attack * recovery);
        }
    }

    private static double progress() {
        return (System.nanoTime() - startedNanos) / (DURATION_SECONDS * 1_000_000_000.0);
    }

    private static boolean canRender(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null
                && minecraft.screen == null && minecraft.getOverlay() == null && !minecraft.isPaused();
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            close();
            failed = false;
            stop();
        });
    }

    @SubscribeEvent
    static void render(RenderFrameEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !minecraft.player.isAlive()) {
            stop();
            close();
            return;
        }
        if (minecraft.screen != null || minecraft.getOverlay() != null || minecraft.isPaused()) {
            stop();
            return;
        }
        int percent = DashClientConfig.DASH_SHADER_INTENSITY.get();
        if (percent == 0 || startedNanos == 0 || failed) {
            return;
        }
        double progress = progress();
        if (progress >= 1) {
            stop();
            return;
        }
        var main = minecraft.getMainRenderTarget();
        try {
            if (chain == null || target != main) {
                close();
                chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main,
                        NoUltraDashPlease.id("shaders/post/dash_prism.json"));
                target = main;
                width = height = 0;
            }
            if (width != main.width || height != main.height) {
                chain.resize(main.width, main.height);
                width = main.width;
                height = main.height;
            }
            chain.setUniform("Intensity", percent / 100.0F);
            chain.setUniform("Progress", (float)progress);
            chain.setUniform("DashSeed", dashSeed);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            // Own chain, after the frame and before its final blit: preserves vanilla/Iris effects and works with F1.
            chain.process(0);
        } catch (Exception exception) {
            failed = true;
            close();
            LOGGER.error("Could not render the dash shader; disabled until the next resource reload", exception);
        } finally {
            main.bindWrite(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
            target = null;
        }
    }
}
