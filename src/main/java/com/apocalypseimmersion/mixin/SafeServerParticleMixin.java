package com.apocalypseimmersion.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class SafeServerParticleMixin {
    @Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At("HEAD"), cancellable = true)
    private <T extends ParticleOptions> void guardNullParticle(T type, double x, double y, double z, int count, double dx, double dy, double dz, double speed, CallbackInfoReturnable<Integer> cir) {
        if (type == null) {
            cir.setReturnValue(0);
        }
    }
}