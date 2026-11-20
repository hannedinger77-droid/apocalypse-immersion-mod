package com.apocalypseimmersion.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "mcjty.lostcities.varia.Tools", remap = false)
public class LostCitiesToolsMixin {
    @Unique
    private static final Logger apocalypseimmersion$LOGGER = LogUtils.getLogger();

    @Inject(method = "stringToState", at = @At("HEAD"), cancellable = true)
    private static void onStringToState(String s, CallbackInfoReturnable<BlockState> cir) {
        if (s == null || !s.contains("[")) return;
        int bracketIndex = s.indexOf('[');
        int endBracket = s.lastIndexOf(']');
        if (bracketIndex <= 0 || endBracket <= bracketIndex) return;

        String blockId = s.substring(0, bracketIndex);
        ResourceLocation rl = ResourceLocation.tryParse(blockId);
        if (rl == null || !ForgeRegistries.BLOCKS.containsKey(rl)) return;

        Block block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null || block == Blocks.AIR) return;

        BlockState state = block.defaultBlockState();
        String propsPart = s.substring(bracketIndex + 1, endBracket);
        boolean modified = false;

        for (String pair : propsPart.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();

            Property<?> prop = block.getStateDefinition().getProperty(key);
            if (prop != null) {
                state = applyProperty(state, prop, val);
            } else if ("facing".equals(key) && state.hasProperty(BlockStateProperties.AXIS)) {
                modified = true;
                if ("north".equals(val) || "south".equals(val)) {
                    state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
                } else if ("east".equals(val) || "west".equals(val)) {
                    state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
                } else {
                    state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
                }
            } else {
                modified = true;
            }
        }

        if (modified) {
            apocalypseimmersion$LOGGER.info("[ApocalypseImmersion] Sanitized Lost Cities blockstate '{}' -> '{}'", s, state);
            cir.setReturnValue(state);
        }
    }

    @Unique
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String val) {
        Optional<T> parsed = prop.getValue(val);
        return parsed.map(t -> state.setValue(prop, t)).orElse(state);
    }
}