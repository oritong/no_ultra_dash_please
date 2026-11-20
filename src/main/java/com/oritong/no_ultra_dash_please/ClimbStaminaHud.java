package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;

/** A small green stamina wheel surrounding the crosshair; full stamina leaves no HUD. */
@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class ClimbStaminaHud {
    private static final int SEGMENTS = 128;

    private ClimbStaminaHud() { }

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, NoUltraDashPlease.id("climb_stamina"),
                (graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !player.isAlive() || player.isSpectator() || minecraft.screen != null
                || minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson()) return;
        float stamina = DashClient.stamina(player);
        // Show depleted stamina after letting go too, until the server actually refills it.
        // Never leave a full background ring behind when landing or touching a refill.
        if (stamina >= 110) return;
        float remaining = stamina / 110;
        float centerX = graphics.guiWidth() / 2;
        float centerY = graphics.guiHeight() / 2;
        arc(graphics, centerX, centerY + 0.5F, 14, 21, 1, 0xA5101A16);
        arc(graphics, centerX, centerY, 14.8F, 20.2F, 1, 0xFF847958);
        arc(graphics, centerX, centerY, 15.6F, 19.4F, 1, 0xFF253B2B);
        int color = remaining > 0.3F ? 0xFF6DE83A : remaining > 0.15F ? 0xFFF4CC43 : 0xFFF05B46;
        if (remaining <= 0.15F) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.nanoTime() / 130_000_000.0));
            color = Mth.color(0.74F + 0.20F * pulse, 0.18F + 0.17F * pulse, 0.12F + 0.13F * pulse) | 0xFF000000;
        }
        arc(graphics, centerX, centerY, 15.6F, 19.4F, remaining, color);
        arc(graphics, centerX, centerY, 18.7F, 19.4F, remaining, 0x60FFFFFF);
    }

    /** Clockwise arc beginning at twelve o'clock, with a clear center for the crosshair. */
    private static void arc(GuiGraphics graphics, float x, float y, float inner, float outer, float amount, int color) {
        if (amount <= 0) return;
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var matrix = graphics.pose().last().pose();
        int count = Mth.ceil(amount * SEGMENTS);
        for (int index = 0; index < count; index++) {
            double start = Math.PI * 2 * index / SEGMENTS - Math.PI / 2;
            double end = Math.PI * 2 * Math.min(amount, (index + 1.0) / SEGMENTS) - Math.PI / 2;
            vertex(vertices, matrix, x, y, outer, start, color);
            vertex(vertices, matrix, x, y, inner, start, color);
            vertex(vertices, matrix, x, y, inner, end, color);
            vertex(vertices, matrix, x, y, outer, end, color);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float radius,
                               double angle, int color) {
        vertices.addVertex(matrix, x + (float) Math.cos(angle) * radius,
                y + (float) Math.sin(angle) * radius, 0).setColor(color);
    }
}
