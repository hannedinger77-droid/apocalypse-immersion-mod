package com.apocalypseimmersion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class BeltLanternModel {

    public final ModelPart belt;
    public final ModelPart lantern;
    public final ModelPart flame;

    public BeltLanternModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. 3D Tactical Waist Belt + Front Brass Buckle wrapped around player torso
        root.addOrReplaceChild("belt", CubeListBuilder.create()
            // Leather belt strap around torso (torso is 8x12x4)
            .texOffs(0, 16).addBox(-4.35F, 9.6F, -2.35F, 8.7F, 1.5F, 4.7F)
            // Front brass belt buckle
            .texOffs(48, 16).addBox(-1.1F, 9.35F, -2.65F, 2.2F, 2.0F, 0.45F),
            PartPose.ZERO);

        // 2. Swinging Lantern Body (pivot at top clip ring 0,0,0)
        PartDefinition lanternPart = root.addOrReplaceChild("lantern", CubeListBuilder.create()
            // Top belt clip ring
            .texOffs(0, 0).addBox(-0.8F, 0.0F, -0.25F, 1.6F, 1.4F, 0.5F)
            // Chimney cap
            .texOffs(0, 4).addBox(-1.6F, 1.4F, -1.6F, 3.2F, 0.8F, 3.2F)
            // Upper hood rim
            .texOffs(0, 10).addBox(-2.4F, 2.2F, -2.4F, 4.8F, 0.8F, 4.8F)
            // 4 sleek forged-iron corner cage bars
            .texOffs(24, 0).addBox(-2.35F, 3.0F, -2.35F, 0.45F, 4.8F, 0.45F)
            .texOffs(24, 0).addBox(1.90F, 3.0F, -2.35F, 0.45F, 4.8F, 0.45F)
            .texOffs(24, 0).addBox(-2.35F, 3.0F, 1.90F, 0.45F, 4.8F, 0.45F)
            .texOffs(24, 0).addBox(1.90F, 3.0F, 1.90F, 0.45F, 4.8F, 0.45F)
            // Bottom base plate
            .texOffs(0, 10).addBox(-2.4F, 7.8F, -2.4F, 4.8F, 1.0F, 4.8F)
            // See-through translucent glass chamber
            .texOffs(0, 26).addBox(-1.95F, 3.0F, -1.95F, 3.9F, 4.8F, 3.9F),
            PartPose.ZERO);

        // 3. Animated Flickering Inner Flame inside the glass chamber
        lanternPart.addOrReplaceChild("flame", CubeListBuilder.create()
            // Main flame body
            .texOffs(0, 50).addBox(-0.75F, -1.8F, -0.75F, 1.5F, 1.8F, 1.5F)
            // Bright inner flame tongue
            .texOffs(42, 50).addBox(-0.4F, -2.7F, -0.4F, 0.8F, 1.1F, 0.8F),
            PartPose.offset(0.0F, 7.2F, 0.0F));

        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        this.belt = baked.getChild("belt");
        this.lantern = baked.getChild("lantern");
        this.flame = this.lantern.getChild("flame");
    }

    public void renderBelt(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        this.belt.render(ps, vc, light, overlay);
    }

    public void renderLantern(PoseStack ps, VertexConsumer vc, int light, int overlay, float pitchDeg, float rollDeg, float ageInTicks) {
        this.lantern.xRot = (float) Math.toRadians(pitchDeg);
        this.lantern.zRot = (float) Math.toRadians(rollDeg);

        // Gentle organic flame flicker & dance inside the see-through glass
        float flicker = 0.90F + Mth.sin(ageInTicks * 0.45F) * 0.14F + Mth.cos(ageInTicks * 0.85F) * 0.07F;
        this.flame.xScale = 0.92F + Mth.sin(ageInTicks * 0.35F) * 0.08F;
        this.flame.yScale = flicker;
        this.flame.zScale = 0.92F + Mth.cos(ageInTicks * 0.35F) * 0.08F;
        this.flame.zRot = Mth.sin(ageInTicks * 0.30F) * 0.09F;

        this.lantern.render(ps, vc, light, overlay);
    }
}