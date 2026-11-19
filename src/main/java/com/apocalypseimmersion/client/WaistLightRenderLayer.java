package com.apocalypseimmersion.client;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaistLightRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation L_TEX = new ResourceLocation("apocalypseimmersion", "textures/entity/belt_lantern.png");
    private static final ResourceLocation SL_TEX = new ResourceLocation("apocalypseimmersion", "textures/entity/soul_belt_lantern.png");
    private static final ResourceLocation T_TEX = new ResourceLocation("apocalypseimmersion", "textures/entity/belt_torch.png");
    private static final ResourceLocation ST_TEX = new ResourceLocation("apocalypseimmersion", "textures/entity/soul_belt_torch.png");
    private static BeltLanternModel lModel;
    private static BeltTorchModel tModel;
    private static class PendulumState { float pitch, velPitch, roll, velRoll, lastYaw; long lastTick = -1L; }
    private static final Map<UUID, PendulumState> STATES = new ConcurrentHashMap<>();

    public WaistLightRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) { super(p); }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbAmt, float pt, float age, float yaw, float pitch) {
        if (!ApocalypseImmersionConfig.ENABLE_WAIST_LIGHT.get()) return;
        ItemStack stack = TacticalLightHelper.findWaistLightItem(p);
        if (stack.isEmpty()) return;
        if (lModel == null) lModel = new BeltLanternModel();
        if (tModel == null) tModel = new BeltTorchModel();

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id != null ? id.getPath().toLowerCase() : "";
        boolean isLantern = path.contains("lantern"), isTorch = path.contains("torch"), soul = path.contains("soul");

        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        float side = ApocalypseImmersionConfig.WAIST_LIGHT_SIDE_OFFSET.get().floatValue();
        float height = ApocalypseImmersionConfig.WAIST_LIGHT_HEIGHT_OFFSET.get().floatValue();
        float hipSign = side >= 0.0F ? 1.0F : -1.0F;
        float safeSide = hipSign * Math.max(0.32F, Math.abs(side));

        lModel.renderBelt(ps, buf.getBuffer(RenderType.entityTranslucent(soul ? SL_TEX : L_TEX)), light, OverlayTexture.NO_OVERLAY);

        if (isLantern || isTorch) {
            PendulumState st = STATES.computeIfAbsent(p.getUUID(), k -> new PendulumState());
            if (st.lastTick != p.level().getGameTime()) {
                st.lastTick = p.level().getGameTime();
                Vec3 vel = p.getDeltaMovement();
                double rad = Math.toRadians(p.yBodyRot);
                double fwd = -vel.x * Math.sin(rad) + vel.z * Math.cos(rad);
                double str = vel.x * Math.cos(rad) + vel.z * Math.sin(rad);
                float yd = Mth.wrapDegrees(p.yBodyRot - st.lastYaw); st.lastYaw = p.yBodyRot;
                float kick = Mth.cos(limbSwing * 0.6662F) * (isTorch ? 20.0F : 16.0F) * limbAmt;
                float tp = Mth.clamp((float)(-fwd * 85.0 + vel.y * 28.0) + kick, -36.0F, 36.0F);
                float tr = Mth.clamp((float)(str * 75.0 + yd * 1.45), -34.0F, 34.0F);
                st.velPitch = st.velPitch * 0.74F + (tp - st.pitch) * 0.24F;
                st.velRoll = st.velRoll * 0.74F + (tr - st.roll) * 0.24F;
                st.pitch += st.velPitch; st.roll += st.velRoll;

                float legAng = (float) Math.toDegrees(this.getParentModel().rightLeg.xRot);
                float minOut = hipSign * (14.0F + Math.abs(legAng) * 0.32F);
                if (hipSign > 0 && st.roll < minOut) { st.roll = minOut; st.velRoll = Math.abs(st.velRoll) * 0.35F; }
                else if (hipSign < 0 && st.roll > minOut) { st.roll = minOut; st.velRoll = -Math.abs(st.velRoll) * 0.35F; }
            }
            if (isLantern) {
                ps.translate(safeSide + hipSign * 0.04F, 0.50F + height, -0.16F); ps.scale(0.82F, 0.82F, 0.82F);
                lModel.renderLantern(ps, buf.getBuffer(RenderType.entityTranslucent(soul ? SL_TEX : L_TEX)), 15728880, OverlayTexture.NO_OVERLAY, st.pitch, st.roll, age);
            } else {
                ps.translate(safeSide + hipSign * 0.03F, 0.54F + height, -0.15F); ps.scale(0.88F, 0.88F, 0.88F);
                tModel.render(ps, buf.getBuffer(RenderType.entityTranslucent(soul ? ST_TEX : T_TEX)), 15728880, OverlayTexture.NO_OVERLAY, st.pitch + 12.0F, st.roll, age);
            }
        } else {
            float sway = Mth.sin(limbSwing * 0.6662F) * 14.0F * limbAmt;
            ps.translate(safeSide, 0.64F + height, -0.13F);
            ps.mulPose(Axis.XP.rotationDegrees(180.0F + sway)); ps.mulPose(Axis.YP.rotationDegrees(90.0F)); ps.scale(0.45F, 0.45F, 0.45F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, ps, buf, p.level(), p.getId());
        }
        ps.popPose();
    }
}