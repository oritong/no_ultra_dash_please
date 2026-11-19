package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Solid gameplay block; its client renderer supplies the connected dream surface. */
final class DreamBlock extends BaseEntityBlock {
    private static final MapCodec<DreamBlock> CODEC = simpleCodec(DreamBlock::new);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NoUltraDashPlease.MOD_ID);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DreamEntity>> ENTITY =
            ENTITIES.register("dream_block", () -> BlockEntityType.Builder.of(DreamEntity::new,
                    DashBlocks.block(DashBlocks.Kind.DREAM)).build(null));

    static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    static BlockBehaviour.Properties dreamProperties() {
        // The camera travels inside this block. Keep ordinary neighboring faces
        // and their chunk sections visible from there; collision stays solid.
        return BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST)
                .lightLevel(state -> 6).noOcclusion();
    }

    DreamBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DreamBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DreamEntity(pos, state);
    }

    static boolean isDream(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DreamBlock;
    }

    static final class DreamEntity extends BlockEntity {
        DreamEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }
    }
}
