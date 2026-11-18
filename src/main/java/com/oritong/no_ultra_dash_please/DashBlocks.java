package com.oritong.no_ultra_dash_please;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class DashBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NoUltraDashPlease.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NoUltraDashPlease.MOD_ID);
    private static final Map<Kind, DeferredBlock<Block>> SURFACES = new EnumMap<>(Kind.class);
    static final TagKey<Block> NO_REFILL = tag("no_ground_refill");
    static final TagKey<Block> DREAM = tag("dream_blocks");

    static void register(IEventBus bus) {
        for (Kind kind : Kind.values()) {
            var block = BLOCKS.<Block>register(kind.id, () -> kind == Kind.REFILL
                    ? new RefillCrystalBlock(RefillCrystalBlock.crystalProperties())
                    : kind == Kind.DREAM ? new DreamBlock(DreamBlock.dreamProperties()) : new Surface(kind));
            SURFACES.put(kind, block);
            ITEMS.registerSimpleBlockItem(block);
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        RefillCrystalBlock.register(bus);
        DreamBlock.register(bus);
        bus.addListener(DashBlocks::creative);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            SURFACES.values().forEach(block -> event.accept(block.get().asItem()));
        }
    }

    static Block block(Kind kind) {
        return SURFACES.get(kind).get();
    }

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, NoUltraDashPlease.id(name));
    }

    static Kind kind(BlockState state) {
        if (state.getBlock() instanceof RefillCrystalBlock) {
            return Kind.REFILL;
        }
        if (state.is(DREAM)) {
            return Kind.DREAM;
        }
        return state.getBlock() instanceof Surface surface ? surface.kind : null;
    }

    enum Kind {
        DREAM("dream_block"), SPRING("spring"), SUPER_SPRING("super_spring"), SIDE_SPRING("side_spring"),
        REBOUND("rebound_block"), REFLECT("reflect_block"), REFILL("refill_crystal"), BOOSTER("booster"), RED_BOOSTER("red_booster");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        boolean floorSpring() {
            return this == SPRING || this == SUPER_SPRING;
        }

        boolean booster() {
            return this == BOOSTER || this == RED_BOOSTER;
        }

        boolean nonSolid() {
            return booster() || this == REFILL;
        }
    }

    private static final class Surface extends Block {
        private static final VoxelShape CORE = box(2, 2, 2, 14, 14, 14);
        private final Kind kind;

        private Surface(Kind kind) {
            super(properties(kind));
            this.kind = kind;
        }

        private static BlockBehaviour.Properties properties(Kind kind) {
            var properties = BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> kind == Kind.DREAM || kind.nonSolid() ? 6 : 0);
            return kind.nonSolid() ? properties.noCollission().noOcclusion() : properties;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return kind.nonSolid() ? CORE : super.getShape(state, level, pos, context);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!kind.nonSolid() || !(entity instanceof ServerPlayer player) || !DashPhysics.eligible(player)
                    || !player.getBoundingBox().intersects(CORE.bounds().move(pos))) {
                return;
            }
            var dash = ServerDash.get(player);
            if (kind.booster()) {
                dash.queueBooster(kind, pos);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hitResult) {
            if (!kind.booster() || !DashPhysics.eligible(player)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer && !ServerDash.get(serverPlayer).queueBooster(kind, pos)) {
                return InteractionResult.PASS;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            if (kind.floorSpring() && entity instanceof Player player && DashPhysics.eligible(player) && !player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ServerDash.get(serverPlayer).queueSpring(kind, pos);
                }
                return;
            }
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    private DashBlocks() {}
}
