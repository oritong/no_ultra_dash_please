package com.oritong.no_ultra_dash_please.mixin;

import com.oritong.no_ultra_dash_please.ServerDash;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Unique private Vec3 noUltraDash$beforePiston;

    @Inject(method = "move", at = @At("HEAD"))
    private void noUltraDash$pistonStart(MoverType type, Vec3 motion, CallbackInfo callback) {
        if (type == MoverType.PISTON && (Object) this instanceof ServerPlayer player && player.onGround()) {
            noUltraDash$beforePiston = player.position();
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void noUltraDash$pistonEnd(MoverType type, Vec3 motion, CallbackInfo callback) {
        if (type == MoverType.PISTON && noUltraDash$beforePiston != null && (Object) this instanceof ServerPlayer player) {
            Vec3 displacement = player.position().subtract(noUltraDash$beforePiston);
            noUltraDash$beforePiston = null;
            if (displacement.lengthSqr() > 1.0e-8) {
                ServerDash.get(player).inheritPlatform(displacement);
            }
        }
    }
}
