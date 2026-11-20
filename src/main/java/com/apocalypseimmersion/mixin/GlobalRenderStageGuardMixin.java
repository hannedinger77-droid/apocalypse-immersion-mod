package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class GlobalRenderStageGuardMixin {
    @Inject(method = "dispatchRenderStage*", at = @At("HEAD"), cancellable = true)
    private static void skipStageWhenBufferBusy(CallbackInfo ci) {
        try {
            if (Tesselator.getInstance().getBuilder().building()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}