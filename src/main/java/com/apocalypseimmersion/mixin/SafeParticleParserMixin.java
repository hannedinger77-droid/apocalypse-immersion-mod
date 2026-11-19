package com.apocalypseimmersion.mixin;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleArgument.class)
public class SafeParticleParserMixin {
    @Inject(method = "readParticle(Lcom/mojang/brigadier/StringReader;Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/core/particles/ParticleOptions;", at = @At("HEAD"), cancellable = true)
    private static void onReadParticle(StringReader reader, HolderLookup<ParticleType<?>> lookup, CallbackInfoReturnable<ParticleOptions> cir) {
        int start = reader.getCursor();
        try {
            ResourceLocation id = ResourceLocation.read(reader);
            if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) {
                while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
                cir.setReturnValue(id.getPath().contains("dust") || id.getPath().contains("smoke") ? ParticleTypes.SMOKE : ParticleTypes.CRIT);
                return;
            }
        } catch (Exception ignored) {
        }
        reader.setCursor(start);
    }
}