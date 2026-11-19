package com.oritong.no_ultra_dash_please;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrumblePlatformGameTests {
    private static final List<BlockPos> PATCH = List.of(new BlockPos(2, 3, 2), new BlockPos(3, 3, 2),
            new BlockPos(4, 3, 2), new BlockPos(4, 3, 3), new BlockPos(4, 3, 4), new BlockPos(5, 3, 4));

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void wholePatchCollapsesTogetherAndReturnsAfterTwoSeconds(GameTestHelper helper) {
        place(helper);
        var level = helper.getLevel();
        var pos = helper.absolutePos(PATCH.getFirst());
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "crumble-standing"));
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        player.setNoGravity(true);
        level.addFreshEntity(player);
        CrumbleGroups.begin(level, pos);
        phase(helper, CrumblePlatformBlock.Phase.CRUMBLING);
        helper.runAfterDelay(11, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.CRUMBLING);
            // Another footstep must not reset the shared timer.
            CrumbleGroups.begin(level, helper.absolutePos(PATCH.getLast()));
        });
        helper.runAfterDelay(12, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.COLLAPSED);
            player.discard();
        });
        helper.runAfterDelay(51, () -> { advance(helper); phase(helper, CrumblePlatformBlock.Phase.COLLAPSED); });
        helper.runAfterDelay(52, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.NORMAL);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void leavingEarlyUsesOriginalMinimumWarning(GameTestHelper helper) {
        place(helper);
        var level = helper.getLevel();
        var origin = helper.absolutePos(PATCH.getFirst());
        var isolated = new BlockPos(1, 3, 1); // Diagonal only; no four-neighbor path.
        var stacked = new BlockPos(2, 4, 2);
        helper.setBlock(isolated, block());
        helper.setBlock(stacked, block());
        CrumbleGroups.begin(level, origin);
        helper.assertTrue(CrumbleGroups.connected(level, origin).size() == PATCH.size(), "Search must follow all branches, excluding diagonal and vertical contacts");
        helper.runAfterDelay(3, () -> { advance(helper); phase(helper, CrumblePlatformBlock.Phase.CRUMBLING); });
        helper.runAfterDelay(4, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.COLLAPSED);
            helper.assertTrue(helper.getBlockState(isolated).getValue(CrumblePlatformBlock.PHASE) == CrumblePlatformBlock.Phase.NORMAL,
                    "Diagonally touching platforms must not join the collapse");
            helper.assertTrue(helper.getBlockState(stacked).getValue(CrumblePlatformBlock.PHASE) == CrumblePlatformBlock.Phase.NORMAL,
                    "Platforms on another height must not join the collapse");
        });
        helper.runAfterDelay(44, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.NORMAL);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void occupiedCellDelaysRecoveryOfEntirePatch(GameTestHelper helper) {
        place(helper);
        CrumbleGroups.begin(helper.getLevel(), helper.absolutePos(PATCH.getFirst()));
        var blocker = helper.spawn(EntityType.ARMOR_STAND, PATCH.getLast());
        blocker.setNoGravity(true);
        helper.runAfterDelay(44, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.COLLAPSED);
            blocker.discard();
        });
        helper.runAfterDelay(46, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.NORMAL);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void searchFollowsLongWindingConnectionsWithoutDiagonalShortcuts(GameTestHelper helper) {
        int count = 0;
        // 7 x 7 serpentine, with gaps between its parallel rows.
        for (int z = 0; z <= 6; z += 2) {
            for (int x = 0; x <= 6; x++) { helper.setBlock(x, 3, z, block()); count++; }
            if (z < 6) { helper.setBlock(z % 4 == 0 ? 6 : 0, 3, z + 1, block()); count++; }
        }
        var positions = CrumbleGroups.connected(helper.getLevel(), helper.absolutePos(new BlockPos(0, 3, 0)));
        helper.assertTrue(positions.size() == count, "Flood fill must reach every cell along a winding component");
        CrumbleGroups.begin(helper.getLevel(), positions.iterator().next());
        for (var pos : positions) helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(CrumblePlatformBlock.PHASE)
                == CrumblePlatformBlock.Phase.CRUMBLING, "All distant branches must share the warning phase");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void timersSurviveSaveAndReloadWithoutRestarting(GameTestHelper helper) {
        place(helper);
        var level = helper.getLevel();
        CrumbleGroups.begin(level, helper.absolutePos(PATCH.getFirst()));
        helper.runAfterDelay(10, () -> {
            var original = level.getDataStorage().get(CrumbleGroups.FACTORY, CrumbleGroups.DATA_ID);
            var saved = original.save(new CompoundTag(), level.registryAccess());
            var loaded = CrumbleGroups.load(saved, level.registryAccess());
            level.getDataStorage().set(CrumbleGroups.DATA_ID, loaded);
            helper.assertTrue(saved.equals(loaded.save(new CompoundTag(), level.registryAccess())), "All group positions and deadlines must round-trip");
        });
        helper.runAfterDelay(44, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.NORMAL);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void warningShakesVisualsTogetherWithoutMovingCollision(GameTestHelper helper) {
        place(helper);
        var level = helper.getLevel();
        var origin = helper.absolutePos(PATCH.getFirst());
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "shake-standing"));
        player.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        player.setNoGravity(true);
        level.addFreshEntity(player);
        CrumbleGroups.begin(level, origin);
        helper.runAfterDelay(2, () -> {
            Vec3 shared = null;
            for (BlockPos relative : PATCH) {
                var pos = helper.absolutePos(relative);
                var state = level.getBlockState(pos);
                var shake = (CrumblePlatformBlock.ShakeEntity) level.getBlockEntity(pos);
                Vec3 offset = shake.visualOffset(0.75F);
                helper.assertTrue(offset.lengthSqr() > 0, "Every warning tile must visibly shake");
                helper.assertTrue(Math.abs(offset.x) <= 1.0 / 16 && Math.abs(offset.z) <= 1.0 / 16
                                && Math.abs(offset.y) <= 0.2 / 16, "Warning jitter must remain within one texture pixel");
                if (shared != null) helper.assertTrue(shared.equals(offset), "A connected platform must keep its seams joined while shaking");
                shared = offset;
                helper.assertTrue(state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED,
                        "The warning must replace, rather than overlap, its stationary chunk model");
                helper.assertTrue(state.getCollisionShape(level, pos).bounds().equals(CrumblePlatformBlock.SLAB.bounds()),
                        "Visual jitter must never move the supporting collision");
                var copy = new CrumblePlatformBlock.ShakeEntity(pos, state);
                copy.setLevel(level);
                copy.loadAdditional(shake.getUpdateTag(level.registryAccess()), level.registryAccess());
                helper.assertTrue(copy.visualOffset(0.75F).equals(offset), "Synchronized warning data must preserve the animation phase");
            }
        });
        helper.runAfterDelay(12, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.COLLAPSED);
            for (BlockPos pos : PATCH) {
                var state = helper.getBlockState(pos);
                helper.assertTrue(state.getRenderShape() == RenderShape.MODEL, "The collapsed outline must return to static rendering");
                helper.assertTrue(((CrumblePlatformBlock.ShakeEntity) level.getBlockEntity(helper.absolutePos(pos)))
                        .visualOffset(0.5F).equals(Vec3.ZERO), "The collapsed outline must stop shaking immediately");
            }
            player.discard();
        });
        helper.runAfterDelay(52, () -> {
            advance(helper);
            phase(helper, CrumblePlatformBlock.Phase.NORMAL);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void oldPlatformsGainWarningEntitiesAndKeepTheirStableModel(GameTestHelper helper) {
        place(helper);
        for (BlockPos relative : PATCH) {
            var pos = helper.absolutePos(relative);
            var state = helper.getLevel().getBlockState(pos);
            var chunk = helper.getLevel().getChunkAt(pos);
            chunk.removeBlockEntity(pos);
            RefillCrystalMigration.ensureEntities(chunk);
            helper.assertTrue(chunk.getBlockEntity(pos) instanceof CrumblePlatformBlock.ShakeEntity,
                    "Existing saved platforms must gain their warning renderer");
            helper.assertTrue(helper.getLevel().getBlockState(pos).equals(state) && state.getRenderShape() == RenderShape.MODEL,
                    "Migration must retain phase, connection flags and the stationary model");
        }
        helper.succeed();
    }

    private static CrumblePlatformBlock block() { return (CrumblePlatformBlock) DashBlocks.block(DashBlocks.Kind.CRUMBLE); }

    private static void place(GameTestHelper helper) { for (var pos : PATCH) helper.setBlock(pos, block()); }

    private static void advance(GameTestHelper helper) {
        helper.getLevel().getDataStorage().get(CrumbleGroups.FACTORY, CrumbleGroups.DATA_ID).advance(helper.getLevel());
    }

    private static void phase(GameTestHelper helper, CrumblePlatformBlock.Phase expected) {
        for (var pos : PATCH) {
            var state = helper.getBlockState(pos);
            helper.assertTrue(state.getValue(CrumblePlatformBlock.PHASE) == expected, "Expected entire patch " + expected + " at " + pos + ", got " + state);
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos)).isEmpty()
                    == (expected == CrumblePlatformBlock.Phase.COLLAPSED), "Only the outline phase should be passable");
        }
    }
}
