package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferBuilder.class, remap = false)
public abstract class BufferBuilderSelfHealMixin {
    @Shadow(aliases = {"f_85661_"}) private boolean building;
    @Shadow(aliases = {"m_85730_"}) public abstract void discard();

    @Inject(method = {"begin", "m_166779_"}, at = @At("HEAD"))
    private void apocalypseimmersion$selfHealAlreadyBuilding(CallbackInfo ci) {
        if (this.building) {
            try {
                this.discard();
            } catch (Throwable ignored) {}
        }
    }
}