package com.oritong.no_ultra_dash_please;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RefillCrystalGameTests {
    private static final BlockPos CRYSTAL = new BlockPos(2, 3, 2);

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void sharedCooldownAndReuse(GameTestHelper helper) {
        helper.setBlock(CRYSTAL, DashBlocks.block(DashBlocks.Kind.REFILL));
        ServerPlayer first = player(helper);
        ServerPlayer second = player(helper);
        spendDash(helper, first);
        spendDash(helper, second);
        touch(helper, first);
        assertPhase(helper, RefillCrystalBlock.Phase.EMPTY);
        helper.assertTrue(!ServerDash.get(first).useRefill(), "First player must receive a full refill");
        touch(helper, second);
        helper.assertTrue(ServerDash.get(second).useRefill(), "Empty crystal must not refill another player");
        helper.runAfterDelay(47, () -> assertPhase(helper, RefillCrystalBlock.Phase.EMPTY));
        helper.runAfterDelay(49, () -> assertPhase(helper, RefillCrystalBlock.Phase.REGENERATING));
        helper.runAfterDelay(50, () -> {
            assertPhase(helper, RefillCrystalBlock.Phase.READY);
            spendDash(helper, first);
            touch(helper, first);
            assertPhase(helper, RefillCrystalBlock.Phase.EMPTY);
            helper.assertTrue(!ServerDash.get(first).useRefill(), "Same player must be able to reuse a regenerated crystal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void fullPlayerDoesNotConsume(GameTestHelper helper) {
        helper.setBlock(CRYSTAL, DashBlocks.block(DashBlocks.Kind.REFILL));
        // No prior dash tick: freshly joined players also start with full charges.
        touch(helper, player(helper));
        assertPhase(helper, RefillCrystalBlock.Phase.READY);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void protectionLightAndOldWorldMigration(GameTestHelper helper) {
        helper.setBlock(CRYSTAL, DashBlocks.block(DashBlocks.Kind.REFILL));
        var level = helper.getLevel();
        var pos = helper.absolutePos(CRYSTAL);
        var state = level.getBlockState(pos);
        helper.assertTrue(state.getDestroySpeed(level, pos) == -1, "Crystal must be unmineable");
        helper.assertTrue(state.getBlock().getExplosionResistance() >= 3_600_000, "Crystal must resist explosions");
        helper.assertTrue(state.getPistonPushReaction() == PushReaction.BLOCK, "Pistons must not move or break crystals");
        helper.assertTrue(state.getLightEmission(level, pos) == 12, "Ready crystal must illuminate the surroundings");
        helper.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "Crystal must remain passable");
        level.setBlock(pos, state.setValue(RefillCrystalBlock.PHASE, RefillCrystalBlock.Phase.EMPTY), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(pos).getLightEmission(level, pos) == 2, "Empty outline must retain a faint light");
        var chunk = level.getChunkAt(pos);
        chunk.removeBlockEntity(pos);
        helper.assertTrue(chunk.getBlockEntity(pos) == null, "Migration fixture must have no saved block entity");
        RefillCrystalMigration.ensureEntities(chunk);
        helper.assertTrue(chunk.getBlockEntity(pos) instanceof RefillCrystalBlock.CrystalEntity,
                "Crystals from older worlds must acquire a renderer block entity");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setNoGravity(true);
        player.getAbilities().flying = false;
        return player;
    }

    private static void spendDash(GameTestHelper helper, ServerPlayer player) {
        var pos = helper.absolutePos(new BlockPos(5, 4, 5));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setOnGround(false);
        var dash = ServerDash.get(player);
        // Drive the actual airborne input path, including the dash cooldown.
        for (int i = 0; i < 12; i++) {
            dash.tick(player);
        }
        dash.input(player, new DashNetwork.Input(DashNetwork.DASH, 0, 0, 0));
        dash.tick(player);
        dash.interrupt(player);
    }

    private static void touch(GameTestHelper helper, ServerPlayer player) {
        var pos = helper.absolutePos(CRYSTAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var state = helper.getLevel().getBlockState(pos);
        state.entityInside(helper.getLevel(), pos, player);
        player.setPos(pos.getX() + 5, pos.getY() + 2, pos.getZ() + 5);
    }

    private static void assertPhase(GameTestHelper helper, RefillCrystalBlock.Phase expected) {
        var actual = helper.getBlockState(CRYSTAL).getValue(RefillCrystalBlock.PHASE);
        helper.assertTrue(actual == expected, "Expected crystal phase " + expected + " but was " + actual);
    }
}
