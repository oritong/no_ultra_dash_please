package com.oritong.no_ultra_dash_please;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.oritong.no_ultra_dash_please.DashNetwork.*;

public final class ServerDash {
    private static final Map<ServerPlayer, ServerDash> PLAYERS = new WeakHashMap<>();
    private static final int MAX_BOOST_TICKS = 18;
    private static final int MAX_RED_TICKS = 80;
    private static final int MAX_DREAM_TICKS = 40;
    private static final int MIN_DREAM_TICKS = 2;
    private static final int MAX_RELEASE_WAIT = 40;

    private Phase phase = Phase.IDLE;
    private Vec3 direction = Vec3.ZERO;
    private Vec3 speed = Vec3.ZERO;
    private Vec3 dashAim = Vec3.ZERO;
    private Vec3 retained = Vec3.ZERO;
    private Vec3 climbAway = Vec3.ZERO;
    private Vec3 climbWall = Vec3.ZERO;
    private Vec3 platformSpeed = Vec3.ZERO;
    private Vec3 dreamEntrance;
    private Vec3 dreamEntryDirection = Vec3.ZERO;
    private boolean dreamReturning;
    private Vec3 lastGround;
    private DashPhysics.Contact spring;
    private DashBlocks.Kind booster;
    private BlockPos lastBooster;
    private BlockPos bubbleOrigin;
    private int bubbleTicks;
    private int lastBubbleVisual;
    private int charges = -1;
    private int maxCharges;
    private int lastCharges = -1;
    private int lastMaximum;
    private int lastCooldown;
    private double lastDashSpeed;
    private int lastDashTicks;
    private int cooldown;
    private int refillDelay;
    private int remaining;
    private int attack;
    private int retention;
    private int variableJump;
    private int climbBoost;
    private int stamina = 110;
    private int input;
    private int releaseId;
    private int releaseWait;
    private int surfaceCooldown;
    private int platformGrace;
    private int dreamMin;
    private long tick;
    private long jumpDeadline = -1;
    private long dashDeadline = -1;
    private long groundDeadline = -1;
    private boolean earnedRefill;
    private boolean duck;
    private boolean dreamExited;
    private boolean redAfterDream;
    private boolean autoJump;
    private boolean slid;
    private boolean inheritedSpeed;
    private double jumpSpeed;

    private ServerDash() {}

