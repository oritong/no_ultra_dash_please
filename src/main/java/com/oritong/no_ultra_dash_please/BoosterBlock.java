package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
    static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
    static final int RESPAWN_TICKS = 20; // Celeste Booster.PlayerReleased: respawnTimer = 1 second.
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
        registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.READY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
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
            activate(level, pos, player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!DashPhysics.eligible(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && !activate(level, pos, serverPlayer)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean activate(Level level, BlockPos pos, ServerPlayer player) {
        var current = level.getBlockState(pos);
        if (!current.is(this) || current.getValue(PHASE) != Phase.READY
                || !ServerDash.get(player).queueBooster(kind(current), pos)) return false;
        level.setBlock(pos, current.setValue(PHASE, Phase.OCCUPIED), UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BubbleEntity bubble) {
            bubble.occupant = player.getUUID();
            bubble.setChanged();
        }
        // Recover safely if a player disconnects, dies or changes dimension mid-boost.
        level.scheduleTick(pos, this, 1);
        return true;
    }

    static void launched(ServerPlayer player, BlockPos pos) {
        if (pos == null) return;
        var state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof BoosterBlock && state.getValue(PHASE) == Phase.OCCUPIED) {
            player.level().setBlock(pos, state.setValue(PHASE, Phase.EMPTY), UPDATE_ALL);
        }
    }

    static void released(ServerPlayer player, BlockPos pos) {
        if (pos == null) return;
        var level = player.serverLevel();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BoosterBlock block && state.getValue(PHASE) != Phase.READY
                && state.getValue(PHASE) != Phase.COOLDOWN) block.cooldown(level, pos, state);
    }

    private void cooldown(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PHASE, Phase.COOLDOWN), UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BubbleEntity bubble) {
            bubble.returnAt = level.getGameTime() + RESPAWN_TICKS;
            bubble.occupant = null;
            bubble.setChanged();
        }
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BubbleEntity bubble)) return;
        Phase phase = state.getValue(PHASE);
        if (phase == Phase.COOLDOWN) {
            if (level.getGameTime() >= bubble.returnAt) {
                level.setBlock(pos, state.setValue(PHASE, Phase.READY), UPDATE_ALL);
                level.playSound(null, pos, (kind(state) == DashBlocks.Kind.RED_BOOSTER
                        ? DashSounds.RED_BOOSTER_REAPPEAR : DashSounds.BOOSTER_REAPPEAR).get(), SoundSource.BLOCKS, 0.65F, 1.0F);
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10, 0.25, 0.25, 0.25, 0.02);
            } else level.scheduleTick(pos, this, 1);
        } else if (phase != Phase.READY) {
            var player = bubble.occupant == null ? null : level.getPlayerByUUID(bubble.occupant);
            if (!(player instanceof ServerPlayer server) || !player.isAlive()
                    || !ServerDash.get(server).usesBooster(pos)) cooldown(level, pos, state);
            else level.scheduleTick(pos, this, 1);
        }
    }

    enum Phase implements StringRepresentable {
        READY("ready"), OCCUPIED("occupied"), EMPTY("empty"), COOLDOWN("cooldown");
        private final String name;
        Phase(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    static final class BubbleEntity extends BlockEntity {
        java.util.UUID occupant;
        long returnAt;
        BubbleEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (occupant != null) tag.putUUID("Occupant", occupant);
            tag.putLong("ReturnAt", returnAt);
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            occupant = tag.hasUUID("Occupant") ? tag.getUUID("Occupant") : null;
            returnAt = tag.getLong("ReturnAt");
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server && getBlockState().getValue(PHASE) != Phase.READY) {
                server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
            }
        }
    }
}
