package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Replace the world with Celeste's crisp, dark dream space while preserving the HUD. */
@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class DreamTravelScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static PostChain chain;
    private static RenderTarget target;
    private static int width;
    private static int height;
    private static boolean failed;
    private static boolean wasDreaming;
    private static boolean drawn;
    private static long entered;
    private static long exited;
    private static float amount;
    private static Vec3 travel = Vec3.ZERO;

    private DreamTravelScreen() { }

    public static boolean active() { return !failed && amount > 0; }

    @SubscribeEvent
    static void frameStart(RenderFrameEvent.Pre event) {
        drawn = false;
        var minecraft = Minecraft.getInstance();
        if (!canRender(minecraft)) {
            wasDreaming = false;
            amount = 0;
            entered = exited = 0;
            if (minecraft.level == null) close();
            return;
        }
        long now = System.nanoTime();
        boolean dreaming = DashClient.dreaming(minecraft.player);
        if (dreaming && !wasDreaming) {
            entered = now;
            exited = 0;
        } else if (!dreaming && wasDreaming) {
            exited = now;
        }
        wasDreaming = dreaming;
        if (dreaming) {
            amount = Math.min(1, (now - entered) / 60_000_000.0F);
            travel = DashClient.dreamSpeed(minecraft.player).normalize();
        } else {
            amount = exited == 0 ? 0 : Math.max(0, 1 - (now - exited) / 100_000_000.0F);
        }
    }

    private static boolean canRender(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null
                && minecraft.screen == null && minecraft.getOverlay() == null && !minecraft.isPaused();
    }

    @SubscribeEvent
    static void beforeHud(RenderGuiEvent.Pre event) { render(); }

    @SubscribeEvent
    static void withoutHud(RenderFrameEvent.Post event) {
        // F1 and alternate HUD renderers can skip RenderGuiEvent entirely.
        if (!drawn) render();
    }

    private static void render() {
        var minecraft = Minecraft.getInstance();
        if (drawn || !active() || !canRender(minecraft)) return;
        var main = minecraft.getMainRenderTarget();
        try {
            if (chain == null || target != main) {
                close();
                chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main,
                        NoUltraDashPlease.id("shaders/post/dream_travel.json"));
                target = main;
                width = height = 0;
            }
            if (width != main.width || height != main.height) {
                chain.resize(main.width, main.height);
                width = main.width;
                height = main.height;
            }
            Vector3f motion = new Vector3f((float) travel.x, (float) travel.y, (float) travel.z)
                    .rotate(new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation()).conjugate());
            chain.setUniform("Amount", amount);
            chain.setUniform("TravelTime", (System.nanoTime() - entered) / 1_000_000_000.0F);
            chain.setUniform("TravelX", motion.x);
            chain.setUniform("TravelY", motion.y);
            chain.setUniform("TravelZ", motion.z);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(0);
            drawn = true;
        } catch (Exception exception) {
            failed = true;
            close();
            LOGGER.error("Could not render the dream travel screen; disabled until resource reload", exception);
        } finally {
            main.bindWrite(true);
            RenderSystem.enableDepthTest();
        }
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            close();
            failed = false;
            wasDreaming = false;
            amount = 0;
            entered = exited = 0;
        });
    }

    private static void close() {
        if (chain != null) chain.close();
        chain = null;
        target = null;
    }
}
