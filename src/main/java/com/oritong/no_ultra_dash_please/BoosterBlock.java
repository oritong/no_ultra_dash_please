package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Keeps booster interaction unchanged; the client entity renderer supplies the membrane. */
final class BoosterBlock extends BaseEntityBlock {
    private static final MapCodec<BoosterBlock> CODEC = simpleCodec(BoosterBlock::new);
    private static final VoxelShape CORE = box(2, 2, 2, 14, 14, 14);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NoUltraDashPlease.MOD_ID);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BubbleEntity>> ENTITY =
            ENTITIES.register("booster", () -> BlockEntityType.Builder.of(BubbleEntity::new,
                    DashBlocks.block(DashBlocks.Kind.BOOSTER), DashBlocks.block(DashBlocks.Kind.RED_BOOSTER)).build(null));

    static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    static BlockBehaviour.Properties bubbleProperties() {
        return BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST)
                .lightLevel(state -> 6).noCollission().noOcclusion();
    }

    BoosterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BoosterBlock> codec() {
        return CODEC;
    }

    static DashBlocks.Kind kind(BlockState state) {
        return state.is(DashBlocks.block(DashBlocks.Kind.RED_BOOSTER))
                ? DashBlocks.Kind.RED_BOOSTER : DashBlocks.Kind.BOOSTER;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BubbleEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && DashPhysics.eligible(player)
                && player.getBoundingBox().intersects(CORE.bounds().move(pos))) {
            ServerDash.get(player).queueBooster(kind(state), pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!DashPhysics.eligible(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && !ServerDash.get(serverPlayer).queueBooster(kind(state), pos)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static final class BubbleEntity extends BlockEntity {
        BubbleEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }
    }
}
