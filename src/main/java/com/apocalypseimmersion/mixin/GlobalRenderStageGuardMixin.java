package com.apocalypseimmersion.mixin;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.reflect.Method;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class GlobalRenderStageGuardMixin {
    @Unique private static Method ai$irisShadowCheck;
    @Unique private static boolean ai$irisChecked = false;

    @Unique
    private static boolean isOculusShadowPass() {
        if (!ai$irisChecked) {
            ai$irisChecked = true;
            try {
                Class<?> cls = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
                ai$irisShadowCheck = cls.getMethod("areShadowsCurrentlyBeingRendered");
            } catch (Throwable ignored) {}
        }
        if (ai$irisShadowCheck != null) {
            try { return (boolean) ai$irisShadowCheck.invoke(null); } catch (Throwable ignored) {}
        }
        return false;
    }

    @Inject(method = "dispatchRenderStage*", at = @At("HEAD"), cancellable = true)
    private static void skipStageWhenUnsafe(CallbackInfo ci) {
        try {
            if (isOculusShadowPass() || Tesselator.getInstance().getBuilder().building()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {}
    }
}