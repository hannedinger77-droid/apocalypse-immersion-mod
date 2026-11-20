package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferBuilder.class, priority = 500)
public abstract class BufferBuilderSelfHealMixin {
    @Shadow private boolean building;
    @Shadow public abstract void discard();

    @Inject(method = "begin", at = @At("HEAD"))
    private void apocalypseimmersion$selfHealAlreadyBuilding(VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        if (this.building) {
            try {
                this.discard();
            } catch (Throwable ignored) {}
            this.building = false;
        }
    }
}