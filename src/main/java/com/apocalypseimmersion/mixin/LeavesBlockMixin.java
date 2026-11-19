package com.apocalypseimmersion.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {
    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetLightBlock(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}