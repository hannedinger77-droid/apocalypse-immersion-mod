package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaczLaserShaderBridge {

    private static boolean hasTaczLaser(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().toLowerCase().contains("tacz")) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        String nbt = tag.toString().toLowerCase();
        return nbt.contains("laser") || nbt.contains("anpeq") || nbt.contains("peq") || nbt.contains("dbal") || nbt.contains("ngal") || nbt.contains("mawl");
    }

    private static float[] getLaserRgb(ItemStack stack) {
        String nbt = stack.getTag() != null ? stack.getTag().toString().toLowerCase() : "";
        if (nbt.contains("green")) return new float[]{0.15F, 1.0F, 0.25F};
        if (nbt.contains("blue") || nbt.contains("cyan")) return new float[]{0.15F, 0.65F, 1.0F};
        if (nbt.contains("ir") || nbt.contains("purple")) return new float[]{0.85F, 0.20F, 1.0F};
        return new float[]{1.0F, 0.12F, 0.12F}; // Tactical Red default
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return;

        ItemStack gun = hasTaczLaser(p.getMainHandItem()) ? p.getMainHandItem() : (hasTaczLaser(p.getOffhandItem()) ? p.getOffhandItem() : ItemStack.EMPTY);
        if (gun.isEmpty()) return;

        float[] rgb = getLaserRgb(gun);
        Vec3 eye = p.getEyePosition(event.getPartialTick());
        Vec3 look = p.getViewVector(event.getPartialTick()).normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();

        // Start beam at right-hand barrel rail offset so it aligns cleanly with first-person & third-person viewmodels
        Vec3 start = eye.add(right.scale(0.18)).add(up.scale(-0.11)).add(look.scale(0.45));
        Vec3 maxEnd = eye.add(look.scale(64.0));
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : maxEnd;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();

        // Draw crisp shader-compatible 3D laser ribbon + bright impact dot
        Vec3 perp = right.scale(0.0085);
        buf.vertex(m, (float)(start.x - perp.x), (float)(start.y - perp.y), (float)(start.z - perp.z)).color(rgb[0], rgb[1], rgb[2], 0.55F).endVertex();
        buf.vertex(m, (float)(start.x + perp.x), (float)(start.y + perp.y), (float)(start.z + perp.z)).color(rgb[0], rgb[1], rgb[2], 0.55F).endVertex();
        buf.vertex(m, (float)(end.x + perp.x), (float)(end.y + perp.y), (float)(end.z + perp.z)).color(rgb[0], rgb[1], rgb[2], 0.35F).endVertex();
        buf.vertex(m, (float)(end.x - perp.x), (float)(end.y - perp.y), (float)(end.z - perp.z)).color(rgb[0], rgb[1], rgb[2], 0.35F).endVertex();

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 dotPos = end.subtract(look.scale(0.02));
            float r = 0.032F;
            buf.vertex(m, (float)(dotPos.x - right.x * r - up.x * r), (float)(dotPos.y - right.y * r - up.y * r), (float)(dotPos.z - right.z * r - up.z * r)).color(rgb[0], rgb[1], rgb[2], 0.92F).endVertex();
            buf.vertex(m, (float)(dotPos.x + right.x * r - up.x * r), (float)(dotPos.y + right.y * r - up.y * r), (float)(dotPos.z + right.z * r - up.z * r)).color(rgb[0], rgb[1], rgb[2], 0.92F).endVertex();
            buf.vertex(m, (float)(dotPos.x + right.x * r + up.x * r), (float)(dotPos.y + right.y * r + up.y * r), (float)(dotPos.z + right.z * r + up.z * r)).color(rgb[0], rgb[1], rgb[2], 0.92F).endVertex();
            buf.vertex(m, (float)(dotPos.x - right.x * r + up.x * r), (float)(dotPos.y - right.y * r + up.y * r), (float)(dotPos.z - right.z * r + up.z * r)).color(rgb[0], rgb[1], rgb[2], 0.92F).endVertex();
        }

        tes.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }
}