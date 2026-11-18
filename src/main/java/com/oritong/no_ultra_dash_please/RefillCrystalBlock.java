package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

final class RefillCrystalBlock extends BaseEntityBlock {
    static final MapCodec<RefillCrystalBlock> CODEC = simpleCodec(RefillCrystalBlock::new);
    static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
    // Celeste Refill.OnPlayer starts a 2.5 second respawn timer; Minecraft runs at 20 ticks/s.
    static final int RESPAWN_TICKS = 50;
    static final int REGEN_EFFECT_TICKS = 2;
    private static final VoxelShape CORE = box(2, 0, 2, 14, 16, 14);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.24F, 1.0F, 0.52F), 0.8F);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NoUltraDashPlease.MOD_ID);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalEntity>> ENTITY =
            ENTITIES.register("refill_crystal", () -> BlockEntityType.Builder.of(CrystalEntity::new,
                    DashBlocks.block(DashBlocks.Kind.REFILL)).build(null));

    static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    static BlockBehaviour.Properties crystalProperties() {
        return BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).sound(SoundType.AMETHYST)
                .noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(PHASE) == Phase.READY ? 12 : 2);
    }

    RefillCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.READY));
    }

    @Override
    protected MapCodec<RefillCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Re-read the world state: multiple players may touch the same crystal in one server tick.
        if (!level.isClientSide && level.getBlockState(pos).getValue(PHASE) == Phase.READY
                && entity instanceof ServerPlayer player && DashPhysics.eligible(player)
                && player.getBoundingBox().intersects(CORE.bounds().move(pos))
                && ServerDash.get(player).touchRefill(player, pos)) {
            level.setBlock(pos, state.setValue(PHASE, Phase.EMPTY), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, RESPAWN_TICKS - REGEN_EFFECT_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        switch (state.getValue(PHASE)) {
            case EMPTY -> {
                level.setBlock(pos, state.setValue(PHASE, Phase.REGENERATING), Block.UPDATE_ALL);
                // A small ring gathers around the empty outline just before the crystal returns.
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4.0;
                    level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5 + Math.cos(angle) * 0.3,
                            pos.getY() + 0.5, pos.getZ() + 0.5 + Math.sin(angle) * 0.3,
                            1, 0, 0.03, 0, 0.005);
                }
                level.scheduleTick(pos, this, REGEN_EFFECT_TICKS);
            }
            case REGENERATING -> {
                level.setBlock(pos, state.setValue(PHASE, Phase.READY), Block.UPDATE_ALL);
                level.sendParticles(GREEN, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        16, 0.18, 0.22, 0.18, 0.015);
                level.playSound(null, pos, DashSounds.REFILL_RETURN.get(), SoundSource.BLOCKS, 0.65F, 1.0F);
            }
            case READY -> { }
        }
    }

    enum Phase implements StringRepresentable {
        READY("ready"), EMPTY("empty"), REGENERATING("regenerating");

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    static final class CrystalEntity extends BlockEntity {
        CrystalEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }
    }
}
