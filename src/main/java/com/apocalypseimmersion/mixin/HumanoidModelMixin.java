package com.apocalypseimmersion.mixin;

import com.apocalypseimmersion.client.TacticalLightHelper;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!ApocalypseImmersionConfig.ENABLE_TORCH_HOLD_ANIM.get()) return;
        if (!(entity instanceof Player player)) return;

        boolean rightMain = player.getMainArm() == HumanoidArm.RIGHT;
        boolean holdingMainLight = TacticalLightHelper.isTorchOrLantern(player.getMainHandItem());
        boolean holdingOffLight = TacticalLightHelper.isTorchOrLantern(player.getOffhandItem());

        if (holdingMainLight && !TacticalLightHelper.isPrimaryWeapon(player.getMainHandItem())) {
            ModelPart arm = rightMain ? this.rightArm : this.leftArm;
            arm.xRot = -0.85F + (headPitch * ((float) Math.PI / 180F) * 0.35F);
            arm.yRot = rightMain ? -0.15F : 0.15F;
        }

        if (holdingOffLight && !TacticalLightHelper.isPrimaryWeapon(player.getMainHandItem())) {
            ModelPart offArm = rightMain ? this.leftArm : this.rightArm;
            offArm.xRot = -0.85F + (headPitch * ((float) Math.PI / 180F) * 0.35F);
            offArm.yRot = rightMain ? 0.15F : -0.15F;
        }
    }
}