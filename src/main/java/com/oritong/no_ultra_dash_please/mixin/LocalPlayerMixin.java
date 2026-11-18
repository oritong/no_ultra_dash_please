package com.oritong.no_ultra_dash_please.mixin;

import com.oritong.no_ultra_dash_please.DashClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$withholdPredictedPosition(CallbackInfo callback) {
        if (DashClient.isPredicting((LocalPlayer) (Object) this)) {
            callback.cancel();
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void noUltraDash$dreamPushOut(double x, double z, CallbackInfo callback) {
        if (DashClient.dreaming((LocalPlayer) (Object) this)) {
            callback.cancel();
        }
    }
}