    public static ServerDash get(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player, ignored -> new ServerDash());
    }

    public boolean ownsMovement() {
        return phase != Phase.IDLE || releaseWait > 0;
    }

    public boolean ducking() {
        return duck;
    }

    public boolean dreaming() {
        return phase == Phase.DREAM && !dreamExited;
    }

    void input(ServerPlayer player, Input data) {
        if (releaseWait > 0 && data.release() == releaseId) {
            releaseWait = 0;
        }
        if (data.buttons() > 127 || !Float.isFinite(data.yaw()) || !Float.isFinite(data.pitch())) {
            return;
        }
        int updated = data.buttons() & ~DASH;
        if (!DashPhysics.eligible(player)) {
            input = updated;
            return;
        }
        if ((updated & JUMP) != 0 && (input & JUMP) == 0) {
            jumpDeadline = tick + 1L + DashTuning.BUFFER.get();
        }
        if ((data.buttons() & DASH) != 0) {
            dashAim = DashPhysics.aim(data.yaw(), data.pitch());
            dashDeadline = tick + 2;
        }
        input = updated;
    }

    void queueSpring(DashBlocks.Kind kind, BlockPos pos) {
        if (surfaceCooldown == 0) {
            spring = new DashPhysics.Contact(kind, pos.immutable());
        }
    }

    boolean queueBooster(DashBlocks.Kind kind, BlockPos pos) {
        if (surfaceCooldown > 0 || booster != null || phase == Phase.DREAM || pos.equals(lastBooster)) {
            return false;
        }
        booster = kind;
        lastBooster = pos.immutable();
        return true;
    }

    public boolean useRefill() {
        maxCharges = DashTuning.CHARGES.get();
        if (charges < 0) {
            charges = maxCharges;
        }
        if (charges == maxCharges && stamina == 110) {
            return false;
        }
        refill();
        return true;
    }

    boolean touchRefill(ServerPlayer player, BlockPos pos) {
        if (useRefill()) {
            effect(player, DashSounds.REFILL_TOUCH.get(), 1.0F);
            trail(player, ParticleTypes.END_ROD);
            return true;
        }
        return false;
    }

    public void inheritPlatform(Vec3 motion) {
        if (Double.isFinite(motion.x) && Double.isFinite(motion.y) && Double.isFinite(motion.z)) {
            platformSpeed = DashPhysics.limit(motion.with(Direction.Axis.Y,
                    Mth.clamp(motion.y, 0, DashTuning.SPEED.get() * (130.0 / 240.0))), DashTuning.SPEED.get() * (250.0 / 240.0));
            platformGrace = 3;
        }
    }

    public void interrupt(ServerPlayer player) {
        boolean notify = ownsMovement() || dashDeadline >= 0;
        phase = Phase.IDLE;
        releaseWait = 0;
        attack = 0;
        retention = 0;
        climbBoost = 0;
        variableJump = 0;
        autoJump = false;
        duck = false;
        dreamEntrance = null;
        dreamReturning = false;
        spring = null;
        booster = null;
        lastBooster = null;
        platformGrace = 0;
        earnedRefill = false;
        jumpDeadline = dashDeadline = groundDeadline = -1;
        lastGround = null;
        if (notify) {
            send(player, RESET);
        }
    }

    public boolean tick(ServerPlayer player) {
        tick++;
        updateTimers();
        maxCharges = DashTuning.CHARGES.get();
        charges = charges < 0 ? maxCharges : Math.min(charges, maxCharges);
        if (!DashPhysics.eligible(player)) {
            if (dreaming()) {
                retreatDream(player);
            }
            boolean wasMoving = ownsMovement();
            interrupt(player);
            syncIdle(player);
            return wasMoving;
        }

        boolean hasGround = phase != Phase.DREAM && DashPhysics.grounded(player);
        boolean safeGround = hasGround && DashPhysics.refillable(player);
        if (lastBooster != null && booster == null && phase != Phase.BOOSTER
                && !player.getBoundingBox().intersects(new AABB(lastBooster).inflate(0.25))) {
            lastBooster = null;
        }
        if (hasGround) {
            groundDeadline = tick + DashTuning.GRACE.get();
            lastGround = player.position();
            if (safeGround) {
                stamina = 110;
            }
        }
        if (refillDelay == 0 && DashTuning.GROUND_REFILL.get() && (safeGround || earnedRefill)) {
            refill();
        }

        if (releaseWait > 0) {
            releaseWait--;
            if (releaseWait == 0) {
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            }
            return true;
        }

        if (booster != null) {
            phase = Phase.BOOSTER;
            bubbleOrigin = lastBooster;
            redAfterDream = booster == DashBlocks.Kind.RED_BOOSTER;
            booster = null;
            remaining = 5;
            surfaceCooldown = 12;
            speed = Vec3.ZERO;
            duck = false;
            attack = retention = 0;
            refill();
            effect(player, (redAfterDream ? DashSounds.RED_BOOSTER_ENTER : DashSounds.BOOSTER_ENTER).get(), 1.0F);
        }
        applySpring(player);

        if (dashDeadline >= tick && (phase == Phase.BOOSTER || canDash())) {
            startDash(player, dashAim, phase == Phase.BOOSTER && redAfterDream, phase == Phase.BOOSTER);
        } else if (phase == Phase.BOOSTER && --remaining <= 0) {
            startDash(player, DashPhysics.aim(player.getYRot(), player.getXRot()), redAfterDream, true);
        } else if (dashDeadline >= 0 && dashDeadline <= tick) {
            dashDeadline = -1;
            if (phase == Phase.IDLE) {
                send(player, REJECT);
            }
        }

        if (phase == Phase.IDLE && jumpDeadline >= tick) {
            tryFreeJump(player, hasGround);
        }
        if (phase == Phase.IDLE && !hasGround) {
            tryGrab(player);
        }
        if (phase == Phase.IDLE) {
            syncIdle(player);
            return false;
        }

        Vec3 previous = player.position();
        switch (phase) {
            case DASH, RED -> stepDash(player, safeGround);
            case MOMENTUM -> stepMomentum(player, hasGround);
            case CLIMB -> stepClimb(player, hasGround);
            case DREAM -> stepDream(player);
            case SQUASH -> {
                if (--remaining <= 0) {
                    finish(player);
                }
            }
            case BOOSTER, IDLE -> { }
        }
        Vec3 displacement = player.position().subtract(previous);
        if (displacement.lengthSqr() > DashPhysics.EPSILON) {
            player.checkMovementStatistics(displacement.x, displacement.y, displacement.z);
        }
        player.setDeltaMovement(speed);
        if (phase != Phase.IDLE) {
            send(player, MOVING);
        }
        return true;
    }

    private boolean canDash() {
        return charges > 0 && cooldown == 0 && (phase == Phase.IDLE || phase == Phase.MOMENTUM
                || phase == Phase.CLIMB || phase == Phase.RED);
    }

    private void updateTimers() {
        bubbleTicks = Math.max(0, bubbleTicks - 1);
        cooldown = Math.max(0, cooldown - 1);
        refillDelay = Math.max(0, refillDelay - 1);
        surfaceCooldown = Math.max(0, surfaceCooldown - 1);
        attack = Math.max(0, attack - 1);
        platformGrace = Math.max(0, platformGrace - 1);
    }

    private void refill() {
        charges = maxCharges;
        stamina = 110;
        earnedRefill = false;
    }

    private void startDash(ServerPlayer player, Vec3 aim, boolean red, boolean boosted) {
        bubbleTicks = boosted && !red ? DashTuning.DURATION.get() : 0;
        dashDeadline = -1;
        if (!boosted && !player.isUnderWater() && !player.isSwimming()) {
            charges = Math.max(0, charges - 1);
        }
        Vec3 incoming = phase == Phase.IDLE ? player.getDeltaMovement() : speed;
        incoming = incoming.add(takePlatform());
        direction = aim;
        double projection = incoming.dot(DashPhysics.horizontal(aim).normalize());
        inheritedSpeed = projection > DashTuning.SPEED.get();
        slid = false;
        speed = DashPhysics.limit(DashPhysics.startVelocity(aim, incoming, DashTuning.SPEED.get(), player.isInWater()), limit());
        phase = red ? Phase.RED : Phase.DASH;
        remaining = red ? MAX_RED_TICKS : DashTuning.DURATION.get();
        attack = remaining + 2;
        cooldown = DashTuning.COOLDOWN.get();
        refillDelay = DashTuning.REFILL_DELAY.get();
        duck = player.isShiftKeyDown() && Math.abs(aim.y) < DashPhysics.EPSILON && DashPhysics.duck(player);
        variableJump = retention = climbBoost = 0;
        autoJump = false;
        earnedRefill = false;
        player.resetFallDistance();
        effect(player, (boosted ? (red ? DashSounds.RED_BOOSTER_DASH : DashSounds.BOOSTER_DASH) : DashSounds.DASH).get(), 1.0F);
        PacketDistributor.sendToPlayer(player, new DashStarted());
    }

    private void stepDash(ServerPlayer player, boolean safeBeforeMove) {
        Vec3 requested = speed;
        if (Math.abs(requested.y) < DashPhysics.EPSILON && DashPhysics.grounded(player)) {
            requested = requested.add(0, -DashPhysics.SUPPORT_DEPTH, 0);
        }
        Vec3 clipped = DashPhysics.resolve(player, player.getBoundingBox(), requested);
        boolean horizontalBlocked = horizontalBlocked(requested, clipped);
        boolean verticalBlocked = Math.abs(requested.y - clipped.y) > DashPhysics.EPSILON;
        boolean verticalAttack = verticalBlocked && Math.abs(direction.y) > DashPhysics.EPSILON;
        if ((horizontalBlocked || verticalAttack) && attack > 0) {
            if (advanceDream(player, speed, false)) {
                return;
            }
            DashPhysics.correct(player, requested, horizontalBlocked, verticalAttack);
        }

        Vec3 actual = move(player, requested);
        boolean sideHit = horizontalBlocked(requested, actual);
        boolean upHit = requested.y > 0 && player.verticalCollision;
        boolean landed = requested.y < 0 && player.verticalCollisionBelow;
        boolean downHit = direction.y < 0 && landed;
        if (applySpring(player) || (sideHit || upHit || downHit) && hitSurface(player, requested, actual, sideHit, downHit, true)) {
            return;
        }
        if (phase == Phase.DASH && downHit && direction.horizontalDistanceSqr() > DashPhysics.EPSILON) {
            direction = DashPhysics.horizontal(direction).normalize();
            speed = DashPhysics.limit(DashPhysics.horizontal(speed).scale(1.2), limit());
            duck = DashPhysics.duck(player);
            slid = true;
        }
        if (landed) {
            groundDeadline = tick + DashTuning.GRACE.get();
            lastGround = player.position();
        }
        if (tryDashJump(player, sideHit, landed ? DashPhysics.refillable(player) : safeBeforeMove)) {
            return;
        }

        if (sideHit && speed.horizontalDistanceSqr() > DashPhysics.EPSILON) {
            retained = DashPhysics.horizontal(speed);
            retention = 2;
            speed = new Vec3(0, speed.y, 0);
        }
        if (upHit || downHit && Math.abs(direction.y) > DashPhysics.EPSILON) {
            speed = DashPhysics.horizontal(speed);
        }
        if (sideHit || upHit || downHit && (phase == Phase.RED || direction.horizontalDistanceSqr() < DashPhysics.EPSILON)) {
            attack = 0;
            if (phase == Phase.RED) {
                phase = Phase.SQUASH;
                duck = false;
                remaining = 2;
                speed = Vec3.ZERO;
                return;
            }
        }
        if (--remaining <= 0 || attack == 0) {
            boolean wet = player.isInWater();
            if (attack != 0 && direction.y >= 0) {
                Vec3 exit = direction.scale(DashTuning.SPEED.get() * (2.0 / 3.0) * (wet ? 0.75 : 1));
                Vec3 flat = (slid || inheritedSpeed) && speed.horizontalDistanceSqr() > exit.horizontalDistanceSqr()
                        ? DashPhysics.horizontal(speed) : DashPhysics.horizontal(exit);
                speed = flat.add(0, exit.y * 0.75, 0);
            }
            if (attack != 0 && direction.y > 0) {
                variableJump = 4;
                jumpSpeed = speed.y;
                autoJump = true;
            }
            momentum(MAX_BOOST_TICKS);
        }
        trail(player, phase == Phase.RED ? ParticleTypes.FLAME : ParticleTypes.CLOUD);
    }

    private boolean tryDashJump(ServerPlayer player, boolean sideHit, boolean safeGround) {
        if (jumpDeadline < tick) {
            return false;
        }
        if (Math.abs(direction.y) < DashPhysics.EPSILON && horizontalGrace(player)) {
            Vec3 forward = DashPhysics.horizontal(direction).normalize();
            double base = DashTuning.SPEED.get() * (260.0 / 240.0) * (duck ? 1.25 : 1);
            speed = forward.scale(Math.max(base, speed.dot(forward))).add(0, duck ? 0.21 : 0.42, 0);
            speed = DashPhysics.limit(speed, limit());
            earnedRefill |= safeGround && DashTuning.GROUND_REFILL.get();
            beginJump(player, (duck ? DashSounds.JUMP_SUPERSLIDE : DashSounds.JUMP_SUPER).get());
            return true;
        }
        if (DashTuning.WALL_JUMPS.get() && (sideHit || !DashPhysics.grounded(player))) {
            Vec3 wall = DashPhysics.wall(player, horizontalFacing(player));
            if (wall.lengthSqr() > 0) {
                wallJump(player, wall, direction.y > 0 && direction.horizontalDistanceSqr() < DashPhysics.EPSILON);
                return true;
            }
        }
        return false;
    }

    private void tryFreeJump(ServerPlayer player, boolean hasGround) {
        if (hasGround || player.isInWater() || player.onClimbable() || player.isNoGravity()) {
            if (hasGround) {
                jumpDeadline = -1;
            }
            return;
        }
        if (horizontalGrace(player) && player.getY() <= lastGround.y + 0.025 && player.getDeltaMovement().y <= 0) {
            speed = DashPhysics.horizontal(player.getDeltaMovement()).add(0, 0.42, 0);
            beginJump(player, DashSounds.JUMP.get());
            return;
        }
        if (DashTuning.WALL_JUMPS.get()) {
            Vec3 wall = DashPhysics.wall(player, horizontalFacing(player));
            if (wall.lengthSqr() > 0) {
                wallJump(player, wall, false);
            }
        }
    }

    private boolean horizontalGrace(ServerPlayer player) {
        return groundDeadline >= tick && lastGround != null && Math.abs(player.getY() - lastGround.y) <= 0.50
                && player.position().distanceToSqr(lastGround) <= 16;
    }

    private void wallJump(ServerPlayer player, Vec3 wall, boolean strong) {
        Vec3 away = wall.scale(-1);
        boolean climb = !strong && (input & SNEAK) != 0 && wish(player).dot(away) <= 0.01 && stamina >= 27;
        speed = climb ? Vec3.ZERO : away.scale(DashTuning.SPEED.get() * (strong ? 170.0 : 130.0) / 240.0);
        speed = speed.add(0, strong ? 0.64 : 0.42, 0);
        beginJump(player, (strong ? DashSounds.JUMP_SUPERWALL : climb ? DashSounds.JUMP_CLIMB : DashSounds.JUMP_WALL).get());
        if (climb) {
            stamina -= 27;
            climbAway = away;
            climbBoost = 4;
        }
    }

    private void beginJump(ServerPlayer player, SoundEvent sound) {
        jumpDeadline = groundDeadline = -1;
        attack = retention = 0;
        speed = DashPhysics.limit(speed.add(takePlatform()), limit());
        momentum(MAX_BOOST_TICKS);
        variableJump = 4;
        jumpSpeed = speed.y;
        player.resetFallDistance();
        effect(player, sound, 1.0F);
    }

    private void momentum(int ticks) {
        phase = Phase.MOMENTUM;
        remaining = ticks;
    }

    private void finish(ServerPlayer player) {
        phase = Phase.IDLE;
        duck = false;
        releaseId = releaseId == Integer.MAX_VALUE ? 1 : releaseId + 1;
        releaseWait = MAX_RELEASE_WAIT;
        send(player, RELEASE);
    }

    private Vec3 takePlatform() {
        if (platformGrace == 0) {
            return Vec3.ZERO;
        }
        platformGrace = 0;
        return platformSpeed;
    }

    private void tryGrab(ServerPlayer player) {
        if (!DashTuning.WALL_JUMPS.get() || (input & SNEAK) == 0 || stamina == 0 || player.isInWater() || player.onClimbable()) {
            return;
        }
        Vec3 facing = horizontalFacing(player);
        Vec3 wall = DashPhysics.wall(player, facing);
        if (wall.dot(facing) > 0.4 && wish(player).dot(wall) >= -0.1) {
            phase = Phase.CLIMB;
            climbWall = wall;
            clearAttack();
            speed = Vec3.ZERO;
            player.resetFallDistance();
        }
    }

    private void stepClimb(ServerPlayer player, boolean hasGround) {
        if ((input & SNEAK) == 0 || stamina == 0 || player.isInWater()
                || DashPhysics.wall(player, climbWall).dot(climbWall) < 0.99 || hasGround && (input & FORWARD) == 0) {
            finish(player);
            return;
        }
        if (jumpDeadline >= tick) {
            wallJump(player, climbWall, false);
            return;
        }
        double vertical = (input & FORWARD) != 0 ? 0.16 : (input & BACK) != 0 ? -0.19 : 0;
        if (vertical != 0) {
            move(player, new Vec3(0, vertical, 0));
        }
        speed = Vec3.ZERO;
        player.resetFallDistance();
        stamina = Math.max(0, stamina - (vertical > 0 ? 5 : 1));
    }

    private void stepMomentum(ServerPlayer player, boolean hasGround) {
        if (attack > 0 && tryDashJump(player, false, hasGround && DashPhysics.refillable(player))) {
            hasGround = false;
        } else if (jumpDeadline >= tick && variableJump == 0) {
            if (hasGround && !player.isInWater()) {
                speed = DashPhysics.horizontal(speed).add(0, 0.42, 0);
                beginJump(player, DashSounds.JUMP.get());
                hasGround = false;
            } else {
                tryFreeJump(player, hasGround);
            }
        }

        Vec3 wish = wish(player);
        if (climbBoost > 0) {
            if (wish.dot(climbAway) > 0.25) {
                speed = climbAway.scale(DashTuning.SPEED.get() * 130.0 / 240.0).add(0, speed.y, 0);
                stamina = Math.min(110, stamina + 27);
                climbBoost = 0;
            } else {
                climbBoost--;
            }
        }
        if (retention > 0) {
            if (wish.dot(retained) < -0.01) {
                retention = 0;
            } else if (!DashPhysics.different(retained, DashPhysics.resolve(player, player.getBoundingBox(), retained))) {
                speed = retained.add(0, speed.y, 0);
                retention = 0;
            } else {
                retention--;
            }
        }

        boolean wet = player.isInWater();
        boolean rising = speed.y > 0;
        double gravity = player.isNoGravity() ? 0 : wet ? 0.025 : 0.08;
        double vertical = speed.y;
        if (variableJump > 0 && ((input & JUMP) != 0 || autoJump) && rising) {
            vertical = Math.max(vertical, jumpSpeed);
            variableJump--;
        } else {
            variableJump = 0;
            autoJump = false;
        }
        if (hasGround && vertical <= 0) {
            vertical = -DashPhysics.SUPPORT_DEPTH;
        }

        Vec3 flat = DashPhysics.horizontal(speed);
        double drag = wet ? 0.8 : hasGround && !rising ? (duck ? 0.72 : 0.80) : 0.985;
        if (wish.dot(flat) < -0.01) {
            drag *= 0.78;
        }
        if (!duck && wish.lengthSqr() > 0) {
            double acceleration = hasGround ? 0.065 : 0.04225;
            if (flat.dot(wish) > 0.24) {
                acceleration *= 0.2;
                if (!hasGround && !wet) {
                    drag = Math.max(drag, 0.995);
                }
            }
            flat = flat.add(wish.scale(acceleration));
        }
        Vec3 requested = DashPhysics.limit(flat.add(0, vertical, 0), limit());
        if (attack > 0 && DashPhysics.different(requested, DashPhysics.resolve(player, player.getBoundingBox(), requested))
                && advanceDream(player, direction.scale(DashTuning.SPEED.get()), false)) {
            return;
        }
        Vec3 actual = move(player, requested);
        if (applySpring(player)) {
            return;
        }
        boolean landed = player.verticalCollisionBelow;
        boolean sideHit = horizontalBlocked(requested, actual);
        if (sideHit && hitSurface(player, requested, actual, true, false, attack > 0)) {
            return;
        }
        if (landed && !hasGround) {
            groundDeadline = tick + DashTuning.GRACE.get();
            lastGround = player.position();
            if (attack > 0 && tryDashJump(player, sideHit, DashPhysics.refillable(player))) {
                return;
            }
        }
        if (sideHit && retention == 0 && requested.horizontalDistanceSqr() > 0.09) {
            retained = DashPhysics.horizontal(requested);
            retention = 2;
        }
        if (player.verticalCollision) {
            variableJump = 0;
            autoJump = false;
        }
        if (!landed && duck && attack == 0) {
            duck = false;
        }
        Vec3 movedSpeed = player.getDeltaMovement();
        double nextY = landed ? 0 : ((player.verticalCollision ? 0 : vertical) - gravity) * (wet ? 0.8 : 0.98);
        double maxFall = Math.min(3.92, DashTuning.SPEED.get() * (((input & SNEAK) != 0 && vertical <= -0.45) ? 1 : 2.0 / 3.0));
        speed = new Vec3(movedSpeed.x * drag, Math.max(-maxFall, nextY), movedSpeed.z * drag);
        if (!landed && speed.y <= 0 && attack == 0 && variableJump == 0) {
            tryGrab(player);
            if (phase == Phase.CLIMB) {
                return;
            }
        }
        if (speed.horizontalDistance() > 0.65) {
            trail(player, ParticleTypes.CLOUD);
        }
        if (--remaining <= 0 || landed && attack == 0 && retention == 0 && speed.horizontalDistanceSqr() < 0.09
                || wet && attack == 0 && speed.horizontalDistanceSqr() < 0.10) {
            finish(player);
        }
    }

    private boolean applySpring(ServerPlayer player) {
        if (spring == null) {
            return false;
        }
        var contact = spring;
        spring = null;
        if (surfaceCooldown > 0 || player.isShiftKeyDown()
                || DashBlocks.kind(player.level().getBlockState(contact.position())) != contact.kind()) {
            return false;
        }
        bounce(player, contact, Vec3.ZERO);
        return true;
    }

    private boolean hitSurface(ServerPlayer player, Vec3 motion, Vec3 actual, boolean side, boolean floor, boolean activeAttack) {
        Vec3 blocked = motion.subtract(actual);
        Vec3 normal = side ? DashPhysics.horizontal(blocked).normalize() : new Vec3(0, floor ? -1 : 1, 0);
        var contact = DashPhysics.contact(player, player.getBoundingBox().expandTowards(normal.scale(0.08)));
        if (contact == null || surfaceCooldown > 0) {
            return false;
        }
        if (floor && contact.kind().floorSpring() && !player.isShiftKeyDown() || side && contact.kind() == DashBlocks.Kind.SIDE_SPRING) {
            bounce(player, contact, normal.scale(-1));
            return true;
        }
        if (activeAttack && (contact.kind() == DashBlocks.Kind.REBOUND || contact.kind() == DashBlocks.Kind.REFLECT)) {
            if (contact.kind() == DashBlocks.Kind.REFLECT) {
                speed = speed.subtract(normal.scale(speed.dot(normal) + DashTuning.SPEED.get()));
            } else {
                speed = (side ? normal.scale(-DashTuning.SPEED.get() * 0.5) : Vec3.ZERO).add(0, 0.44, 0);
            }
            clearAttack();
            surfaceCooldown = 5;
            momentum(10);
            effect(player, DashSounds.REBOUND.get(), 1.0F);
            return true;
        }
        return false;
    }

    private void bounce(ServerPlayer player, DashPhysics.Contact contact, Vec3 fallback) {
        Vec3 away = DashPhysics.horizontal(player.position().subtract(contact.position().getCenter())).normalize();
        if (away.lengthSqr() < DashPhysics.EPSILON) {
            away = fallback.lengthSqr() > 0 ? fallback : horizontalFacing(player);
        }
        double base = DashTuning.SPEED.get();
        speed = switch (contact.kind()) {
            case SPRING -> away.scale(base * 0.6).add(0, 0.60, 0);
            case SUPER_SPRING -> new Vec3(0, 0.75, 0);
            case SIDE_SPRING -> away.scale(base * 0.82).add(0, 0.34, 0);
            default -> throw new IllegalArgumentException("Not a spring: " + contact.kind());
        };
        clearAttack();
        jumpDeadline = groundDeadline = -1;
        surfaceCooldown = 6;
        player.resetFallDistance();
        refill();
        momentum(MAX_BOOST_TICKS);
        effect(player, DashSounds.SPRING.get(), 1.0F);
    }

    private void clearAttack() {
        attack = retention = variableJump = climbBoost = 0;
        autoJump = false;
        duck = false;
    }

    private boolean advanceDream(ServerPlayer player, Vec3 motion, boolean alreadyInside) {
        if (motion.lengthSqr() < DashPhysics.EPSILON) {
            return false;
        }
        int steps = Mth.ceil(motion.length() / 0.125);
        Vec3 increment = motion.scale(1.0 / steps);
        Vec3 origin = player.position();
        Vec3 candidate = origin;
        Vec3 safe = origin;
        boolean seen = alreadyInside;
        for (int step = 0; step < steps; step++) {
            Vec3 next = candidate.add(increment);
            var space = DashPhysics.dreamSpace(player, player.getBoundingBox().move(next.subtract(origin)));
            if (space == DashPhysics.DreamSpace.BLOCKED) {
                if (!seen) {
                    return false;
                }
                // Stop at the last legal substep, then travel back on the next tick.
                // Do not reject an entry simply because its far exit has a solid wall.
                player.setPos(candidate);
                if (!dreamReturning) {
                    dreamReturning = true;
                    direction = direction.scale(-1);
                    speed = motion.scale(-1);
                    remaining = MAX_DREAM_TICKS;
                    dreamExited = false;
                    player.setOnGround(false);
                    player.resetFallDistance();
                    effect(player, DashSounds.DREAM_BOUNCE.get(), 1.0F);
                } else {
                    // A second obstruction means the entry was changed while traversing.
                    abortDream(player);
                }
                return true;
            }
            candidate = next;
            if (space == DashPhysics.DreamSpace.INSIDE) {
                if (!seen) {
                    seen = true;
                    dreamEntrance = safe;
                    dreamEntryDirection = motion.normalize();
                    dreamReturning = false;
                    direction = dreamEntryDirection;
                    speed = motion;
                    redAfterDream = phase == Phase.RED;
                    phase = Phase.DREAM;
                    remaining = MAX_DREAM_TICKS;
                    dreamMin = MIN_DREAM_TICKS;
                    dreamExited = false;
                    player.resetFallDistance();
                    effect(player, DashSounds.DREAM_ENTER.get(), 1.0F);
                }
            } else if (seen) {
                dreamExited = true;
                break;
            } else {
                safe = candidate;
            }
        }
        if (!seen) {
            return false;
        }
        player.setPos(candidate);
        player.setOnGround(false);
        trail(player, ParticleTypes.PORTAL);
        return true;
    }

    private void stepDream(ServerPlayer player) {
        if (!dreamExited) {
            advanceDream(player, speed, true);
        }
        if (phase != Phase.DREAM) {
            return;
        }
        dreamMin = Math.max(0, dreamMin - 1);
        if (dreamExited && dreamMin == 0) {
            dreamEntrance = null;
            refill();
            clearAttack();
            effect(player, DashSounds.DREAM_EXIT.get(), 1.0F);
            if (direction.horizontalDistanceSqr() > DashPhysics.EPSILON) {
                groundDeadline = tick + DashTuning.GRACE.get();
                lastGround = player.position();
                if (jumpDeadline >= tick) {
                    speed = speed.add(0, 0.42, 0);
                    beginJump(player, DashSounds.JUMP_DREAMBLOCK.get());
                    return;
                }
            }
            if (redAfterDream) {
                phase = Phase.RED;
                remaining = MAX_RED_TICKS;
                attack = remaining + 2;
            } else {
                momentum(MAX_BOOST_TICKS);
                tryGrab(player);
            }
        } else if (--remaining <= 0) {
            abortDream(player);
        }
    }

    private void abortDream(ServerPlayer player) {
        retreatDream(player);
        dreamEntrance = null;
        clearAttack();
        speed = Vec3.ZERO;
        finish(player);
    }

    private void retreatDream(ServerPlayer player) {
        if (dreamEntrance == null) {
            return;
        }
        for (int offset = 0; offset <= 8; offset++) {
            Vec3 candidate = dreamEntrance.subtract(dreamEntryDirection.scale(offset * 0.25));
            if (DashPhysics.clear(player, player.getBoundingBox().move(candidate.subtract(player.position())))) {
                player.setPos(candidate);
                return;
            }
        }
    }

    private Vec3 move(ServerPlayer player, Vec3 motion) {
        if (!DashPhysics.loaded(player, player.getBoundingBox().expandTowards(motion))) {
            speed = Vec3.ZERO;
            player.setDeltaMovement(Vec3.ZERO);
            return Vec3.ZERO;
        }
        Vec3 before = player.position();
        DashPhysics.move(player, motion);
        Vec3 moved = player.position().subtract(before);
        player.doCheckFallDamage(moved.x, moved.y, moved.z, player.onGround());
        return moved;
    }

    private static boolean horizontalBlocked(Vec3 requested, Vec3 actual) {
        return Math.abs(requested.x - actual.x) > DashPhysics.EPSILON || Math.abs(requested.z - actual.z) > DashPhysics.EPSILON;
    }

    private Vec3 wish(ServerPlayer player) {
        Vec3 forward = horizontalFacing(player);
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        int longitudinal = ((input & FORWARD) != 0 ? 1 : 0) - ((input & BACK) != 0 ? 1 : 0);
        int lateral = ((input & LEFT) != 0 ? 1 : 0) - ((input & RIGHT) != 0 ? 1 : 0);
        return forward.scale(longitudinal).add(left.scale(lateral)).normalize();
    }

    private static Vec3 horizontalFacing(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }

    private static double limit() {
        return Math.max(DashTuning.MAX_MOMENTUM.get(), DashTuning.SPEED.get());
    }

    private void syncIdle(ServerPlayer player) {
        if (lastCharges != charges || lastMaximum != maxCharges || lastCooldown != cooldown
                || lastDashSpeed != DashTuning.SPEED.get() || lastDashTicks != DashTuning.DURATION.get()) {
            send(player, IDLE);
        }
    }

    private void send(ServerPlayer player, int mode) {
        syncBubbleVisual(player);
        lastCharges = charges;
        lastMaximum = maxCharges;
        lastCooldown = cooldown;
        lastDashSpeed = DashTuning.SPEED.get();
        lastDashTicks = DashTuning.DURATION.get();
        PacketDistributor.sendToPlayer(player, new Frame(mode, releaseId, charges, maxCharges, cooldown, player.onGround(),
                duck, dreaming(), player.position(), speed, lastDashSpeed, lastDashTicks));
    }

    private void syncBubbleVisual(ServerPlayer player) {
        int visual = phase == Phase.BOOSTER ? (redAfterDream ? 2 : 1)
                : phase == Phase.RED || phase == Phase.DREAM && redAfterDream ? 4
                : phase == Phase.DASH && bubbleTicks > 0 ? 3 : 0;
        if (visual != lastBubbleVisual || visual != 0 && tick % 10 == 0) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BubbleVisual(player.getId(), visual,
                    bubbleOrigin == null ? player.blockPosition() : bubbleOrigin));
            lastBubbleVisual = visual;
        }
    }

    private static void effect(ServerPlayer player, SoundEvent sound, float pitch) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.65F, pitch);
    }

    private static void trail(ServerPlayer player, SimpleParticleType particle) {
        player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 0.6, player.getZ(), 4, 0.18, 0.27, 0.18, 0.015);
    }

    private enum Phase { IDLE, DASH, RED, DREAM, MOMENTUM, CLIMB, BOOSTER, SQUASH }
}
