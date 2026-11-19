package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Half-height crumble floor with dependency-free, diagonal-aware multipart textures. */
final class CrumblePlatformBlock extends BaseEntityBlock {
    private static final MapCodec<CrumblePlatformBlock> CODEC = simpleCodec(CrumblePlatformBlock::new);
    static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
    private static final BooleanProperty[] CONNECTIONS = {
            BooleanProperty.create("north"), BooleanProperty.create("east"),
            BooleanProperty.create("south"), BooleanProperty.create("west"),
            BooleanProperty.create("north_east"), BooleanProperty.create("south_east"),
            BooleanProperty.create("south_west"), BooleanProperty.create("north_west")
    };
    private static final int[][] OFFSETS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };
    static final VoxelShape SLAB = box(0, 0, 0, 16, 8, 16);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NoUltraDashPlease.MOD_ID);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShakeEntity>> ENTITY =
            ENTITIES.register("crumble_platform", () -> BlockEntityType.Builder.of(ShakeEntity::new,
                    DashBlocks.block(DashBlocks.Kind.CRUMBLE)).build(null));

    static void register(IEventBus bus) { ENTITIES.register(bus); }

    static BlockBehaviour.Properties platformProperties() {
        return BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.STONE).noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    CrumblePlatformBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(PHASE, Phase.NORMAL);
        for (BooleanProperty connection : CONNECTIONS) {
            state = state.setValue(connection, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<CrumblePlatformBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ShakeEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Only the warning model moves; stable and collapsed geometry remains chunk-batched.
        return state.getValue(PHASE) == Phase.CRUMBLING ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
        builder.add(CONNECTIONS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        boolean collapsed = state.getValue(PHASE) == Phase.COLLAPSED;
        for (int i = 0; i < CONNECTIONS.length; i++) {
            BlockState neighbor = level.getBlockState(pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]));
            state = state.setValue(CONNECTIONS[i], neighbor.is(this)
                    && (neighbor.getValue(PHASE) == Phase.COLLAPSED) == collapsed);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connected(state, level, pos);
    }

    void refresh(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.is(this)) {
            BlockState updated = connected(current, level, pos);
            if (updated != current) {
                level.setBlock(pos, updated, UPDATE_CLIENTS);
            }
        }
    }

    private void refreshNeighbors(Level level, BlockPos pos) {
        // Vanilla shape updates reach six faces only; concave CTM corners also need diagonals.
        for (int[] offset : OFFSETS) {
            refresh(level, pos.offset(offset[0], 0, offset[1]));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (level.isClientSide) {
            return;
        }
        boolean added = !oldState.is(this);
        if (added || oldState.getValue(PHASE) != state.getValue(PHASE)) {
            if (added && state.getValue(PHASE) != Phase.NORMAL) {
                // Adopt a command-placed/legacy outline into the persistent group timer.
                level.scheduleTick(pos, this, 1);
            }
        }
        if (added || (oldState.getValue(PHASE) == Phase.COLLAPSED) != (state.getValue(PHASE) == Phase.COLLAPSED)) {
            refresh(level, pos);
            refreshNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (!level.isClientSide && !newState.is(this)) {
            refreshNeighbors(level, pos);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PHASE) == Phase.COLLAPSED ? Shapes.empty() : SLAB;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        BlockState current = level.getBlockState(pos);
        if (!level.isClientSide && entity instanceof Player player && !player.isSpectator()
                && current.is(this) && current.getValue(PHASE) == Phase.NORMAL) {
            CrumbleGroups.begin((ServerLevel) level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PHASE) != Phase.NORMAL) CrumbleGroups.begin(level, pos);
    }

    enum Phase implements StringRepresentable {
        NORMAL("normal"), CRUMBLING("crumbling"), COLLAPSED("collapsed");

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    static final class ShakeEntity extends BlockEntity {
        private long warningStarted = Long.MIN_VALUE;
        private long warningSeed;

        ShakeEntity(BlockPos pos, BlockState state) { super(ENTITY.get(), pos, state); }

        void startWarning(long started, long seed) {
            warningStarted = started;
            warningSeed = seed;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), UPDATE_CLIENTS);
            }
        }

        public Vec3 visualOffset(float partialTick) {
            if (level == null || getBlockState().getValue(PHASE) != Phase.CRUMBLING
                    || warningStarted == Long.MIN_VALUE) return Vec3.ZERO;
            return CrumbleShake.offset(warningSeed, level.getGameTime() - warningStarted + partialTick);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server && getBlockState().getValue(PHASE) == Phase.CRUMBLING) {
                CrumbleGroups.restoreWarning(server, worldPosition, this);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putLong("WarningStarted", warningStarted);
            tag.putLong("WarningSeed", warningSeed);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            warningStarted = tag.contains("WarningStarted") ? tag.getLong("WarningStarted") : Long.MIN_VALUE;
            warningSeed = tag.getLong("WarningSeed");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    }
}
