package com.oritong.no_ultra_dash_please.mixin;

import com.oritong.no_ultra_dash_please.ServerDash;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerMovementMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow public abstract void resetPosition();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"))
    private void noUltraDash$move(CallbackInfo callback) {
        if (awaitingPositionFromClient == null && !player.wonGame) {
            Vec3 before = player.position();
            if (ServerDash.get(player).tick(player)) {
                resetPosition();
                if (!before.equals(player.position())) {
                    player.serverLevel().getChunkSource().move(player);
                }
                clientIsFloating = false;
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;wonGame:Z"), cancellable = true)
    private void noUltraDash$rotationOnly(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (awaitingPositionFromClient == null && ServerDash.get(player).ownsMovement()) {
            player.setYRot(Mth.wrapDegrees(packet.getYRot(player.getYRot())));
            player.setXRot(Mth.clamp(Mth.wrapDegrees(packet.getXRot(player.getXRot())), -90, 90));
            callback.cancel();
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void noUltraDash$teleport(double x, double y, double z, float yaw, float pitch,
                                     Set<RelativeMovement> relative, CallbackInfo callback) {
        ServerDash.get(player).interrupt(player);
    }
}
