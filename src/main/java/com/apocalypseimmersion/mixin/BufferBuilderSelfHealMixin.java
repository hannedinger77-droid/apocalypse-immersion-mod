package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferBuilder.class, priority = 500, remap = false)
public abstract class BufferBuilderSelfHealMixin {
    @Shadow(aliases = {"f_85661_"}) private boolean building;
    @Shadow(aliases = {"m_85730_"}) public abstract void discard();

    @Inject(
        method = {
            "m_166779_(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
            "begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"
        },
        at = @At("HEAD"),
        require = 0
    )
    private void apocalypseimmersion$selfHealAlreadyBuilding(VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        if (this.building) {
            try {
                this.discard();
            } catch (Throwable ignored) {}
            this.building = false;
        }
    }
}