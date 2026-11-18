package com.oritong.no_ultra_dash_please.mixin;

import com.oritong.no_ultra_dash_please.DashClient;
import com.oritong.no_ultra_dash_please.DashPhysics;
import com.oritong.no_ultra_dash_please.ServerDash;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$travel(Vec3 input, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (player instanceof ServerPlayer server ? ServerDash.get(server).ownsMovement()
                : player.level().isClientSide && DashClient.travel(player)) {
            callback.cancel();
        }
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$pose(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        // Dream movement deliberately overlaps solid dream blocks. Vanilla's fit test would
        // shrink a standing player to SWIMMING and make underside entry immediately "exit".
        if (player instanceof ServerPlayer server ? ServerDash.get(server).dreaming()
                : player.level().isClientSide && DashClient.dreaming(player)) {
            callback.cancel();
            return;
        }
        boolean duck = player instanceof ServerPlayer server ? ServerDash.get(server).ducking()
                : player.level().isClientSide && DashClient.ducking(player);
        if (duck && DashPhysics.duck(player)) {
            callback.cancel();
        }
    }
}
