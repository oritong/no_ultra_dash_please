package com.oritong.no_ultra_dash_please;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreamTraversalGameTests {
    @GameTest(template = "empty")
    public static void enterFromAllSixFaces(GameTestHelper helper) {
        fillDream(helper, new BlockPos(3, 3, 3), new BlockPos(4, 4, 4));
        crossing(helper, new Vec3(1.5, 3.05, 3.5), new Vec3(1, 0, 0), -90, 0);
        crossing(helper, new Vec3(6.5, 3.05, 3.5), new Vec3(-1, 0, 0), 90, 0);
        crossing(helper, new Vec3(3.5, 3.05, 1.5), new Vec3(0, 0, 1), 0, 0);
        crossing(helper, new Vec3(3.5, 3.05, 6.5), new Vec3(0, 0, -1), 180, 0);
        crossing(helper, new Vec3(3.5, 0.8, 3.5), new Vec3(0, 1, 0), 0, -90);
        crossing(helper, new Vec3(3.5, 6.5, 3.5), new Vec3(0, -1, 0), 0, 90);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sealedExitBouncesAlongEntryPath(GameTestHelper helper) {
        fillDream(helper, new BlockPos(3, 3, 3), new BlockPos(4, 4, 4));
        for (int y = 1; y < 7; y++) {
            for (int z = 1; z < 7; z++) {
                helper.setBlock(5, y, z, Blocks.STONE);
            }
        }
        var player = player(helper, new Vec3(1.5, 3.05, 3.5));
        var dash = start(player, -90, 0);
        boolean entered = false, bounced = false, exited = false;
        for (int i = 0; i < 16; i++) {
            Vec3 before = player.position();
            dash.tick(player);
            entered |= dash.dreaming();
            if (dash.dreaming() && player.getDeltaMovement().x < -0.9) {
                bounced = true;
                helper.assertTrue(player.getDeltaMovement().distanceTo(new Vec3(-1, 0, 0)) < 1.0e-4,
                        "Bounce must reverse the complete incoming vector without changing its speed");
            }
            helper.assertTrue(player.position().distanceTo(before) <= 1.01, "Wall bounce must travel, not teleport to entry");
            helper.assertTrue(player.getBoundingBox().maxX <= helper.absolutePos(new BlockPos(5, 0, 0)).getX() + 1.0e-4,
                    "Dream traversal must not clip through the ordinary wall");
            if (bounced && !dash.dreaming() && player.getX() < helper.absolutePos(new BlockPos(3, 0, 0)).getX()) {
                exited = true;
                break;
            }
        }
        helper.assertTrue(entered, "Sealed exit must still allow entry into the dream block");
        helper.assertTrue(bounced, "Ordinary wall must reverse dream movement");
        helper.assertTrue(exited, "Player must travel back through the original entrance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void undersideWithSolidRoofBouncesDown(GameTestHelper helper) {
        for (int x = 2; x <= 5; x++) {
            for (int z = 2; z <= 5; z++) {
                helper.setBlock(x, 3, z, DashBlocks.block(DashBlocks.Kind.DREAM));
                helper.setBlock(x, 4, z, Blocks.STONE);
            }
        }
        var player = player(helper, new Vec3(3.5, 0.8, 3.5));
        var dash = start(player, 0, -90);
        boolean entered = false, bounced = false;
        for (int i = 0; i < 12; i++) {
            dash.tick(player);
            entered |= dash.dreaming();
            bounced |= dash.dreaming() && player.getDeltaMovement().y < -0.9;
            if (bounced && !dash.dreaming()) break;
        }
        helper.assertTrue(entered, "The bottom face must admit an upward dash");
        helper.assertTrue(bounced, "Solid roof must reverse the upward dash downwards");
        helper.assertTrue(!dash.dreaming(), "Player must leave through the bottom after bouncing");
        helper.succeed();
    }

    private static void crossing(GameTestHelper helper, Vec3 start, Vec3 direction, float yaw, float pitch) {
        var player = player(helper, start);
        var dash = start(player, yaw, pitch);
        boolean entered = false, exited = false;
        for (int i = 0; i < 12; i++) {
            dash.tick(player);
            entered |= dash.dreaming();
            if (entered && !dash.dreaming()) {
                exited = true;
                break;
            }
        }
        helper.assertTrue(entered, "Dream entry failed for direction " + direction);
        helper.assertTrue(exited, "Dream exit failed for direction " + direction);
        helper.assertTrue(player.position().subtract(helper.absoluteVec(start)).dot(direction) > 3,
                "Traversal must exit on the opposite side: " + direction);
        helper.assertTrue(DashPhysics.clear(player, player.getBoundingBox()), "Player must exit into a clear position");
    }

    private static void fillDream(GameTestHelper helper, BlockPos min, BlockPos max) {
        for (var pos : BlockPos.betweenClosed(min, max)) {
            helper.setBlock(pos, DashBlocks.block(DashBlocks.Kind.DREAM));
        }
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 position) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dream-test"));
        player.setNoGravity(true);
        player.setPos(helper.absoluteVec(position));
        player.setOnGround(false);
        return player;
    }

    private static ServerDash start(ServerPlayer player, float yaw, float pitch) {
        var dash = ServerDash.get(player);
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, yaw, pitch, 0));
        return dash;
    }
}
