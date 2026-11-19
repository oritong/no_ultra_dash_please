package com.oritong.no_ultra_dash_please;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreamExitPoseGameTests {
    @GameTest(template = "empty")
    public static void crouchesIntoOneAndHalfBlockExit(GameTestHelper helper) {
        sideExit(helper, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        exitsAndRestores(helper, Pose.CROUCHING);
    }

    @GameTest(template = "empty")
    public static void crawlsIntoOneBlockExit(GameTestHelper helper) {
        sideExit(helper, Blocks.STONE.defaultBlockState());
        exitsAndRestores(helper, Pose.SWIMMING);
    }

    @GameTest(template = "empty")
    public static void downwardExitCrawlsBeforeHittingFloor(GameTestHelper helper) {
        fill(helper, new BlockPos(1, 1, 1), new BlockPos(6, 1, 6), Blocks.STONE.defaultBlockState());
        fill(helper, new BlockPos(2, 3, 2), new BlockPos(5, 4, 5), DashBlocks.block(DashBlocks.Kind.DREAM).defaultBlockState());
        var player = player(helper, new Vec3(3.5, 6, 3.5));
        var dash = ServerDash.get(player);
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, 0, 90, 0));
        boolean entered = false;
        for (int tick = 0; tick < 12; tick++) {
            dash.tick(player);
            player.doTick();
            entered |= dash.dreaming();
            if (entered && !dash.dreaming()) break;
            helper.assertTrue(player.getDeltaMovement().y <= 0, "A crawlable bottom exit must not bounce upwards");
        }
        helper.assertTrue(entered && !dash.dreaming(), "Downward traversal must leave into the short space");
        helper.assertTrue(player.getPose() == Pose.SWIMMING, "One-block bottom exit requires a crawling pose");
        helper.assertTrue(DashPhysics.clear(player, player.getBoundingBox()), "Crawling player must fit outside dream and floor");
        helper.assertTrue(player.getBoundingBox().minY >= helper.absolutePos(new BlockPos(0, 2, 0)).getY(),
                "Automatic crawling must never push feet into the floor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void halfBlockExitStillBounces(GameTestHelper helper) {
        sideExit(helper, Blocks.STONE.defaultBlockState());
        fill(helper, new BlockPos(5, 3, 1), new BlockPos(7, 3, 6),
                Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        var player = player(helper, new Vec3(1.5, 3, 3.5));
        var dash = ServerDash.get(player);
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, -90, 0, 0));
        boolean bounced = false;
        for (int tick = 0; tick < 12; tick++) {
            dash.tick(player);
            player.doTick();
            bounced |= dash.dreaming() && player.getDeltaMovement().x < -0.9;
            helper.assertTrue(player.getBoundingBox().maxX <= helper.absolutePos(new BlockPos(5, 0, 0)).getX() + 1.0e-4,
                    "Even the smallest pose must not clip through a half-block gap");
            if (bounced && !dash.dreaming()) break;
        }
        helper.assertTrue(bounced, "An exit too small for crawling must retain the original reverse bounce");
        helper.succeed();
    }

    private static void exitsAndRestores(GameTestHelper helper, Pose expected) {
        var player = player(helper, new Vec3(1.5, 3, 3.5));
        var dash = ServerDash.get(player);
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, -90, 0, 0));
        boolean entered = false;
        for (int tick = 0; tick < 12; tick++) {
            dash.tick(player);
            player.doTick();
            entered |= dash.dreaming();
            if (entered && !dash.dreaming()) break;
            helper.assertTrue(player.getDeltaMovement().x >= 0, "A fitting exit must not bounce the player");
        }
        helper.assertTrue(entered && !dash.dreaming(), "Traversal must exit successfully");
        helper.assertTrue(player.getX() > helper.absolutePos(new BlockPos(5, 0, 0)).getX(), "Player must emerge on the far side");
        helper.assertTrue(player.getPose() == expected, "Choose the tallest fitting pose: expected " + expected + ", got " + player.getPose());
        helper.assertTrue(DashPhysics.clear(player, player.getBoundingBox()), "Selected exit pose must not overlap solid blocks");
        // Stay beneath the roof after dash ownership ends, then move to open space.
        for (int tick = 0; tick < 24; tick++) {
            dash.tick(player);
            player.doTick();
        }
        helper.assertTrue(player.getPose() == expected, "The low pose must persist while the ceiling remains");
        player.setPos(helper.absoluteVec(new Vec3(1, 3, 3.5)));
        dash.tick(player);
        player.doTick();
        helper.assertTrue(player.getPose() == Pose.STANDING, "Open space must restore standing automatically");
        helper.succeed();
    }

    private static void sideExit(GameTestHelper helper, BlockState roof) {
        fill(helper, new BlockPos(0, 2, 0), new BlockPos(7, 2, 7), Blocks.STONE.defaultBlockState());
        fill(helper, new BlockPos(3, 3, 1), new BlockPos(4, 4, 6), DashBlocks.block(DashBlocks.Kind.DREAM).defaultBlockState());
        fill(helper, new BlockPos(5, 4, 1), new BlockPos(7, 4, 6), roof);
    }

    private static void fill(GameTestHelper helper, BlockPos min, BlockPos max, BlockState state) {
        for (var pos : BlockPos.betweenClosed(min, max)) helper.setBlock(pos, state);
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 position) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dream-pose-test"));
        player.setNoGravity(true);
        player.setPos(helper.absoluteVec(position));
        player.setOnGround(false);
        return player;
    }
}
