package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DynamicShadowRenderer {
    private static final ResourceLocation SHADOW_TEX = new ResourceLocation("apocalypseimmersion", "textures/misc/dynamic_shadow.png");

    private static boolean isSeeThrough(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof IronBarsBlock || s.is(BlockTags.LEAVES)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        return id != null && (id.getPath().contains("glass") || id.getPath().contains("window") || id.getPath().contains("grate"));
    }

    private static Vec3 findLight(Level lvl, Player p) {
        if (TacticalLightHelper.hasAnyActiveFlashlight(p)) {
            Vec3 look = p.getLookAngle();
            return p.getEyePosition().add(look.x * 0.65, -0.25 + look.y * 0.4, look.z * 0.65);
        }
        if (!CuriosCompatHelper.findLightInBeltSlot(p).isEmpty() || TacticalLightHelper.isTorchOrLantern(p.getMainHandItem()) || TacticalLightHelper.isTorchOrLantern(p.getOffhandItem())) {
            float rad = (float) Math.toRadians(p.getYRot());
            return p.position().add(-Math.sin(rad) * 0.45, 1.15, Math.cos(rad) * 0.45);
        }
        BlockPos c = p.blockPosition(); Vec3 best = null; int maxE = 7;
        for (int x = -8; x <= 8; x++) for (int y = -3; y <= 5; y++) for (int z = -8; z <= 8; z++) {
            BlockPos bp = c.offset(x, y, z); int em = lvl.getLightEmission(bp);
            if (em > maxE) { maxE = em; best = Vec3.atCenterOf(bp); }
        }
        if (best == null && lvl.isDay() && lvl.canSeeSky(c.above())) {
            float sa = lvl.getSunAngle(1.0F);
            best = p.position().add(Math.sin(sa) * 16.0, Math.max(3.5, Math.cos(sa) * 12.0), 3.0);
        }
        return best;
    }

    @SubscribeEvent public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance(); Level lvl = mc.level; Player p = mc.player;
        if (lvl == null || p == null || !ApocalypseImmersionConfig.ENABLE_DYNAMIC_SHADOWS.get()) return;
        boolean fl = TacticalLightHelper.hasAnyActiveFlashlight(p); Vec3 lp = findLight(lvl, p); if (lp == null) return;
        Vec3 look = p.getLookAngle().normalize(); double maxR = fl ? 18.0 : 12.0;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack(); ps.pushPose(); ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader); RenderSystem.setShaderTexture(0, SHADOW_TEX);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = ps.last().pose(); boolean any = false;

        for (Entity ent : lvl.getEntities(null, p.getBoundingBox().inflate(maxR))) {
            if (!(ent instanceof LivingEntity liv) || liv.isSpectator() || liv.isInvisible() || (fl && liv == p)) continue;
            Vec3 ep = liv.position(), toE = ep.subtract(lp); double d = toE.length();
            if (d < 0.35 || d > maxR || (fl && toE.normalize().dot(look) < 0.68)) continue;
            double dx = toE.x / d, dz = toE.z / d;
            float str = Mth.clamp((float) ((fl ? 3.4 : 2.6) + (2.5 / Math.max(0.5, d * 0.35))), 2.2F, 5.8F);
            float a = Mth.clamp((float) ((fl ? 0.74 : 0.62) * (1.0 - d / (maxR + 1.0))), 0.12F, 0.74F);
            BlockPos g = BlockPos.containing(ep.x, ep.y - 0.1, ep.z);
            while (g.getY() > ep.y - 4 && lvl.isEmptyBlock(g)) g = g.below();
            if (lvl.getBlockState(g).isSolidRender(lvl, g)) {
                any = true;
                drawShadow(buf, m, ep.x, g.getY() + 1.006F, ep.z, dx, dz, liv.getBbWidth() * 0.68F, str, a);
            }
        }

        int rScan = PerformanceOptimizer.getEffectiveRadius(fl ? 12 : 8); BlockPos pc = p.blockPosition();
        for (int x = -rScan; x <= rScan; x++) for (int z = -rScan; z <= rScan; z++) {
            for (int y = -2; y <= 2; y++) {
                BlockPos bp = pc.offset(x, y, z); BlockState st = lvl.getBlockState(bp);
                if (!st.isSolidRender(lvl, bp) || isSeeThrough(st) || lvl.getLightEmission(bp) > 0) continue;
                if (!lvl.getBlockState(bp.below()).isSolidRender(lvl, bp.below())) continue;

                int wallH = 1;
                while (wallH < 5 && lvl.getBlockState(bp.above(wallH)).isSolidRender(lvl, bp.above(wallH))) wallH++;

                Vec3 bc = Vec3.atBottomCenterOf(bp), toB = bc.subtract(lp); double d = toB.length();
                if (d < 1.0 || d > maxR || (fl && toB.normalize().dot(look) < 0.70)) continue;
                double bdx = toB.x / d, bdz = toB.z / d;

                BlockPos behindAir = BlockPos.containing(bc.x + bdx * 0.85, bp.getY() + 0.2, bc.z + bdz * 0.85);
                if (lvl.isEmptyBlock(behindAir) && lvl.getBlockState(behindAir.below()).isSolidRender(lvl, behindAir.below())) {
                    any = true;
                    float str = Mth.clamp((float) (((fl ? 2.6 : 2.0) + (1.8 / Math.max(0.6, d * 0.4))) * (1.0F + (wallH - 1) * 0.45F)), 1.8F, 6.5F);
                    float a = Mth.clamp((float) ((fl ? 0.66 : 0.54) * (1.0 - d / (maxR + 1.0))), 0.12F, 0.66F);
                    drawShadow(buf, m, bc.x + bdx * 0.48, bp.getY() + 0.006F, bc.z + bdz * 0.48, bdx, bdz, 0.52F, str, a);
                }
                break;
            }
        }
        if (any) tes.end();
        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void drawShadow(BufferBuilder b, Matrix4f m, double sx, float y, double sz, double dx, double dz, float hw, float len, float a) {
        double px = -dz * hw, pz = dx * hw, ex = sx + dx * len, ez = sz + dz * len;
        b.vertex(m, (float)(sx - px), y, (float)(sz - pz)).uv(0F, 0F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, (float)(sx + px), y, (float)(sz + pz)).uv(1F, 0F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, (float)(ex + px * 1.35), y, (float)(ez + pz * 1.35)).uv(1F, 1F).color(1F, 1F, 1F, a * 0.22F).endVertex();
        b.vertex(m, (float)(ex - px * 1.35), y, (float)(ez - pz * 1.35)).uv(0F, 1F).color(1F, 1F, 1F, a * 0.22F).endVertex();
    }
}