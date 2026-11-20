package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UnifiedLightRenderer {
    private static final ResourceLocation L_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/entity/belt_lantern.png");
    private static final ResourceLocation SL_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/entity/soul_belt_lantern.png");
    private static final ResourceLocation T_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/entity/belt_torch.png");
    private static final ResourceLocation ST_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/entity/soul_belt_torch.png");
    private static BeltLanternModel lModel;
    private static BeltTorchModel tModel;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        String p = id != null ? id.getPath().toLowerCase() : "";
        boolean isLantern = p.contains("lantern"), isTorch = p.contains("torch");
        if (!isLantern && !isTorch) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        e.setCanceled(true);
        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        float age = mc.player.tickCount + e.getPartialTick();
        float side = ((e.getHand() == InteractionHand.MAIN_HAND) == (mc.player.getMainArm() == HumanoidArm.RIGHT)) ? 1.0F : -1.0F;

        ps.pushPose();
        ps.translate(side * 0.42F, -0.36F - e.getEquipProgress() * 0.35F, -0.62F);
        ps.mulPose(Axis.XP.rotationDegrees(180.0F));
        ps.mulPose(Axis.YP.rotationDegrees(side * 16.0F));
        if (isLantern) {
            if (lModel == null) lModel = new BeltLanternModel();
            ps.translate(0.0F, -0.15F, 0.0F); ps.scale(0.85F, 0.85F, 0.85F);
            lModel.renderLantern(ps, buf.getBuffer(RenderType.entityCutoutNoCull(p.contains("soul") ? SL_TEX : L_TEX)), 15728880, OverlayTexture.NO_OVERLAY, -8.0F, 0.0F, age);
        } else {
            if (tModel == null) tModel = new BeltTorchModel();
            ps.mulPose(Axis.XP.rotationDegrees(-16.0F)); ps.scale(0.95F, 0.95F, 0.95F);
            tModel.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(p.contains("soul") ? ST_TEX : T_TEX)), 15728880, OverlayTexture.NO_OVERLAY, 0.0F, 0.0F, age);
        }
        ps.popPose();
    }

    @SubscribeEvent
    public static void onRenderWorldLights(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance(); Level lvl = mc.level;
        if (lvl == null || mc.player == null) return;
        if (tModel == null) tModel = new BeltTorchModel();
        Vec3 cam = e.getCamera().getPosition(); BlockPos c = mc.player.blockPosition();
        PoseStack ps = e.getPoseStack(); MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float age = mc.player.tickCount + e.getPartialTick();

        for (int dx = -10; dx <= 10; dx++) for (int dy = -4; dy <= 6; dy++) for (int dz = -10; dz <= 10; dz++) {
            BlockPos pos = c.offset(dx, dy, dz);
            Block b = lvl.getBlockState(pos).getBlock();
            if (!(b instanceof TorchBlock) && !(b instanceof LanternBlock)) continue;
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
            boolean soul = id != null && id.getPath().toLowerCase().contains("soul");
            ps.pushPose();
            ps.translate(pos.getX() + 0.5 - cam.x, pos.getY() + (b instanceof LanternBlock ? 0.34 : 0.58) - cam.y, pos.getZ() + 0.5 - cam.z);
            ps.mulPose(Axis.XP.rotationDegrees(180.0F)); ps.scale(0.75F, 0.75F, 0.75F);
            tModel.renderFlameOnly(ps, buf.getBuffer(RenderType.entityCutoutNoCull(soul ? ST_TEX : T_TEX)), 15728880, OverlayTexture.NO_OVERLAY, age);
            ps.popPose();
        }
    }
}