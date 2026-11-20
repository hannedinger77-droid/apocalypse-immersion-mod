package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderSelfHealMixin {
    @Shadow private boolean building;
    @Shadow public abstract void discard();

    @Inject(method = "begin", at = @At("HEAD"))
    private void apocalypseimmersion$selfHealAlreadyBuilding(CallbackInfo ci) {
        if (this.building) {
            try {
                this.discard();
            } catch (Throwable ignored) {}
        }
    }
}