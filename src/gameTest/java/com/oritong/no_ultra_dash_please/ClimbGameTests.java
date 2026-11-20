package com.oritong.no_ultra_dash_please;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClimbGameTests {
    @GameTest(template = "empty")
    public static void rightClickGrabsClimbsDescendsAndReleases(GameTestHelper helper) {
        var player = wallPlayer(helper);
        var dash = ServerDash.get(player);
        input(player, DashNetwork.GRAB);
        dash.tick(player);
        double y = player.getY();
        helper.assertTrue(dash.climbing(), "Unsigned input bit 128 must grab the wall with both hands empty");
        tick(player, 3);
        helper.assertTrue(player.getY() == y, "Holding right click without movement must hold height");
        input(player, DashNetwork.GRAB | DashNetwork.FORWARD);
        tick(player, 4);
        helper.assertTrue(player.getY() > y + 0.7, "Forward while grabbing must climb upwards");
        double top = player.getY();
        input(player, DashNetwork.GRAB | DashNetwork.BACK);
        tick(player, 2);
        helper.assertTrue(player.getY() < top - 0.6 && dash.climbing(), "Back while grabbing must descend the wall");
        input(player, 0);
        dash.tick(player);
        helper.assertTrue(!dash.climbing(), "Releasing right click must immediately leave climb mode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eitherOccupiedHandBlocksGrabAndInventoryChangesReleaseIt(GameTestHelper helper) {
        var player = wallPlayer(helper);
        var dash = ServerDash.get(player);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(hand, new ItemStack(Items.STONE));
            input(player, DashNetwork.GRAB);
            dash.tick(player);
            helper.assertTrue(!dash.climbing(), "Server must reject occupied " + hand + " even if the client requests grab");
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        input(player, DashNetwork.GRAB);
        dash.tick(player);
        helper.assertTrue(dash.climbing(), "Removing both held items must enable grab");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        dash.tick(player);
        helper.assertTrue(!dash.climbing(), "Changing the offhand must release an existing grab without another input packet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exactEmptyHandTagExemptsEitherHand(GameTestHelper helper) {
        var player = wallPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.FEATHER));
        helper.assertTrue(ClimbHands.EMPTY_HAND.location().toString().equals("no_ultra_dash:empty_hand"), "Use the user's exact tag namespace");
        helper.assertTrue(ClimbHands.empty(player), "Both tagged items must be treated as empty hands");
        input(player, DashNetwork.GRAB | DashNetwork.FORWARD);
        ServerDash.get(player).tick(player);
        helper.assertTrue(ServerDash.get(player).climbing(), "Tag-exempt items in both hands must allow climbing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        ServerDash.get(player).tick(player);
        helper.assertTrue(!ServerDash.get(player).climbing(), "Replacing a tagged item with an ordinary item must release grab");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void groundStartAndClimbJumpUseHeldRightClick(GameTestHelper helper) {
        var player = wallPlayer(helper);
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 5; z++) helper.setBlock(x, 2, z, Blocks.STONE);
        helper.assertTrue(DashPhysics.grounded(player), "Fixture starts on the ground beside the wall");
        input(player, DashNetwork.GRAB | DashNetwork.FORWARD);
        var dash = ServerDash.get(player);
        dash.tick(player);
        helper.assertTrue(dash.climbing() && player.getY() > helper.absolutePos(new BlockPos(0, 3, 0)).getY(),
                "Holding forward and right click must start climbing directly from the ground");
        input(player, DashNetwork.GRAB | DashNetwork.JUMP);
        dash.tick(player);
        helper.assertTrue(!dash.climbing() && player.getDeltaMovement().y > 0.4,
                "Jump while holding grab must trigger the Celeste climb jump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void originalStaminaCostsAndDownwardFreeTravel(GameTestHelper helper) {
        var player = wallPlayer(helper);
        var dash = ServerDash.get(player);
        input(player, DashNetwork.GRAB);
        tick(player, 220);
        helper.assertTrue(dash.climbing(), "110 stamina at 10 per second must support 11 seconds of hanging");
        dash.tick(player);
        helper.assertTrue(!dash.climbing(), "The next tick must release after exhausting hang stamina");
        dash.interrupt(player);
        dash.useRefill();
        input(player, DashNetwork.GRAB | DashNetwork.BACK);
        for (int tick = 0; tick < 240; tick++) {
            player.setPos(helper.absoluteVec(new Vec3(3.68, 3, 3.5)));
            dash.tick(player);
        }
        helper.assertTrue(dash.climbing() && !dash.useRefill(), "Climbing down must not consume stamina");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noWallAndDisabledClimbingCannotGrab(GameTestHelper helper) {
        var player = wallPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 3, 3.5)));
        input(player, DashNetwork.GRAB);
        ServerDash.get(player).tick(player);
        helper.assertTrue(!ServerDash.get(player).climbing(), "Right click in the air must not suspend the player");
        player.setPos(helper.absoluteVec(new Vec3(3.68, 3, 3.5)));
        boolean enabled = DashTuning.WALL_JUMPS.get();
        try {
            DashTuning.WALL_JUMPS.set(false);
            ServerDash.get(player).tick(player);
            helper.assertTrue(!ServerDash.get(player).climbing(), "The existing wall movement config must also disable right-click grab");
        } finally { DashTuning.WALL_JUMPS.set(enabled); }
        input(player, DashNetwork.SNEAK);
        ServerDash.get(player).tick(player);
        helper.assertTrue(ServerDash.get(player).climbing(), "The existing sneak grab must remain available");
        helper.succeed();
    }

    private static ServerPlayer wallPlayer(GameTestHelper helper) {
        for (int y = 0; y < 8; y++) for (int z = 1; z <= 5; z++) helper.setBlock(4, y, z, Blocks.STONE);
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "climb-test"));
        player.setPos(helper.absoluteVec(new Vec3(3.68, 3, 3.5)));
        player.setYRot(-90);
        player.setXRot(0);
        player.setNoGravity(true);
        player.setOnGround(false);
        return player;
    }

    private static void input(ServerPlayer player, int buttons) {
        ServerDash.get(player).input(player, new DashNetwork.Input(buttons, -90, 0, 0));
    }

    private static void tick(ServerPlayer player, int count) {
        for (int tick = 0; tick < count; tick++) ServerDash.get(player).tick(player);
    }
}
