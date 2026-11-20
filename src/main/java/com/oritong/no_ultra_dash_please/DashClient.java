package com.oritong.no_ultra_dash_please;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.oritong.no_ultra_dash_please.DashNetwork.*;

@EventBusSubscriber(modid = NoUltraDashPlease.MOD_ID, value = Dist.CLIENT)
public final class DashClient {
    private static final KeyMapping KEY = new KeyMapping("key.no_ultra_dash_please.dash", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.no_ultra_dash_please");
    private static LocalPlayer owner;
    private static Frame movement;
    private static Vec3 predictedSpeed = Vec3.ZERO;
    private static Vec3 previewOrigin = Vec3.ZERO;
    private static Vec3 previewVelocity = Vec3.ZERO;
    private static int prediction;
    private static int previewWait;
    private static int buttons;
    private static int charges;
    private static int maximum;
    private static int cooldown;
    private static int dashTicks;
    private static double dashSpeed;
    private static boolean driven;
    private static boolean duck;
    private static boolean dreaming;
    private static boolean climbing;
    private static float stamina = 110;
    private static Vec3 dreamSpeed = Vec3.ZERO;
    private static Pose exitPose;
    private static DreamTravelSound dreamSound;

    private DashClient() {}

    @SubscribeEvent
    static void keyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void gui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, NoUltraDashPlease.id("charges"), (graphics, delta) -> {
            var minecraft = Minecraft.getInstance();
            if (maximum == 0 || owner == null || owner != minecraft.player || minecraft.options.hideGui
                    || owner.isSpectator() || !owner.isAlive()) {
                return;
            }
            int start = graphics.guiWidth() / 2 + 96;
            int centerY = graphics.guiHeight() - 12;
            for (int index = 0; index < maximum; index++) {
                int centerX = start + index * 9;
                int color = index < charges ? (cooldown == 0 ? 0xFF65EBFF : 0xFF62A2BC) : 0xFF515661;
                for (int row = -4; row <= 4; row++) {
                    int radius = 4 - Math.abs(row);
                    graphics.fill(centerX - radius, centerY + row, centerX + radius + 1, centerY + row + 1,
                            row == 0 ? color : (index < charges ? color : 0xFF30343C));
                }
            }
        });
    }

    @SubscribeEvent
    static void blockOverlay(RenderBlockScreenEffectEvent event) {
        if (owner == event.getPlayer() && driven && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
                && event.getBlockState().is(DashBlocks.DREAM)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void input(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (owner != player) {
            reset(player);
        }
        boolean clicked = false;
        while (KEY.consumeClick()) {
            clicked = true;
        }
        if (player == null || !player.connection.getConnection().isConnected()) {
            return;
        }
        var options = minecraft.options;
        boolean allowed = minecraft.screen == null && DashPhysics.eligible(player);
        int next = allowed ? (options.keyJump.isDown() ? JUMP : 0) | (options.keyShift.isDown() ? SNEAK : 0)
                | (options.keyUp.isDown() ? FORWARD : 0) | (options.keyDown.isDown() ? BACK : 0)
                | (options.keyLeft.isDown() ? LEFT : 0) | (options.keyRight.isDown() ? RIGHT : 0)
                | (options.keyUse.isDown() && ClimbHands.empty(player) ? GRAB : 0) : 0;
        if (next != buttons || allowed && clicked) {
            buttons = next;
            send(player, allowed && clicked, 0);
        }
        if (allowed && clicked && !driven && movement == null && previewWait == 0 && charges > 0 && cooldown == 0) {
            Vec3 aim = DashPhysics.aim(player.getYRot(), player.getXRot());
            previewOrigin = player.position();
            previewVelocity = player.getDeltaMovement();
            predictedSpeed = DashPhysics.startVelocity(aim, player.getDeltaMovement(), dashSpeed, player.isInWater());
            prediction = Math.min(2, dashTicks);
            previewWait = 8;
        }
    }

    static void receive(Frame frame) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (owner != player) {
            reset(player);
        }
        charges = frame.charges();
        maximum = frame.maximum();
        cooldown = frame.cooldown();
        dashSpeed = frame.dashSpeed();
        dashTicks = frame.dashTicks();
        dreaming = frame.dreaming();
        climbing = frame.climbing();
        stamina = Float.isFinite(frame.stamina()) ? Math.clamp(frame.stamina(), 0, 110) : 110;
        dreamSpeed = dreaming ? frame.speed() : Vec3.ZERO;
        exitPose = switch (frame.compactPose()) {
            case 1 -> Pose.CROUCHING;
            case 2 -> Pose.SWIMMING;
            default -> null;
        };
        applyExitPose(player);
        if (frame.dreaming()) {
            if (dreamSound == null || dreamSound.isStopped()) {
                dreamSound = new DreamTravelSound(player);
                Minecraft.getInstance().getSoundManager().play(dreamSound);
            }
        } else {
            stopDreamSound();
        }
        if (frame.mode() == REJECT && previewWait > 0) {
            previewWait = 1;
            prediction = 0;
        } else if (frame.mode() == RESET) {
            DashScreenEffect.stop();
            movement = null;
            prediction = previewWait = 0;
            driven = duck = climbing = false;
        } else if (frame.mode() == MOVING || frame.mode() == RELEASE) {
            prediction = previewWait = 0;
            movement = frame;
            driven = true;
            duck = frame.duck();
        }
    }

    public static boolean travel(Player player) {
        if (owner != player || !DashPhysics.eligible(player)) {
            return false;
        }
        if (movement != null) {
            Frame frame = movement;
            movement = null;
            player.setPos(frame.position());
            player.setOnGround(frame.grounded());
            player.setDeltaMovement(frame.speed());
            if (frame.mode() == RELEASE) {
                driven = duck = false;
                send(owner, false, frame.release());
            }
            return true;
        }
        if (prediction > 0) {
            prediction--;
            Vec3 velocity = predictedSpeed;
            if (Math.abs(velocity.y) < DashPhysics.EPSILON && DashPhysics.grounded(player)) {
                velocity = velocity.add(0, -DashPhysics.SUPPORT_DEPTH, 0);
            }
            if (DashPhysics.loaded(player, player.getBoundingBox().expandTowards(velocity))) {
                DashPhysics.move(player, velocity);
            }
        }
        if (previewWait > 0) {
            if (--previewWait == 0) {
                player.setPos(previewOrigin);
                player.setDeltaMovement(previewVelocity);
                player.setOnGround(DashPhysics.grounded(player));
            }
            return true;
        }
        return driven;
    }

    public static boolean overridesJump(Player player) {
        return owner == player && (driven || previewWait > 0);
    }

    public static boolean isPredicting(LocalPlayer player) {
        return owner == player && previewWait > 0;
    }

    public static boolean ducking(Player player) {
        return owner == player && duck;
    }

    public static boolean dreaming(Player player) {
        return owner == player && dreaming;
    }

    public static boolean climbing(Player player) { return owner == player && climbing; }

    public static float stamina(Player player) { return owner == player ? stamina : 110; }

    @SubscribeEvent
    static void grabInsteadOfUsingItem(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!event.isUseItem() || player == null || minecraft.screen != null || !DashPhysics.eligible(player)
                || !ClimbHands.empty(player) || !DashTuning.WALL_CLIMBING.get()
                || player.isInWater() || player.onClimbable()) return;
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        if (climbing(player) || DashPhysics.wall(player, facing).dot(facing) > 0.4) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    static Vec3 dreamSpeed(Player player) {
        return owner == player ? dreamSpeed : Vec3.ZERO;
    }

    public static boolean applyExitPose(Player player) {
        if (owner != player || exitPose == null || player.getForcedPose() != null) return false;
        player.setPose(exitPose);
        return true;
    }

    private static void send(LocalPlayer player, boolean dash, int release) {
        PacketDistributor.sendToServer(new Input(buttons | (dash ? DASH : 0), player.getYRot(), player.getXRot(), release));
    }

    private static void reset(LocalPlayer player) {
        stopDreamSound();
        DashScreenEffect.stop();
        owner = player;
        movement = null;
        predictedSpeed = Vec3.ZERO;
        prediction = previewWait = buttons = charges = maximum = cooldown = dashTicks = 0;
        dashSpeed = 0;
        driven = duck = dreaming = climbing = false;
        stamina = 110;
        dreamSpeed = Vec3.ZERO;
        exitPose = null;
    }

    private static void stopDreamSound() {
        if (dreamSound != null) {
            dreamSound.finish();
            Minecraft.getInstance().getSoundManager().stop(dreamSound);
            dreamSound = null;
        }
    }
}
