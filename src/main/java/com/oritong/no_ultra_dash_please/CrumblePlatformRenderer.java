package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class CrumblePlatformRenderer implements BlockEntityRenderer<CrumblePlatformBlock.ShakeEntity> {
    private CrumblePlatformRenderer(BlockEntityRendererProvider.Context context) { }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CrumblePlatformBlock.ENTITY.get(), CrumblePlatformRenderer::new);
    }

    @Override
    public void render(CrumblePlatformBlock.ShakeEntity block, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        var level = block.getLevel();
        var state = block.getBlockState();
        if (level == null || state.getValue(CrumblePlatformBlock.PHASE) != CrumblePlatformBlock.Phase.CRUMBLING) return;
        var offset = block.visualOffset(partialTick);
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var model = dispatcher.getBlockModel(state);
        long seed = state.getSeed(block.getBlockPos());
        poses.pushPose();
        poses.translate(offset.x, offset.y, offset.z);
        // Tessellate the exact same multipart model with its original position seed.
        // renderSingleBlock uses a constant seed and would change the four random textures.
        dispatcher.getModelRenderer().tesselateBlock(level, model, state, block.getBlockPos(), poses,
                buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(RenderType.cutout())), false,
                RandomSource.create(seed), seed, packedOverlay, ModelData.EMPTY, RenderType.cutout());
        poses.popPose();
    }
}
