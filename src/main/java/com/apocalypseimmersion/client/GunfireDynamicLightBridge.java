package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunfireDynamicLightBridge {

    private static int muzzleFlashTicks = 0;
    private static Vec3 flashOrigin = Vec3.ZERO;
    private static Vec3 flashDir = new Vec3(0, 0, 1);

    @SubscribeEvent
    public static void onGunSound(PlaySoundEvent event) {
        if (event.getSound() == null) return;
        if (event.getSound().getSource() == SoundSource.AMBIENT || event.getSound().getSource() == SoundSource.MUSIC) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return;

        ResourceLocation loc = event.getSound().getLocation();
        String fullId = loc.toString().toLowerCase();
        String resolved = "";
        try {
            if (event.getSound().getSound() != null && event.getSound().getSound().getLocation() != null) {
                resolved = event.getSound().getSound().getLocation().toString().toLowerCase();
            }
        } catch (Exception ignored) {}
        String combined = fullId + " " + resolved;

        if (combined.contains("reload") || combined.contains("draw") || combined.contains("equip")
                || combined.contains("swap") || combined.contains("holster") || combined.contains("bolt")
                || combined.contains("mag") || combined.contains("dry") || combined.contains("silence") || combined.contains("suppress")) {
            return;
        }

        boolean isShot = (combined.contains("shoot") || combined.contains("fire") || combined.contains("shot")
                || combined.contains("tp_shooting") || combined.contains("ssr") || combined.contains("gd656"))
                && (combined.contains("tacz") || combined.contains("gun") || combined.contains("ssr") || combined.contains("tp_shooting") || combined.contains("pointblank"));
        if (loc.getNamespace().equals("tacz") && resolved.isEmpty()) {
            try {
                if (event.getSound().getSound() != null) {
                    isShot = event.getSound().getVolume() >= 1.55f;
                }
            } catch (NullPointerException ignored) {
                isShot = false;
            }
        }
        if (!isShot) return;

        Vec3 src = new Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
        if (src.distanceToSqr(p.position()) <= 25.0) {
            muzzleFlashTicks = 3;
            flashDir = p.getLookAngle().normalize();
            flashOrigin = p.getEyePosition().add(flashDir.scale(0.85)).add(0, -0.1, 0);

            // Spawn warm spark/flash particles ahead of barrel (complements GD656LightCore without blocking it)
            mc.level.addParticle(ParticleTypes.FLAME, flashOrigin.x, flashOrigin.y, flashOrigin.z, flashDir.x * 0.12, flashDir.y * 0.12, flashDir.z * 0.12);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && muzzleFlashTicks > 0) muzzleFlashTicks--;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || muzzleFlashTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return;

        // Cast warm muzzle-flash illumination onto nearby walls/floors (compatible with GD656LightCore & SSR5)
        BlockHitResult hit = mc.level.clip(new ClipContext(flashOrigin, flashOrigin.add(flashDir.scale(14.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 target = hit.getType() != HitResult.Type.MISS ? hit.getLocation().subtract(flashDir.scale(0.05)) : flashOrigin.add(flashDir.scale(6.0));
        BlockPos bp = BlockPos.containing(target);
        int blockLight = mc.level.getBrightness(LightLayer.BLOCK, bp);
        float darkBoost = blockLight < 10 ? 1.15f : 0.65f;
        float alpha = (muzzleFlashTicks / 3.0f) * 0.42f * darkBoost;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        if (buf.building()) return; buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();

        Vec3 right = flashDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(flashDir).normalize();
        float r = 2.6f;

        for (int i = 0; i < 14; i++) {
            double a1 = (Math.PI * 2.0 * i) / 14.0, a2 = (Math.PI * 2.0 * (i + 1)) / 14.0;
            Vec3 p1 = target.add(right.scale(Math.cos(a1) * r)).add(up.scale(Math.sin(a1) * r));
            Vec3 p2 = target.add(right.scale(Math.cos(a2) * r)).add(up.scale(Math.sin(a2) * r));
            buf.vertex(m, (float) target.x, (float) target.y, (float) target.z).color(1.0f, 0.78f, 0.42f, alpha).endVertex();
            buf.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(1.0f, 0.58f, 0.18f, 0.0f).endVertex();
            buf.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(1.0f, 0.58f, 0.18f, 0.0f).endVertex();
            buf.vertex(m, (float) target.x, (float) target.y, (float) target.z).color(1.0f, 0.78f, 0.42f, alpha).endVertex();
        }

        tes.end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }
}