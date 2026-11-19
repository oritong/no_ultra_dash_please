package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** One reusable low-poly membrane for empty and occupied green/red boosters. */
@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class BoosterVisuals implements BlockEntityRenderer<BoosterBlock.BubbleEntity> {
    private static final RenderType[] MEMBRANES = {
            material("booster"), material("red_booster"),
            material("booster_occupied"), material("red_booster_occupied")
    };
    private static final RenderType EMPTY_OUTLINE = material("booster_outline");
    private static final Vec3[] UNIT = makeVertices();
    private static final int[][] FACES = makeFaces();
    private static final Map<Integer, Occupant> OCCUPANTS = new HashMap<>();
    private static final Map<Integer, BoosterTravelSound> TRAVEL_SOUNDS = new HashMap<>();
    private static ClientLevel world;

    private BoosterVisuals(BlockEntityRendererProvider.Context context) { }

    private static RenderType material(String name) {
        return RenderType.create("no_ultra_dash_please_" + name,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 4096, false, true,
                RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_TEXT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                NoUltraDashPlease.id("textures/block/" + name + ".png"), false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE).setLightmapState(RenderStateShard.LIGHTMAP)
                        .createCompositeState(false));
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BoosterBlock.ENTITY.get(), BoosterVisuals::new);
    }

    static void receive(DashNetwork.BubbleVisual data) {
        var level = Minecraft.getInstance().level;
        if (world != level) {
            stopTravelSounds();
            OCCUPANTS.clear();
            world = level;
        }
        if (level == null) return;
        if (data.mode() == 0) OCCUPANTS.remove(data.playerId());
        else if (data.mode() <= 4) OCCUPANTS.put(data.playerId(),
                new Occupant(data.mode(), data.origin(), level.getGameTime() + 30));
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (world != level) {
            stopTravelSounds();
            OCCUPANTS.clear();
            world = level;
        }
        if (level != null) OCCUPANTS.entrySet().removeIf(e -> e.getValue().expires < level.getGameTime()
                || !(level.getEntity(e.getKey()) instanceof Player player) || !player.isAlive());
        TRAVEL_SOUNDS.entrySet().removeIf(entry -> {
            Occupant occupant = OCCUPANTS.get(entry.getKey());
            if (occupant != null && occupant.mode == 4 && !entry.getValue().isStopped()) return false;
            entry.getValue().finish();
            Minecraft.getInstance().getSoundManager().stop(entry.getValue());
            return true;
        });
        if (level != null) OCCUPANTS.forEach((id, occupant) -> {
            if (occupant.mode == 4 && !TRAVEL_SOUNDS.containsKey(id) && level.getEntity(id) instanceof Player player) {
                var sound = new BoosterTravelSound(player);
                TRAVEL_SOUNDS.put(id, sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        });
    }

    public static int mode(Player player) {
        Occupant occupant = OCCUPANTS.get(player.getId());
        return player.level() == world && occupant != null ? occupant.mode : 0;
    }

    private static void stopTravelSounds() {
        TRAVEL_SOUNDS.values().forEach(sound -> {
            sound.finish();
            Minecraft.getInstance().getSoundManager().stop(sound);
        });
        TRAVEL_SOUNDS.clear();
    }

    @SubscribeEvent
    static void wrapPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Occupant occupant = OCCUPANTS.get(player.getId());
        if (occupant == null || player.isSpectator() || !player.isAlive()
                || !(player.level() instanceof ClientLevel level) || level != world) return;
        var poses = event.getPoseStack();
        float partial = event.getPartialTick();
        double seconds = ((level.getGameTime() % 120_000L) + partial) / 20;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 feet = player.getPosition(partial);
        double height = Math.max(1.8, player.getBbHeight());
        double radius = Math.max(0.75, player.getBbWidth() * 0.85);
        poses.pushPose();
        poses.translate(0, height * 0.5, 0);
        double pulse = 1 + Math.sin(seconds * Math.PI * 5) * 0.025;
        shell(poses, event.getMultiBufferSource(), camera.subtract(feet.add(0, height * 0.5, 0)),
                occupant.mode % 2 == 0, true, seconds, radius * pulse, height * 0.5 + 0.225, radius * pulse);
        poses.popPose();
    }

    @Override
    public void render(BoosterBlock.BubbleEntity bubble, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var level = bubble.getLevel();
        if (level == null) return;
        // Block state is authoritative for the shared empty marker and its 1 s recovery,
        // including observers who join after the player has already launched.
        var phase = bubble.getBlockState().getValue(BoosterBlock.PHASE);
        if (phase == BoosterBlock.Phase.OCCUPIED) return;
        if (phase == BoosterBlock.Phase.EMPTY || phase == BoosterBlock.Phase.COOLDOWN) {
            outline(poses, buffers);
            return;
        }
        double seconds = ((level.getGameTime() % 120_000L) + partialTick) / 20;
        double seed = Math.floorMod(bubble.getBlockPos().asLong(), 1024) / 1024.0 * Math.PI * 2;
        double pulse = 1 + Math.sin(seconds * Math.PI * 4 + seed) * 0.045;
        double bob = Math.sin(seconds * 2 + seed) * 0.045;
        poses.pushPose();
        poses.translate(0.5, 0.5 + bob, 0.5);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .subtract(Vec3.atCenterOf(bubble.getBlockPos()).add(0, bob, 0));
        shell(poses, buffers, camera, BoosterBlock.kind(bubble.getBlockState()) == DashBlocks.Kind.RED_BOOSTER,
                false, seconds, 0.4375 * pulse, 0.4375 / pulse, 0.4375 * pulse);
        poses.popPose();
    }

    private static void outline(PoseStack poses, MultiBufferSource buffers) {
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        // A billboard preserves the original circular marker from above, below and at oblique angles.
        poses.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        VertexConsumer out = buffers.getBuffer(EMPTY_OUTLINE);
        var pose = poses.last();
        out.addVertex(pose, -0.5F, 0.5F, 0).setColor(255, 255, 255, 255).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(pose, -0.5F, -0.5F, 0).setColor(255, 255, 255, 255).setUv(0, 1).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(pose, 0.5F, -0.5F, 0).setColor(255, 255, 255, 255).setUv(1, 1).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(pose, 0.5F, 0.5F, 0).setColor(255, 255, 255, 255).setUv(1, 0).setLight(LightTexture.FULL_BRIGHT);
        poses.popPose();
    }

    private static void shell(PoseStack poses, MultiBufferSource buffers, Vec3 camera, boolean red,
                              boolean occupied, double seconds, double rx, double ry, double rz) {
        VertexConsumer out = buffers.getBuffer(MEMBRANES[(occupied ? 2 : 0) + (red ? 1 : 0)]);
        var pose = poses.last();
        int count = occupied ? 4 : 5;
        int frame = (int) Math.floor(seconds * 10) % count;
        // Front-side shell only: avoid drawing a second translucent back surface over the player's skin.
        for (int[] face : FACES) {
            Vec3 centroid = Vec3.ZERO;
            for (int index : face) centroid = centroid.add(UNIT[index]);
            centroid = centroid.scale(1.0 / face.length);
            Vec3 center = new Vec3(centroid.x * rx, centroid.y * ry, centroid.z * rz);
            Vec3 a = scaled(UNIT[face[0]], rx, ry, rz);
            Vec3 b = scaled(UNIT[face[1]], rx, ry, rz);
            Vec3 c = scaled(UNIT[face[2]], rx, ry, rz);
            Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
            if (normal.dot(camera.subtract(center)) <= 0) continue;
            double rim = 1 - Math.max(0, normal.dot(camera.subtract(center).normalize()));
            int axis = Math.abs(centroid.x) > Math.abs(centroid.y) ? 0 : 1;
            if (Math.abs(centroid.z) > Math.abs(axis == 0 ? centroid.x : centroid.y)) axis = 2;
            for (int i = 0; i < 4; i++) {
                Vec3 unit = UNIT[face[Math.min(i, face.length - 1)]];
                Vec3 point = new Vec3(unit.x * rx, unit.y * ry, unit.z * rz);
                // The source art has a bright rim. Sample its highlight for grazing faces,
                // and the native pixel face projection elsewhere; do not smooth or resample.
                float u = rim > 0.60 ? 8.5F : (float) (8 + (axis == 0 ? unit.z : unit.x) * 7);
                float v = rim > 0.60 ? 2.5F : (float) (8 - (axis == 1 ? unit.z : unit.y) * 7);
                out.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                        .setColor(255, 255, 255, 255).setUv(u / 16, (frame * 16 + v) / (count * 16))
                        .setLight(LightTexture.FULL_BRIGHT);
            }
        }
    }

    private static Vec3 scaled(Vec3 unit, double rx, double ry, double rz) {
        return new Vec3(unit.x * rx, unit.y * ry, unit.z * rz);
    }

    private static Vec3[] makeVertices() {
        Vec3[] vertices = new Vec3[42];
        vertices[0] = new Vec3(0, 1, 0);
        vertices[1] = new Vec3(0, -1, 0);
        for (int ring = 1; ring < 6; ring++) {
            double phi = Math.PI * ring / 6;
            for (int i = 0; i < 8; i++) {
                double theta = Math.PI * i / 4;
                vertices[2 + (ring - 1) * 8 + i] = new Vec3(Math.sin(phi) * Math.cos(theta),
                        Math.cos(phi), Math.sin(phi) * Math.sin(theta));
            }
        }
        return vertices;
    }

    private static int[][] makeFaces() {
        int[][] faces = new int[48][];
        int at = 0;
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            faces[at++] = new int[] {0, 2 + j, 2 + i};
            for (int ring = 0; ring < 4; ring++) faces[at++] =
                    new int[] {2 + ring * 8 + i, 2 + ring * 8 + j, 10 + ring * 8 + j, 10 + ring * 8 + i};
            faces[at++] = new int[] {1, 34 + i, 34 + j};
        }
        return faces;
    }

    private record Occupant(int mode, BlockPos origin, long expires) { }
}
