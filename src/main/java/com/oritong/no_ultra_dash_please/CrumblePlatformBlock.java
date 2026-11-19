package com.oritong.no_ultra_dash_please;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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

/** Half-height crumble floor with dependency-free, diagonal-aware multipart textures. */
final class CrumblePlatformBlock extends Block {
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
    private static final VoxelShape SLAB = box(0, 0, 0, 16, 8, 16);
    // Minecraft adaptation: 0.6 s warning, then a 2 s absence before safe recovery.
    private static final int WARNING_TICKS = 12;
    private static final int RETURN_TICKS = 40;

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

    private void refresh(Level level, BlockPos pos) {
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
            if (state.getValue(PHASE) != Phase.NORMAL) {
                level.scheduleTick(pos, this, state.getValue(PHASE) == Phase.COLLAPSED ? RETURN_TICKS : WARNING_TICKS);
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
            level.setBlock(pos, current.setValue(PHASE, Phase.CRUMBLING), UPDATE_ALL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PHASE) == Phase.CRUMBLING) {
            level.setBlock(pos, state.setValue(PHASE, Phase.COLLAPSED), UPDATE_ALL);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 16, 0.35, 0.08, 0.35, 0.03);
        } else if (state.getValue(PHASE) == Phase.COLLAPSED) {
            // Never regenerate a solid slab inside a player or mob passing through its outline.
            if (level.getEntitiesOfClass(LivingEntity.class, SLAB.bounds().move(pos),
                    entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) {
                level.setBlock(pos, state.setValue(PHASE, Phase.NORMAL), UPDATE_ALL);
            } else {
                level.scheduleTick(pos, this, 2);
            }
        }
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
}
