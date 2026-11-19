package com.oritong.no_ultra_dash_please;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WaterDashGameTests {
    @GameTest(template = "empty")
    public static void shallowFlowingWaterRefillsWithoutGroundOrDelay(GameTestHelper helper) {
        var player = player(helper, new Vec3(3.5, 5, 3.5));
        var dash = ServerDash.get(player);
        start(player);
        dash.tick(player);
        dash.interrupt(player);
        helper.assertTrue(!player.isInWater(), "Fixture must spend its dash in dry air");
        helper.setBlock(3, 3, 3, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 4));
        player.setPos(helper.absoluteVec(new Vec3(3.5, 3.1, 3.5)));
        player.baseTick();
        helper.assertTrue(player.isInWater() && !player.isUnderWater() && !player.isSwimming(),
                "Only the feet should touch shallow flowing water");
        helper.assertTrue(!DashPhysics.grounded(player), "Water refill must not require ground support");
        boolean groundRefill = DashTuning.GROUND_REFILL.get();
        try {
            DashTuning.GROUND_REFILL.set(false);
            // The prior dash still has one tick of refill delay: water must bypass it.
            dash.tick(player);
            helper.assertTrue(!dash.useRefill(), "Touching water must immediately restore all charges");
        } finally {
            DashTuning.GROUND_REFILL.set(groundRefill);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shallowWaterDashDoesNotSpendCharges(GameTestHelper helper) {
        pool(helper, 3);
        var player = player(helper, new Vec3(3.5, 3.75, 2.5));
        player.baseTick();
        helper.assertTrue(player.isInWater() && !player.isUnderWater() && !player.isSwimming(),
                "Shallow-water dashing must not depend on eyes or swimming pose");
        var dash = ServerDash.get(player);
        double before = player.getZ();
        start(player);
        dash.tick(player);
        helper.assertTrue(player.getZ() > before + 0.7, "The shallow-water dash must actually launch");
        helper.assertTrue(!dash.useRefill(), "A shallow-water dash must leave charges full");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repeatedUnderwaterDashesKeepChargesForExit(GameTestHelper helper) {
        pool(helper, 6);
        var center = helper.absoluteVec(new Vec3(3.5, 3.1, 2.5));
        var player = player(helper, new Vec3(3.5, 3.1, 2.5));
        var dash = ServerDash.get(player);
        for (int attempt = 0; attempt < 3; attempt++) {
            player.setPos(center);
            player.baseTick();
            player.baseTick();
            helper.assertTrue(player.isUnderWater(), "Fixture must be fully submerged");
            start(player);
            dash.tick(player);
            helper.assertTrue(player.getZ() > center.z + 0.7, "Each underwater dash must launch");
            helper.assertTrue(!dash.useRefill(), "Repeated underwater dashes must preserve charges");
            dash.interrupt(player);
            for (int tick = 0; tick < DashTuning.COOLDOWN.get() + 1; tick++) dash.tick(player);
        }
        player.setPos(helper.absoluteVec(new Vec3(7.5, 3.1, 2.5)));
        player.baseTick();
        helper.assertTrue(!player.isInWater(), "Exit fixture must be in dry air");
        double before = player.getZ();
        start(player);
        dash.tick(player);
        helper.assertTrue(player.getZ() > before + 0.9, "The retained charge must work after leaving water: before="
                + before + ", after=" + player.getZ() + ", velocity=" + player.getDeltaMovement()
                + ", eligible=" + DashPhysics.eligible(player));
        helper.assertTrue(dash.useRefill(), "A dry-air dash must still consume a charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lavaDoesNotRestoreSpentDash(GameTestHelper helper) {
        var player = player(helper, new Vec3(3.5, 5, 3.5));
        var dash = ServerDash.get(player);
        start(player);
        dash.tick(player);
        dash.interrupt(player);
        helper.setBlock(3, 3, 3, Blocks.LAVA);
        player.setPos(helper.absoluteVec(new Vec3(3.5, 3.1, 3.5)));
        player.baseTick();
        helper.assertTrue(player.isInLava() && !player.isInWater(), "Fixture must touch lava only");
        dash.tick(player);
        helper.assertTrue(dash.useRefill(), "The water refill rule must not also refill lava contact");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 position) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "water-test"));
        player.setNoGravity(true);
        player.setPos(helper.absoluteVec(position));
        player.setOnGround(false);
        return player;
    }

    private static void pool(GameTestHelper helper, int top) {
        for (var pos : BlockPos.betweenClosed(1, 3, 1, 6, top, 6)) helper.setBlock(pos, Blocks.WATER);
    }

    private static void start(ServerPlayer player) {
        ServerDash.get(player).input(player, new DashNetwork.Input(DashNetwork.DASH, 0, 0, 0));
    }
}
