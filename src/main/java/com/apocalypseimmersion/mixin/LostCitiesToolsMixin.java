package com.apocalypseimmersion.mixin;

import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mcjty.lostcities.varia.Tools", remap = false)
public class LostCitiesToolsMixin {
    @Unique
    private static final Logger apocalypseimmersion$LOGGER = LogUtils.getLogger();

    @Inject(method = "stringToState", at = @At("HEAD"), cancellable = true)
    private static void onStringToState(String s, CallbackInfoReturnable<BlockState> cir) {
        if (!ApocalypseImmersionConfig.FIX_LOST_CITIES_FACING.get()) return;
        if (s != null && s.startsWith("doomsday_decoration:") && s.contains("[facing=")) {
            int bracketIndex = s.indexOf('[');
            String blockId = s.substring(0, bracketIndex);
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if (block != null) {
                BlockState defaultState = block.defaultBlockState();
                if (!defaultState.hasProperty(BlockStateProperties.FACING) && !defaultState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    BlockState resolved = defaultState;
                    if (defaultState.hasProperty(BlockStateProperties.AXIS)) {
                        if (s.contains("facing=north") || s.contains("facing=south")) {
                            resolved = defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
                        } else if (s.contains("facing=east") || s.contains("facing=west")) {
                            resolved = defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
                        }
                    }
                    if (ApocalypseImmersionConfig.LOG_LOST_CITIES_FIXES.get()) {
                        apocalypseimmersion$LOGGER.info("[ApocalypseImmersion] Converted '{}' -> '{}'", s, resolved);
                    }
                    cir.setReturnValue(resolved);
                }
            }
        }
    }
}