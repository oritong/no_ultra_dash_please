package com.oritong.no_ultra_dash_please;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Persistent timers shared by an entire four-connected, same-height platform. */
@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID)
public final class CrumbleGroups extends SavedData {
    static final int MIN_WARNING_TICKS = 4; // Celeste: 0.2 s, then up to another 0.4 s on top.
    static final int MAX_WARNING_TICKS = 12;
    static final int RETURN_TICKS = 40;
    static final String DATA_ID = NoUltraDashPlease.MOD_ID + "_crumble_groups";
    static final Factory<CrumbleGroups> FACTORY = new Factory<>(CrumbleGroups::new, CrumbleGroups::load);
    private final List<Group> groups = new ArrayList<>();

    private CrumbleGroups() { }

    static Set<BlockPos> connected(ServerLevel level, BlockPos start) {
        var result = new LinkedHashSet<BlockPos>();
        var pending = new ArrayDeque<BlockPos>();
        pending.add(start.immutable());
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (result.contains(pos) || !level.hasChunkAt(pos)
                    || !(level.getBlockState(pos).getBlock() instanceof CrumblePlatformBlock)) continue;
            result.add(pos);
            for (Direction direction : Direction.Plane.HORIZONTAL) pending.addLast(pos.relative(direction));
        }
        return result;
    }

    static void begin(ServerLevel level, BlockPos origin) {
        var data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
        // Repeated footsteps must neither restart the countdown nor play stacked sounds.
        if (data.groups.stream().anyMatch(group -> group.positions.contains(origin))) return;
        Set<BlockPos> positions = connected(level, origin);
        if (positions.isEmpty()) return;
        long now = level.getGameTime();
        var group = new Group(positions, origin.immutable(), now, now + MIN_WARNING_TICKS,
                level.getBlockState(origin).getValue(CrumblePlatformBlock.PHASE) == CrumblePlatformBlock.Phase.COLLAPSED);
        // A newly placed bridge may join running platforms: keep their earliest warning.
        var iterator = data.groups.iterator();
        while (iterator.hasNext()) {
            Group other = iterator.next();
            if (other.positions.stream().noneMatch(positions::contains)) continue;
            group.positions.addAll(other.positions);
            group.started = Math.min(group.started, other.started);
            group.collapsed |= other.collapsed;
            iterator.remove();
        }
        group.next = group.collapsed ? now + RETURN_TICKS : Math.max(now, group.started + MIN_WARNING_TICKS);
        data.groups.add(group);
        changePhase(level, group, group.collapsed ? CrumblePlatformBlock.Phase.COLLAPSED : CrumblePlatformBlock.Phase.CRUMBLING);
        level.playSound(null, origin, DashSounds.PLATFORM_DISINTEGRATE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        data.setDirty();
    }

    @SubscribeEvent
    static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var data = level.getDataStorage().get(FACTORY, DATA_ID);
            if (data != null) data.advance(level);
        }
    }

    void advance(ServerLevel level) {
        long now = level.getGameTime();
        var iterator = groups.iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            if (now < group.next) continue;
            // Do not generate or force-load terrain just to advance a platform animation.
            if (group.positions.stream().anyMatch(pos -> !level.hasChunkAt(pos))) {
                group.next = now + 20;
                setDirty();
                continue;
            }
            group.positions.removeIf(pos -> !(level.getBlockState(pos).getBlock() instanceof CrumblePlatformBlock));
            if (group.positions.isEmpty()) {
                iterator.remove();
            } else if (!group.collapsed) {
                if (now < group.started + MAX_WARNING_TICKS && onTop(level, group)) {
                    group.next = now + 1;
                } else {
                    changePhase(level, group, CrumblePlatformBlock.Phase.COLLAPSED);
                    group.collapsed = true;
                    group.next = now + RETURN_TICKS;
                    for (BlockPos pos : group.positions) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                        level.getBlockState(pos).setValue(CrumblePlatformBlock.PHASE, CrumblePlatformBlock.Phase.NORMAL)),
                                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.35, 0.08, 0.35, 0.03);
                    }
                }
            } else if (occupied(level, group)) {
                group.next = now + 1;
            } else {
                changePhase(level, group, CrumblePlatformBlock.Phase.NORMAL);
                level.playSound(null, group.origin, DashSounds.PLATFORM_RETURN.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
                iterator.remove();
            }
            setDirty();
        }
    }

    private static boolean onTop(ServerLevel level, Group group) {
        double top = group.origin.getY() + 0.5;
        return !level.getEntitiesOfClass(Player.class, group.bounds().inflate(0, 0.1, 0), player ->
                player.isAlive() && !player.isSpectator() && Math.abs(player.getY() - top) < 0.08
                        && group.positions.stream().anyMatch(pos -> player.getBoundingBox().intersects(
                        new AABB(pos.getX(), top - 0.02, pos.getZ(), pos.getX() + 1, top + 0.08, pos.getZ() + 1)))).isEmpty();
    }

    private static boolean occupied(ServerLevel level, Group group) {
        return !level.getEntitiesOfClass(LivingEntity.class, group.bounds(), entity -> entity.isAlive()
                && !entity.isSpectator() && group.positions.stream().anyMatch(pos ->
                entity.getBoundingBox().intersects(CrumblePlatformBlock.SLAB.bounds().move(pos)))).isEmpty();
    }

    private static void changePhase(ServerLevel level, Group group, CrumblePlatformBlock.Phase phase) {
        for (BlockPos pos : group.positions) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof CrumblePlatformBlock) {
                level.setBlock(pos, state.setValue(CrumblePlatformBlock.PHASE, phase), Block.UPDATE_CLIENTS);
                if (phase == CrumblePlatformBlock.Phase.CRUMBLING
                        && level.getBlockEntity(pos) instanceof CrumblePlatformBlock.ShakeEntity shake) {
                    shake.startWarning(group.started, group.origin.asLong());
                }
            }
        }
        // Finish the whole phase change before rebuilding each multipart seam.
        for (BlockPos pos : group.positions) {
            if (level.getBlockState(pos).getBlock() instanceof CrumblePlatformBlock block) block.refresh(level, pos);
        }
    }

    static void restoreWarning(ServerLevel level, BlockPos pos, CrumblePlatformBlock.ShakeEntity shake) {
        var data = level.getDataStorage().get(FACTORY, DATA_ID);
        if (data != null) {
            for (Group group : data.groups) {
                if (!group.collapsed && group.positions.contains(pos)) {
                    shake.startWarning(group.started, group.origin.asLong());
                    return;
                }
            }
        }
        shake.startWarning(level.getGameTime(), pos.asLong());
    }

    static CrumbleGroups load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new CrumbleGroups();
        for (Tag entry : tag.getList("Groups", Tag.TAG_COMPOUND)) {
            var saved = (CompoundTag) entry;
            var positions = new LinkedHashSet<BlockPos>();
            for (long packed : saved.getLongArray("Positions")) positions.add(BlockPos.of(packed));
            if (!positions.isEmpty()) result.groups.add(new Group(positions, BlockPos.of(saved.getLong("Origin")),
                    saved.getLong("Started"), saved.getLong("Next"), saved.getBoolean("Collapsed")));
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (Group group : groups) {
            var saved = new CompoundTag();
            saved.putLongArray("Positions", group.positions.stream().mapToLong(BlockPos::asLong).toArray());
            saved.putLong("Origin", group.origin.asLong());
            saved.putLong("Started", group.started);
            saved.putLong("Next", group.next);
            saved.putBoolean("Collapsed", group.collapsed);
            list.add(saved);
        }
        tag.put("Groups", list);
        return tag;
    }

    private static final class Group {
        final Set<BlockPos> positions;
        final BlockPos origin;
        long started;
        long next;
        boolean collapsed;

        Group(Set<BlockPos> positions, BlockPos origin, long started, long next, boolean collapsed) {
            this.positions = positions;
            this.origin = origin;
            this.started = started;
            this.next = next;
            this.collapsed = collapsed;
        }

        AABB bounds() {
            AABB bounds = CrumblePlatformBlock.SLAB.bounds().move(origin);
            for (BlockPos pos : positions) bounds = bounds.minmax(CrumblePlatformBlock.SLAB.bounds().move(pos));
            return bounds;
        }
    }
}
