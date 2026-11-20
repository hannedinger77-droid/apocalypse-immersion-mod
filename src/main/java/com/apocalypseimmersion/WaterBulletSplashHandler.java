package com.apocalypseimmersion;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaterBulletSplashHandler {
    private static final ResourceLocation PLUME_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/misc/water_plume.png");
    private static final SoundEvent SPLASH_SFX = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "explosive_water_splash"));
    private record WaterPlume(double x, double y, double z, float scale, int age, int maxAge) {}
    private static final List<WaterPlume> PLUMES = new CopyOnWriteArrayList<>();
    private static long lastSplashMs = 0L;
    private static Vec3 lastSplashPos = Vec3.ZERO;

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide()) return;
        float radius = Math.max(3.5f, Math.min(8.5f, 3.0f + event.getAffectedBlocks().size() * 0.04f));
        triggerDetailedWaterSplash((ClientLevel) event.getLevel(), event.getExplosion().getPosition(), radius);
    }

    @SubscribeEvent
    public static void onExplosiveSound(PlaySoundEvent event) {
        if (event.getSound() == null) return;
        if (event.getSound().getSource() == SoundSource.AMBIENT || event.getSound().getSource() == SoundSource.MUSIC) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String id = event.getSound().getLocation().toString().toLowerCase();
        if (!(id.contains("grenade") || id.contains("frag") || id.contains("rpg") || id.contains("rocket") || id.contains("c4") || id.contains("claymore") || id.contains("mortar") || id.contains("detonat") || id.contains("explosive") || id.contains("he_") || id.contains("tnt"))) return;
        triggerDetailedWaterSplash(mc.level, new Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ()), 4.5f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PLUMES.isEmpty()) return;
        for (int i = PLUMES.size() - 1; i >= 0; i--) {
            WaterPlume p = PLUMES.get(i);
            if (p.age + 1 >= p.maxAge) PLUMES.remove(i);
            else PLUMES.set(i, new WaterPlume(p.x, p.y, p.z, p.scale, p.age + 1, p.maxAge));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || PLUMES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader); RenderSystem.setShaderTexture(0, PLUME_TEX);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = ps.last().pose();

        for (WaterPlume p : PLUMES) {
            float t = (p.age + event.getPartialTick()) / (float) p.maxAge;
            float h = (4.8f + (float) Math.sin(Math.min(1.0, t * 1.35) * (Math.PI * 0.5)) * 5.2f) * p.scale;
            float w = (1.6f + t * 2.4f) * p.scale;
            float alpha = (1.0f - t) * 0.88f;
            double[][] axes = {{1.0, 0.0}, {0.0, 1.0}, {0.707, 0.707}, {-0.707, 0.707}};
            for (double[] ax : axes) {
                float dx = (float) (ax[0] * w), dz = (float) (ax[1] * w);
                buf.vertex(m, (float) p.x - dx, (float) p.y - 0.1f, (float) p.z - dz).uv(0f, 1f).color(0.90f, 0.97f, 1.0f, alpha).endVertex();
                buf.vertex(m, (float) p.x + dx, (float) p.y - 0.1f, (float) p.z + dz).uv(1f, 1f).color(0.90f, 0.97f, 1.0f, alpha).endVertex();
                buf.vertex(m, (float) p.x + dx, (float) p.y + h, (float) p.z + dz).uv(1f, 0f).color(0.96f, 0.99f, 1.0f, alpha * 0.75f).endVertex();
                buf.vertex(m, (float) p.x - dx, (float) p.y + h, (float) p.z - dz).uv(0f, 0f).color(0.96f, 0.99f, 1.0f, alpha * 0.75f).endVertex();
            }
        }
        tes.end();
        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    public static void triggerDetailedWaterSplash(ClientLevel level, Vec3 origin, float power) {
        long now = System.currentTimeMillis();
        if (now - lastSplashMs < 140L && origin.distanceToSqr(lastSplashPos) < 9.0) return;
        BlockPos center = BlockPos.containing(origin);
        BlockPos waterSurface = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = -3; dy <= 3; dy++) {
            BlockPos p = center.offset(dx, dy, dz);
            if (level.getFluidState(p).is(FluidTags.WATER)) {
                BlockPos top = p;
                while (level.getFluidState(top.above()).is(FluidTags.WATER) && top.getY() - p.getY() < 16) top = top.above();
                double dSq = origin.distanceToSqr(top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5);
                if (dSq < bestDistSq && dSq <= 20.25) { bestDistSq = dSq; waterSurface = top; }
                break;
            }
        }
        if (waterSurface == null) return;
        lastSplashMs = now; lastSplashPos = origin;
        double sx = waterSurface.getX() + 0.5, sy = waterSurface.getY() + 1.0, sz = waterSurface.getZ() + 0.5;
        float scale = Math.min(2.6f, Math.max(0.95f, power / 3.5f));
        PLUMES.add(new WaterPlume(sx, sy, sz, scale, 0, 26));
        RandomSource rng = level.random;
        for (int i = 0; i < (int) (140 * scale); i++) {
            double spX = (rng.nextDouble() - 0.5) * 1.6 * scale, spZ = (rng.nextDouble() - 0.5) * 1.6 * scale;
            level.addAlwaysVisibleParticle(ParticleTypes.SPLASH, true, sx + spX, sy + rng.nextDouble() * 0.5, sz + spZ, spX * 0.5, (0.55 + rng.nextDouble() * 1.45) * scale, spZ * 0.5);
        }
        level.playLocalSound(sx, sy, sz, SPLASH_SFX, SoundSource.PLAYERS, 1.85f * scale, 0.94f + rng.nextFloat() * 0.12f, false);
    }
}