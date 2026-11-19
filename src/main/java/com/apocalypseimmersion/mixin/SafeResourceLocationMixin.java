package com.apocalypseimmersion.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ResourceLocation.class)
public class SafeResourceLocationMixin {
    @ModifyVariable(method = "<init>(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation$Dummy;)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static String sanitizePath(String path) {
        if (path == null) return "missing";
        return path.toLowerCase().replace(' ', '_').replaceAll("[^a-z0-9_./-]", "_");
    }
}