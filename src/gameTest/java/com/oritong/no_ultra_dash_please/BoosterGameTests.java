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
public final class BoosterGameTests {
    private static final BlockPos BUBBLE = new BlockPos(3, 4, 3);

    @GameTest(template = "empty")
    public static void greenBubbleCentersAndCarriesPlayer(GameTestHelper helper) {
        var player = enter(helper, DashBlocks.Kind.BOOSTER);
        var dash = ServerDash.get(player);
        tick(player, 2);
        helper.assertTrue(dash.bubbleMode() == 1, "Green bubble must wrap the waiting player");
        var center = helper.absolutePos(BUBBLE).getCenter();
        helper.assertTrue(player.position().add(0, player.getBbHeight() / 2, 0).distanceTo(center) < 0.01,
                "Waiting player must be drawn into the bubble center");
        helper.assertTrue(!dash.queueBooster(DashBlocks.Kind.RED_BOOSTER, helper.absolutePos(BUBBLE.east())),
                "A held player must not be stolen by an overlapping bubble");
        double before = player.getZ();
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, 0, 0, 0));
        tick(player, 1);
        helper.assertTrue(dash.bubbleMode() == 3, "Green membrane must follow the launched player");
        helper.assertTrue(player.getZ() > before + 0.9, "Green booster must carry the player forwards");
        tick(player, 4);
        helper.assertTrue(dash.bubbleMode() == 0, "Green membrane must release after the dash");
        helper.assertTrue(!dash.useRefill(), "Booster launch must leave refilled charges available");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void redBubbleAutoLaunchesAndStopsAtWall(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) helper.setBlock(x, y, 7, Blocks.STONE);
        }
        var player = enter(helper, DashBlocks.Kind.RED_BOOSTER);
        var dash = ServerDash.get(player);
        tick(player, 1);
        helper.assertTrue(dash.bubbleMode() == 2, "Red bubble must first wrap and hold the player");
        tick(player, 4);
        helper.assertTrue(dash.bubbleMode() == 4, "Red bubble must automatically launch without another key press");
        double previous = player.getZ();
        tick(player, 1);
        helper.assertTrue(dash.bubbleMode() == 4 && player.getZ() > previous + 0.9,
                "Red membrane must stay around the continuously moving player");
        for (int i = 0; i < 8 && dash.bubbleMode() != 0; i++) tick(player, 1);
        helper.assertTrue(dash.bubbleMode() == 0, "Red bubble and travel audio state must stop at the wall");
        helper.assertTrue(player.getBoundingBox().maxZ <= helper.absolutePos(new BlockPos(0, 0, 7)).getZ() + 1.0e-4,
                "Red booster must not carry players through ordinary walls");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interruptClearsBubbleBeforeOrdinaryDash(GameTestHelper helper) {
        var player = enter(helper, DashBlocks.Kind.RED_BOOSTER);
        var dash = ServerDash.get(player);
        tick(player, 5);
        helper.assertTrue(dash.bubbleMode() == 4, "Fixture must be travelling in a red bubble");
        dash.interrupt(player);
        helper.assertTrue(dash.bubbleMode() == 0 && !dash.ownsMovement(), "Teleport/cancel must clear bubble movement");
        player.setPos(helper.absoluteVec(new Vec3(3.5, 4, 1.5)));
        tick(player, 12);
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, 0, 0, 0));
        tick(player, 1);
        helper.assertTrue(dash.bubbleMode() == 0, "An ordinary dash must not inherit a prior booster membrane");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void floorBubbleDoesNotPullFeetIntoStone(GameTestHelper helper) {
        for (int x = 1; x < 6; x++) for (int z = 1; z < 6; z++) helper.setBlock(x, 3, z, Blocks.STONE);
        helper.setBlock(BUBBLE, DashBlocks.block(DashBlocks.Kind.BOOSTER));
        var player = player(helper, new Vec3(3.8, 4, 3.5));
        touch(helper, player);
        tick(player, 2);
        helper.assertTrue(ServerDash.get(player).bubbleMode() == 1, "Floor bubble must still hold the player");
        helper.assertTrue(player.getY() >= helper.absolutePos(new BlockPos(0, 4, 0)).getY(),
                "Centering must keep feet above the supporting floor");
        helper.assertTrue(DashPhysics.clear(player, player.getBoundingBox()), "Player must remain outside solid support");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bothOldBoosterColorsAcquireRendererEntities(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var kind : new DashBlocks.Kind[] {DashBlocks.Kind.BOOSTER, DashBlocks.Kind.RED_BOOSTER}) {
            helper.setBlock(BUBBLE, DashBlocks.block(kind));
            var pos = helper.absolutePos(BUBBLE);
            var state = level.getBlockState(pos);
            helper.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "Booster membrane must remain passable");
            helper.assertTrue(BoosterBlock.kind(state) == kind, "Red and green blocks must retain their correct color");
            var chunk = level.getChunkAt(pos);
            chunk.removeBlockEntity(pos);
            RefillCrystalMigration.ensureEntities(chunk);
            helper.assertTrue(chunk.getBlockEntity(pos) instanceof BoosterBlock.BubbleEntity,
                    "Old-world boosters must gain their animated renderer entity");
        }
        helper.succeed();
    }

    private static ServerPlayer enter(GameTestHelper helper, DashBlocks.Kind kind) {
        helper.setBlock(BUBBLE, DashBlocks.block(kind));
        var player = player(helper, new Vec3(3.8, 3.6, 3.5));
        touch(helper, player);
        return player;
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 pos) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "booster-test"));
        player.setPos(helper.absoluteVec(pos));
        player.setNoGravity(true);
        player.setOnGround(false);
        player.setYRot(0);
        player.setXRot(0);
        return player;
    }

    private static void touch(GameTestHelper helper, ServerPlayer player) {
        var pos = helper.absolutePos(BUBBLE);
        helper.getLevel().getBlockState(pos).entityInside(helper.getLevel(), pos, player);
    }

    private static void tick(ServerPlayer player, int times) {
        for (int i = 0; i < times; i++) {
            ServerDash.get(player).tick(player);
            player.doTick();
        }
    }
}
