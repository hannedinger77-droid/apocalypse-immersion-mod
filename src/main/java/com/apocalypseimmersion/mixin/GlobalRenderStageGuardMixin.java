package com.apocalypseimmersion.mixin;

import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class GlobalRenderStageGuardMixin {

    @Inject(method = "dispatchRenderStage*", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onDispatchRenderStage(CallbackInfo ci) {
        String trace = Arrays.toString(Thread.currentThread().getStackTrace()).toLowerCase();
        if (trace.contains("shadowrender") || trace.contains("rendershadows") || trace.contains("camerapass")) {
            ci.cancel();
        }
    }
}