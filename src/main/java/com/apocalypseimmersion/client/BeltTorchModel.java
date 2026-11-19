package com.apocalypseimmersion.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class BeltTorchModel {
    public final ModelPart torch, flame;
    public BeltTorchModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition torchPart = mesh.getRoot().addOrReplaceChild("torch", CubeListBuilder.create()
            .texOffs(0, 24).addBox(-1.35F, -0.6F, -1.35F, 2.7F, 1.2F, 2.7F)
            .texOffs(0, 0).addBox(-1.0F, 0.6F, -1.0F, 2.0F, 7.6F, 2.0F)
            .texOffs(0, 0).addBox(-1.0F, -2.6F, -1.0F, 2.0F, 2.0F, 2.0F)
            .texOffs(0, 36).addBox(-1.15F, -4.2F, -1.15F, 2.3F, 1.6F, 2.3F), PartPose.ZERO);
        torchPart.addOrReplaceChild("flame", CubeListBuilder.create()
            .texOffs(0, 50).addBox(-0.95F, -2.4F, -0.95F, 1.9F, 2.4F, 1.9F)
            .texOffs(42, 50).addBox(-0.55F, -3.6F, -0.55F, 1.1F, 1.5F, 1.1F), PartPose.offset(0.0F, -4.2F, 0.0F));
        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        this.torch = baked.getChild("torch");
        this.flame = this.torch.getChild("flame");
    }
    public void render(PoseStack ps, VertexConsumer vc, int light, int overlay, float pitchDeg, float rollDeg, float age) {
        this.torch.xRot = (float) Math.toRadians(pitchDeg);
        this.torch.zRot = (float) Math.toRadians(rollDeg);
        this.flame.xScale = 0.90F + Mth.sin(age * 0.40F) * 0.10F;
        this.flame.yScale = 0.88F + Mth.sin(age * 0.52F) * 0.16F + Mth.cos(age * 0.92F) * 0.08F;
        this.flame.zScale = 0.90F + Mth.cos(age * 0.40F) * 0.10F;
        this.flame.zRot = Mth.sin(age * 0.34F) * 0.11F;
        this.torch.render(ps, vc, light, overlay);
    }
}