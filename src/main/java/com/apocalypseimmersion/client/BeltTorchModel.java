package com.apocalypseimmersion.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class BeltTorchModel {
    public final ModelPart torch, flame, core, tongue, ember;
    public BeltTorchModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition torchPart = mesh.getRoot().addOrReplaceChild("torch", CubeListBuilder.create()
            .texOffs(0, 24).addBox(-1.35F, -0.6F, -1.35F, 2.7F, 1.2F, 2.7F)
            .texOffs(0, 0).addBox(-1.0F, 0.6F, -1.0F, 2.0F, 7.6F, 2.0F)
            .texOffs(0, 0).addBox(-1.0F, -2.6F, -1.0F, 2.0F, 2.0F, 2.0F)
            .texOffs(0, 36).addBox(-1.15F, -4.2F, -1.15F, 2.3F, 1.6F, 2.3F), PartPose.ZERO);

        PartDefinition flameRoot = torchPart.addOrReplaceChild("flame", CubeListBuilder.create()
            .texOffs(0, 50).addBox(-0.85F, -1.1F, -0.85F, 1.7F, 1.1F, 1.7F)
            .texOffs(0, 50).addBox(-1.05F, -2.3F, -1.05F, 2.1F, 1.3F, 2.1F)
            .texOffs(0, 50).addBox(-0.78F, -3.4F, -0.78F, 1.56F, 1.2F, 1.56F), PartPose.offset(0.0F, -4.1F, 0.0F));
        flameRoot.addOrReplaceChild("core", CubeListBuilder.create()
            .texOffs(42, 50).addBox(-0.48F, -2.6F, -0.48F, 0.96F, 2.1F, 0.96F)
            .texOffs(42, 50).addBox(-0.30F, -3.5F, -0.30F, 0.60F, 1.1F, 0.60F), PartPose.ZERO);
        flameRoot.addOrReplaceChild("tongue", CubeListBuilder.create()
            .texOffs(0, 50).addBox(-0.48F, -1.3F, -0.48F, 0.96F, 1.3F, 0.96F)
            .texOffs(42, 50).addBox(-0.24F, -2.2F, -0.24F, 0.48F, 1.0F, 0.48F), PartPose.offset(0.0F, -3.3F, 0.0F));
        flameRoot.addOrReplaceChild("ember", CubeListBuilder.create()
            .texOffs(42, 50).addBox(-0.16F, -0.32F, -0.16F, 0.32F, 0.32F, 0.32F), PartPose.offset(0.0F, -5.6F, 0.0F));

        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        this.torch = baked.getChild("torch"); this.flame = this.torch.getChild("flame");
        this.core = this.flame.getChild("core"); this.tongue = this.flame.getChild("tongue"); this.ember = this.flame.getChild("ember");
    }
    public void render(PoseStack ps, VertexConsumer vc, int light, int overlay, float pitchDeg, float rollDeg, float age) {
        this.torch.xRot = (float) Math.toRadians(pitchDeg); this.torch.zRot = (float) Math.toRadians(rollDeg);
        animateFlame(age); this.torch.render(ps, vc, light, overlay);
    }
    public void renderFlameOnly(PoseStack ps, VertexConsumer vc, int light, int overlay, float age) {
        animateFlame(age); this.flame.render(ps, vc, light, overlay);
    }
    private void animateFlame(float age) {
        this.flame.xScale = 0.94F + Mth.sin(age * 0.38F) * 0.07F;
        this.flame.yScale = 0.92F + Mth.sin(age * 0.48F) * 0.12F + Mth.cos(age * 0.85F) * 0.05F;
        this.flame.zScale = 0.94F + Mth.cos(age * 0.38F) * 0.07F;
        this.flame.zRot = Mth.sin(age * 0.28F) * 0.07F; this.flame.xRot = Mth.cos(age * 0.24F) * 0.06F;
        this.core.xScale = 0.92F + Mth.cos(age * 0.62F) * 0.09F; this.core.yScale = 0.95F + Mth.sin(age * 0.71F) * 0.11F; this.core.zScale = this.core.xScale;
        this.tongue.zRot = Mth.sin(age * 0.44F + 0.8F) * 0.18F; this.tongue.xRot = Mth.cos(age * 0.39F + 0.5F) * 0.15F; this.tongue.yScale = 0.85F + Mth.sin(age * 0.66F) * 0.22F;
        float cycle = (age * 0.14F) % 1.0F;
        this.ember.y = -5.2F - cycle * 2.4F; this.ember.x = Mth.sin(age * 0.35F + cycle * 4.0F) * 0.45F; this.ember.z = Mth.cos(age * 0.31F + cycle * 4.0F) * 0.45F;
        float s = Math.max(0.05F, 1.0F - cycle); this.ember.xScale = s; this.ember.yScale = s; this.ember.zScale = s;
    }
}