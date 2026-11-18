package com.oritong.no_ultra_dash_please.mixin;

import com.oritong.no_ultra_dash_please.DashClient;
import com.oritong.no_ultra_dash_please.ServerDash;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$dreamSuffocation(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player && ServerDash.get(player).dreaming()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$jump(CallbackInfo callback) {
        if ((Object) this instanceof Player player && (player instanceof ServerPlayer server
                ? ServerDash.get(server).ownsMovement() : player.level().isClientSide && DashClient.overridesJump(player))) {
            callback.cancel();
        }
    }
}
