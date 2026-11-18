package com.oritong.no_ultra_dash_please;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID)
public final class RefillCrystalMigration {
    private RefillCrystalMigration() { }

    @SubscribeEvent
    static void load(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk) {
            ensureEntities(chunk);
        }
    }

    // Earlier crystals and dream blocks have no saved block entity. Only visit relevant sections.
    static void ensureEntities(LevelChunk chunk) {
        var sections = chunk.getSections();
        var pos = new BlockPos.MutableBlockPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (!section.maybeHas(state -> state.getBlock() instanceof RefillCrystalBlock
                    || state.getBlock() instanceof DreamBlock)) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var block = section.getBlockState(x, y, z).getBlock();
                        if (block instanceof RefillCrystalBlock || block instanceof DreamBlock) {
                            pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            if (chunk.getBlockEntity(pos) == null) {
                                chunk.getBlockEntity(pos.immutable(), LevelChunk.EntityCreationType.IMMEDIATE);
                                chunk.getLevel().getLightEngine().checkBlock(pos.immutable());
                                chunk.setUnsaved(true);
                            }
                        }
                    }
                }
            }
        }
    }
}
