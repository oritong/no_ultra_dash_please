package com.oritong.no_ultra_dash_please;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

public final class DashPhysics {
    static final double EPSILON = 1.0e-5;
    static final double SUPPORT_DEPTH = 0.05;
    private static final double CORRECTION_STEP = 0.0625;
    private static final double WALL_REACH = 0.18;
    private static final List<Vec3> WALLS = List.of(new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
            new Vec3(0, 0, 1), new Vec3(0, 0, -1));

    private DashPhysics() {}

    static Vec3 aim(float yaw, float pitch) {
        int band = (pitch < 0 ? -1 : 1) * Math.min(2, Mth.floor(Math.abs(pitch) / 45 + 0.5F));
        if (Math.abs(band) == 2) {
            return new Vec3(0, -Integer.signum(band), 0);
        }
        Vec3 forward = Vec3.directionFromRotation(0, Mth.wrapDegrees(yaw));
        return new Vec3(forward.x, -band, forward.z).normalize();
    }

    static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0, vector.z);
    }

    static Vec3 limit(Vec3 speed, double maximum) {
        double length = speed.horizontalDistance();
        return length > maximum ? horizontal(speed).scale(maximum / length).add(0, speed.y, 0) : speed;
    }

    static Vec3 startVelocity(Vec3 aim, Vec3 previous, double speed, boolean wet) {
        Vec3 forward = horizontal(aim).normalize();
        double projection = previous.dot(forward);
        Vec3 base = aim.scale(speed);
        if (projection > base.horizontalDistance()) {
            base = forward.scale(projection).add(0, base.y, 0);
        }
        return wet ? base.scale(0.75) : base;
    }

    static boolean eligible(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isAutoSpinAttack()
                && !player.isInLava() && !player.isInPowderSnow && !player.noPhysics;
    }

    static boolean grounded(Player player) {
        return resolve(player, player.getBoundingBox(), new Vec3(0, -SUPPORT_DEPTH, 0)).y > -SUPPORT_DEPTH + EPSILON;
    }

    static boolean refillable(Player player) {
        if (player.isInWater() || player.onClimbable()) {
            return false;
        }
        AABB feet = player.getBoundingBox().move(0, -SUPPORT_DEPTH, 0);
        boolean blockSupport = false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(feet.minX, feet.minY, feet.minZ),
                BlockPos.containing(feet.maxX, feet.minY + 0.12, feet.maxZ))) {
            var state = player.level().getBlockState(pos);
            if (state.is(DashBlocks.NO_REFILL) || state.is(BlockTags.FIRE)) {
                return false;
            }
            var shape = state.getCollisionShape(player.level(), pos, CollisionContext.of(player));
            if (Shapes.joinIsNotEmpty(Shapes.create(feet), shape.move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) {
                blockSupport = true;
            }
        }
        return blockSupport;
    }

    static Vec3 wall(Player player, Vec3 preference) {
        Vec3 best = Vec3.ZERO;
        double score = -Double.MAX_VALUE;
        AABB box = player.getBoundingBox().deflate(0, 0.075, 0);
        for (Vec3 side : WALLS) {
            Vec3 probe = side.scale(WALL_REACH);
            AABB shifted = box.move(probe);
            if (!insideBorder(player, shifted) || !loaded(player, shifted)) {
                continue;
            }
            for (var shape : player.level().getBlockCollisions(player, shifted)) {
                if (!shape.isEmpty() && side.dot(preference) > score) {
                    best = side;
                    score = side.dot(preference);
                }
            }
        }
        return best;
    }

    static boolean different(Vec3 first, Vec3 second) {
        return first.distanceToSqr(second) > EPSILON * EPSILON;
    }

    static Vec3 resolve(Player player, AABB box, Vec3 motion) {
        return Entity.collideBoundingBox(player, motion, box, player.level(),
                player.level().getEntityCollisions(player, box.expandTowards(motion)));
    }

    static boolean loaded(Player player, AABB box) {
        var level = player.level();
        int endX = SectionPos.blockToSectionCoord(Mth.floor(box.maxX));
        int endZ = SectionPos.blockToSectionCoord(Mth.floor(box.maxZ));
        for (int chunkX = SectionPos.blockToSectionCoord(Mth.floor(box.minX)); chunkX <= endX; chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(box.minZ)); chunkZ <= endZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean clear(Player player, AABB box) {
        return insideBorder(player, box) && loaded(player, box)
                && player.level().noCollision(player, box);
    }

    private static boolean insideBorder(Player player, AABB box) {
        var border = player.level().getWorldBorder();
        return box.minX >= border.getMinX() && box.maxX <= border.getMaxX()
                && box.minZ >= border.getMinZ() && box.maxZ <= border.getMaxZ();
    }

    static boolean correct(Player player, Vec3 motion, boolean horizontalBlocked, boolean verticalBlocked) {
        Vec3 flat = horizontal(motion);
        Vec3 across = new Vec3(-flat.z, 0, flat.x).normalize();
        if (across.equals(Vec3.ZERO)) {
            Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
            across = new Vec3(-facing.z, 0, facing.x);
        }
        AABB box = player.getBoundingBox();
        for (int step = 1; step <= 4; step++) {
            double distance = CORRECTION_STEP * step;
            if (horizontalBlocked && motion.y >= -SUPPORT_DEPTH && tryCorrection(player, box, motion, new Vec3(0, distance, 0))) {
                return true;
            }
            if (verticalBlocked || horizontalBlocked && !grounded(player)) {
                if (tryCorrection(player, box, motion, across.scale(distance))
                        || tryCorrection(player, box, motion, across.scale(-distance))) {
                    return true;
                }
                Vec3 perpendicular = new Vec3(across.z, 0, -across.x);
                if (flat.lengthSqr() < EPSILON && (tryCorrection(player, box, motion, perpendicular.scale(distance))
                        || tryCorrection(player, box, motion, perpendicular.scale(-distance)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryCorrection(Player player, AABB box, Vec3 motion, Vec3 correction) {
        AABB shifted = box.move(correction);
        if (!clear(player, shifted) || different(resolve(player, box, correction), correction)
                || different(resolve(player, shifted, motion), motion)) {
            return false;
        }
        player.setPos(player.position().add(correction));
        return true;
    }

    static Contact contact(Player player, AABB swept) {
        Contact best = null;
        double distance = Double.MAX_VALUE;
        var volume = Shapes.create(swept);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(swept.minX, swept.minY, swept.minZ),
                BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) {
            var state = player.level().getBlockState(pos);
            DashBlocks.Kind kind = DashBlocks.kind(state);
            if (kind == null || kind.nonSolid()) {
                continue;
            }
            var shape = state.getCollisionShape(player.level(), pos, CollisionContext.of(player)).move(pos.getX(), pos.getY(), pos.getZ());
            double squared = player.distanceToSqr(pos.getCenter());
            if (squared < distance && Shapes.joinIsNotEmpty(volume, shape, BooleanOp.AND)) {
                best = new Contact(kind, pos.immutable());
                distance = squared;
            }
        }
        return best;
    }

    static DreamSpace dreamSpace(Player player, AABB box) {
        var level = player.level();
        if (!loaded(player, box) || !insideBorder(player, box)
                || !level.getEntityCollisions(player, box).isEmpty()) {
            return DreamSpace.BLOCKED;
        }
        boolean tagged = false;
        var context = CollisionContext.of(player);
        var volume = Shapes.create(box.deflate(EPSILON));
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX - 1, box.minY - 1, box.minZ - 1),
                BlockPos.containing(box.maxX + 1, box.maxY + 1, box.maxZ + 1))) {
            if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
                return DreamSpace.BLOCKED;
            }
            var state = level.getBlockState(pos);
            if (!Shapes.joinIsNotEmpty(volume, state.getCollisionShape(level, pos, context)
                    .move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) {
                continue;
            }
            if (!state.is(DashBlocks.DREAM)) {
                return DreamSpace.BLOCKED;
            }
            tagged = true;
        }
        return tagged ? DreamSpace.INSIDE : DreamSpace.CLEAR;
    }

    static void move(Player player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.move(MoverType.SELF, motion);
    }

    public static boolean duck(Player player) {
        AABB box = player.getDimensions(Pose.CROUCHING).makeBoundingBox(player.position());
        if (player.getForcedPose() == null && clear(player, box)) {
            player.setPose(Pose.CROUCHING);
            return true;
        }
        return false;
    }

    enum DreamSpace { CLEAR, INSIDE, BLOCKED }

    record Contact(DashBlocks.Kind kind, BlockPos position) {}
}
