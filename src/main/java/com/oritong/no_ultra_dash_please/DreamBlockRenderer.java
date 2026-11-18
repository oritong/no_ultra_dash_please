package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Black connected surfaces with Celeste-style camera parallax, twinkles and wavy white edges. */
@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class DreamBlockRenderer implements BlockEntityRenderer<DreamBlock.DreamEntity> {
    private static final ResourceLocation PARTICLES = NoUltraDashPlease.id("textures/block/dream_block_particles.png");
    private static final RenderType SURFACE = SurfaceType.createSurface();
    private static final RenderType EDGE = EdgeType.createEdge();
    private static final int[][] COLORS = {
            {0xFFEF11, 0xFF00D0, 0x08A310},
            {0x5FCDE4, 0x7FB25E, 0xE0564C},
            {0x5B6EE1, 0xCC3B3B, 0x7DAA64}
    };
    private static final double[] PARALLAX = {0.30, 0.55, 0.80};
    private static final double PIXEL = 1.0 / 16;
    private static final double STAR_RADIUS = 2.5 * PIXEL;
    private static final double CELL = 0.5;
    // The particle mask has one opaque white sampling pixel for the black backing quad.
    private static final float WHITE_UV = 15.5F / 16;
    // U x V points inward; the quad order below supplies the outward normal.
    private static final Face[] FACES = {
            new Face(Direction.NORTH, Direction.EAST, Direction.UP, 0, 0, 0),
            new Face(Direction.SOUTH, Direction.WEST, Direction.UP, 1, 0, 1),
            new Face(Direction.WEST, Direction.NORTH, Direction.UP, 0, 0, 1),
            new Face(Direction.EAST, Direction.SOUTH, Direction.UP, 1, 0, 0),
            new Face(Direction.UP, Direction.EAST, Direction.SOUTH, 0, 1, 0),
            new Face(Direction.DOWN, Direction.EAST, Direction.NORTH, 0, 0, 1)
    };

    private DreamBlockRenderer(BlockEntityRendererProvider.Context context) { }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DreamBlock.ENTITY.get(), DreamBlockRenderer::new);
    }

    @Override
    public void render(DreamBlock.DreamEntity block, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var level = block.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = block.getBlockPos();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double seconds = ((level.getGameTime() % 120_000L) + partialTick) / 20.0;
        var vertices = buffers.getBuffer(SURFACE);
        var pose = poses.last();
        for (Face face : FACES) {
            Direction normal = face.normal;
            double facing = (camera.x - pos.getX() - 0.5) * normal.getStepX()
                    + (camera.y - pos.getY() - 0.5) * normal.getStepY()
                    + (camera.z - pos.getZ() - 0.5) * normal.getStepZ();
            if (facing <= 0.001 || !exposed(level, pos, normal)) {
                continue;
            }
            boolean left = !connected(level, pos, normal, face.u.getOpposite());
            boolean right = !connected(level, pos, normal, face.u);
            boolean bottom = !connected(level, pos, normal, face.v.getOpposite());
            boolean top = !connected(level, pos, normal, face.v);
            // Background lies precisely on the cube; overlays have one shared outward offset.
            rect(vertices, pose, face, 0, 0, 1, 1, 0, 0x000000,
                    WHITE_UV, WHITE_UV, WHITE_UV, WHITE_UV);
            double minU = left ? 2 * PIXEL : 0, maxU = right ? 1 - 2 * PIXEL : 1;
            double minV = bottom ? 2 * PIXEL : 0, maxV = top ? 1 - 2 * PIXEL : 1;
            stars(vertices, pose, face, pos, camera, seconds, minU, minV, maxU, maxV);
            VertexConsumer edges = buffers.getBuffer(EDGE);
            if (left) edge(edges, pose, face, pos, seconds, 0, bottom, top);
            if (right) edge(edges, pose, face, pos, seconds, 1, bottom, top);
            if (bottom) edge(edges, pose, face, pos, seconds, 2, left, right);
            if (top) edge(edges, pose, face, pos, seconds, 3, left, right);
        }
    }

    private static boolean exposed(BlockGetter level, BlockPos pos, Direction face) {
        BlockPos other = pos.relative(face);
        var neighbor = level.getBlockState(other);
        return !(neighbor.getBlock() instanceof DreamBlock) && !neighbor.isSolidRender(level, other);
    }

    private static boolean connected(BlockGetter level, BlockPos pos, Direction face, Direction along) {
        BlockPos other = pos.relative(along);
        return DreamBlock.isDream(level, other) && exposed(level, other, face);
    }

    private static void stars(VertexConsumer out, PoseStack.Pose pose, Face face, BlockPos pos,
                              Vec3 camera, double seconds, double minU, double minV, double maxU, double maxV) {
        double originU = face.projectU(pos.getX() + face.x, pos.getY() + face.y, pos.getZ() + face.z);
        double originV = face.projectV(pos.getX() + face.x, pos.getY() + face.y, pos.getZ() + face.z);
        long plane = (long) ((pos.getX() + face.x) * face.normal.getStepX()
                + (pos.getY() + face.y) * face.normal.getStepY()
                + (pos.getZ() + face.z) * face.normal.getStepZ());
        for (int layer = 2; layer >= 0; layer--) {
            double shiftU = face.projectU(camera.x, camera.y, camera.z) * PARALLAX[layer];
            double shiftV = face.projectV(camera.x, camera.y, camera.z) * PARALLAX[layer];
            int fromU = (int) Math.floor((originU - shiftU - STAR_RADIUS) / CELL);
            int toU = (int) Math.floor((originU - shiftU + 1 + STAR_RADIUS) / CELL);
            int fromV = (int) Math.floor((originV - shiftV - STAR_RADIUS) / CELL);
            int toV = (int) Math.floor((originV - shiftV + 1 + STAR_RADIUS) / CELL);
            for (int cy = fromV; cy <= toV; cy++) {
                for (int cx = fromU; cx <= toU; cx++) {
                    long seed = mix(cx * 0x9E3779B97F4A7C15L ^ cy * 0xC2B2AE3D27D4EB4FL
                            ^ plane * 0x165667B19E3779F9L ^ face.normal.ordinal() * 0x85EBCA77L);
                    // Vanilla density: about 0.7 particles per 8x8 pixels, weighted layers 1:2:3.
                    if (fraction(seed) >= 0.70) continue;
                    long details = mix(seed);
                    int choice = (int) Long.remainderUnsigned(details, 6);
                    int actualLayer = choice == 0 ? 0 : choice < 3 ? 1 : 2;
                    if (actualLayer != layer) continue;
                    double x = cx * CELL + ((details >>> 8) & 7) * PIXEL + PIXEL / 2
                            + shiftU - originU;
                    double y = cy * CELL + ((details >>> 12) & 7) * PIXEL + PIXEL / 2
                            + shiftV - originV;
                    double u0 = Math.max(x - STAR_RADIUS, minU), u1 = Math.min(x + STAR_RADIUS, maxU);
                    double v0 = Math.max(y - STAR_RADIUS, minV), v1 = Math.min(y + STAR_RADIUS, maxV);
                    if (u0 >= u1 || v0 >= v1) continue;
                    double phase = ((details >>> 20) & 1023) / 1024.0;
                    int shape = layer == 2 ? 0 : layer == 1
                            ? (((int) (seconds * 6 + phase * 2) & 1) == 0 ? 1 : 0)
                            : switch ((int) (seconds * 6 + phase * 4) & 3) {
                                case 0, 2 -> 1;
                                case 1 -> 2;
                                default -> 0;
                            };
                    int color = COLORS[layer][(int) ((details >>> 30) % 3)];
                    float texX = shape == 1 ? 9 : 1, texY = shape == 2 ? 9 : 1;
                    float s0 = (float) ((texX + (u0 - x + STAR_RADIUS) / PIXEL) / 16);
                    float s1 = (float) ((texX + (u1 - x + STAR_RADIUS) / PIXEL) / 16);
                    float t0 = (float) ((texY + (y + STAR_RADIUS - v1) / PIXEL) / 16);
                    float t1 = (float) ((texY + (y + STAR_RADIUS - v0) / PIXEL) / 16);
                    rect(out, pose, face, u0, v0, u1, v1, 0.0005, color, s0, t0, s1, t1);
                }
            }
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double fraction(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static void edge(VertexConsumer out, PoseStack.Pose pose, Face face, BlockPos pos,
                             double seconds, int side, boolean startCorner, boolean endCorner) {
        boolean vertical = side < 2;
        double worldAlong = vertical ? face.projectV(pos.getX(), pos.getY(), pos.getZ())
                : face.projectU(pos.getX(), pos.getY(), pos.getZ());
        double worldEdge = vertical ? face.projectU(pos.getX() + face.x, pos.getY() + face.y, pos.getZ() + face.z)
                : face.projectV(pos.getX() + face.x, pos.getY() + face.y, pos.getZ() + face.z);
        if (side == 1 || side == 3) worldEdge++;
        double previousInset = -1;
        for (int segment = 0; segment < 4; segment++) {
            double a = segment / 4.0, b = (segment + 1) / 4.0;
            double wave = Math.sin(seconds * 2 + (worldAlong + a) * 2.7
                    + Math.sin(seconds * 1.1 + worldEdge * 0.7) * 2);
            double inset = wave > 0.35 ? PIXEL : 0;
            if ((segment == 0 && startCorner) || (segment == 3 && endCorner)) inset = 0;
            double low = (side == 1 || side == 3) ? 1 - PIXEL - inset : inset;
            if (vertical) {
                rectSolid(out, pose, face, low, a, low + PIXEL, b, 0.0005, 0xFFFFFF);
            } else {
                rectSolid(out, pose, face, a, low, b, low + PIXEL, 0.0005, 0xFFFFFF);
            }
            // Short connectors keep the one-pixel contour unbroken where its inset changes.
            if (segment > 0 && previousInset != inset) {
                double previousLow = (side == 1 || side == 3) ? 1 - PIXEL - previousInset : previousInset;
                if (vertical) rectSolid(out, pose, face, previousLow,
                        a, previousLow + PIXEL, a + PIXEL, 0.0005, 0xFFFFFF);
                else rectSolid(out, pose, face, a, previousLow,
                        a + PIXEL, previousLow + PIXEL, 0.0005, 0xFFFFFF);
            }
            previousInset = inset;
        }
    }

    private static void rectSolid(VertexConsumer out, PoseStack.Pose pose, Face face,
                                  double u0, double v0, double u1, double v1, double offset, int rgb) {
        vertexSolid(out, pose, face, u0, v0, offset, rgb);
        vertexSolid(out, pose, face, u0, v1, offset, rgb);
        vertexSolid(out, pose, face, u1, v1, offset, rgb);
        vertexSolid(out, pose, face, u1, v0, offset, rgb);
    }

    private static void vertexSolid(VertexConsumer out, PoseStack.Pose pose, Face face,
                                    double u, double v, double offset, int rgb) {
        var n = face.normal;
        float x = (float) (face.x + u * face.u.getStepX() + v * face.v.getStepX() + offset * n.getStepX());
        float y = (float) (face.y + u * face.u.getStepY() + v * face.v.getStepY() + offset * n.getStepY());
        float z = (float) (face.z + u * face.u.getStepZ() + v * face.v.getStepZ() + offset * n.getStepZ());
        out.addVertex(pose, x, y, z).setColor((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, 255)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static void rect(VertexConsumer out, PoseStack.Pose pose, Face face,
                             double u0, double v0, double u1, double v1, double offset, int rgb,
                             float s0, float t0, float s1, float t1) {
        vertex(out, pose, face, u0, v0, offset, rgb, s0, t1);
        vertex(out, pose, face, u0, v1, offset, rgb, s0, t0);
        vertex(out, pose, face, u1, v1, offset, rgb, s1, t0);
        vertex(out, pose, face, u1, v0, offset, rgb, s1, t1);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Face face,
                               double u, double v, double offset, int rgb, float s, float t) {
        var n = face.normal;
        float x = (float) (face.x + u * face.u.getStepX() + v * face.v.getStepX() + offset * n.getStepX());
        float y = (float) (face.y + u * face.u.getStepY() + v * face.v.getStepY() + offset * n.getStepY());
        float z = (float) (face.z + u * face.u.getStepZ() + v * face.v.getStepZ() + offset * n.getStepZ());
        out.addVertex(pose, x, y, z).setColor((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, 255)
                .setUv(s, t).setLight(LightTexture.FULL_BRIGHT);
    }

    /** Vanilla unlit text shader, nearest sampling, opaque depth-writing cutout: no custom GLSL. */
    private static final class SurfaceType {
        static RenderType createSurface() {
            return RenderType.create("no_ultra_dash_please_dream", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 4096, false, false,
                    RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(PARTICLES, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY).setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false));
        }
    }

    private static final class EdgeType {
        static RenderType createEdge() {
            return RenderType.create("no_ultra_dash_please_dream_edge", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 4096, false, false,
                    RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .createCompositeState(false));
        }
    }

    private record Face(Direction normal, Direction u, Direction v, double x, double y, double z) {
        double projectU(double px, double py, double pz) {
            return px * u.getStepX() + py * u.getStepY() + pz * u.getStepZ();
        }

        double projectV(double px, double py, double pz) {
            return px * v.getStepX() + py * v.getStepY() + pz * v.getStepZ();
        }
    }
}
