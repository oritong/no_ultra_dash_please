package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class RefillCrystalRenderer implements BlockEntityRenderer<RefillCrystalBlock.CrystalEntity> {
    private final BlockRenderDispatcher blocks;

    private RefillCrystalRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RefillCrystalBlock.ENTITY.get(), RefillCrystalRenderer::new);
    }

    @Override
    public void render(RefillCrystalBlock.CrystalEntity crystal, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var level = crystal.getLevel();
        if (level == null) {
            return;
        }
        var state = crystal.getBlockState();
        boolean empty = state.getValue(RefillCrystalBlock.PHASE) != RefillCrystalBlock.Phase.READY;
        // Position offsets prevent rows of crystals from moving in lockstep. Keep time bounded for precision.
        double seconds = ((level.getGameTime() % 120_000L) + partialTick) / 20.0;
        double offset = Math.floorMod(crystal.getBlockPos().asLong(), 1024L) / 1024.0 * Math.PI * 2;
        double bob = Math.sin(seconds * Math.PI * 2 * 0.6 + offset) * 0.125;
        poses.pushPose();
        poses.translate(0.5, 0.5 + bob, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees((float)(seconds * 45 + Math.toDegrees(offset))));
        poses.translate(-0.5, -0.5, -0.5);
        blocks.getModelRenderer().renderModel(poses.last(),
                buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), state,
                blocks.getBlockModel(state), 1, 1, 1, LightTexture.FULL_BRIGHT, packedOverlay,
                ModelData.EMPTY, empty ? RenderType.cutout() : RenderType.solid());
        poses.popPose();
    }
}
