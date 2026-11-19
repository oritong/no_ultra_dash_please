package com.oritong.no_ultra_dash_please;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NoUltraDashPlease.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreamOcclusionGameTests {
    @GameTest(template = "empty")
    public static void adjacentWallFacesRemainVisibleFromInsideDream(GameTestHelper helper) {
        var center = new BlockPos(3, 3, 3);
        helper.setBlock(center, DashBlocks.block(DashBlocks.Kind.DREAM));
        var level = helper.getLevel();
        var dreamPos = helper.absolutePos(center);
        var dream = level.getBlockState(dreamPos);
        for (Direction direction : Direction.values()) {
            var wall = center.relative(direction);
            helper.setBlock(wall, Blocks.STONE);
            var wallPos = helper.absolutePos(wall);
            helper.assertTrue(Block.shouldRenderFace(level.getBlockState(wallPos), level, wallPos,
                    direction.getOpposite(), dreamPos), "Ordinary wall face must remain rendered beside dream: " + direction);
        }
        helper.assertTrue(!dream.canOcclude() && !dream.isSolidRender(level, dreamPos),
                "Dream geometry must not occlude adjacent faces or chunk sections from the inside");
        helper.assertTrue(Block.isShapeFullBlock(dream.getCollisionShape(level, dreamPos)),
                "Changing visual occlusion must preserve the solid gameplay collision");
        helper.succeed();
    }
}
