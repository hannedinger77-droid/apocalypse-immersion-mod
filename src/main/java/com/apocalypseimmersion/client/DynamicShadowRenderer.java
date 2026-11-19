package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    private static boolean isFlashlightOn(ItemStack s) {
        if (!TacticalLightHelper.isFlashlight(s)) return false;
        CompoundTag t = s.getTag();
        if (t == null) return true;
        if (t.contains("on")) return t.getBoolean("on");
        if (t.contains("isOn")) return t.getBoolean("isOn");
        if (t.contains("active")) return t.getBoolean("active");
        return !t.contains("lit") || t.getBoolean("lit");
    }

    private static boolean hasFlashlight(Player p) { return TacticalLightHelper.hasAnyActiveFlashlight(p); }

    private static boolean isGlassOrSeeThrough(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof IronBarsBlock || s.is(BlockTags.LEAVES)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        return id != null && (id.getPath().contains("glass") || id.getPath().contains("window") || id.getPath().contains("grate"));
    }

    private static Vec3 findLight(Level lvl, Player p) {
        if (hasFlashlight(p)) {
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
        if (lvl == null || p == null) return;
        boolean fl = hasFlashlight(p); Vec3 lp = findLight(lvl, p); if (lp == null) return;
        Vec3 look = p.getLookAngle().normalize(); double maxR = fl ? 16.0 : 12.0;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack(); ps.pushPose(); ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader); RenderSystem.setShaderTexture(0, SHADOW_TEX);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = ps.last().pose(); boolean any = false;

        // 1. Long stretched directional shadows from players & entities (like Photo 5)
        for (Entity ent : lvl.getEntities(null, p.getBoundingBox().inflate(maxR))) {
            if (!(ent instanceof LivingEntity liv) || liv.isSpectator() || liv.isInvisible() || (fl && liv == p)) continue;
            Vec3 ep = liv.position(), toE = ep.subtract(lp); double d = toE.length();
            if (d < 0.35 || d > maxR || (fl && toE.normalize().dot(look) < 0.68)) continue;
            double dx = toE.x / d, dz = toE.z / d;
            float str = Mth.clamp((float) ((fl ? 3.2 : 2.6) + (2.5 / Math.max(0.5, d * 0.35))), 2.2F, 5.5F);
            float a = Mth.clamp((float) ((fl ? 0.72 : 0.62) * (1.0 - d / (maxR + 1.0))), 0.12F, 0.72F);
            BlockPos g = BlockPos.containing(ep.x, ep.y - 0.1, ep.z);
            while (g.getY() > ep.y - 4 && lvl.isEmptyBlock(g)) g = g.below();
            if (lvl.getBlockState(g).isSolidRender(lvl, g)) {
                any = true;
                drawStretchedShadow(buf, m, ep.x, g.getY() + 1.006F, ep.z, dx, dz, liv.getBbWidth() * 0.68F, str, a);
            }
        }

        // 2. Long stretched directional shadows starting from the base of blocks & stretching across the floor
        int rScan = fl ? 12 : 8; BlockPos pc = p.blockPosition();
        for (int x = -rScan; x <= rScan; x++) for (int y = -1; y <= 3; y++) for (int z = -rScan; z <= rScan; z++) {
            BlockPos bp = pc.offset(x, y, z); BlockState st = lvl.getBlockState(bp);
            if (!st.isSolidRender(lvl, bp) || isGlassOrSeeThrough(st) || lvl.getLightEmission(bp) > 0) continue;
            if (!lvl.isEmptyBlock(bp.below()) && lvl.getBlockState(bp.below()).isSolidRender(lvl, bp.below())) {
                Vec3 bc = Vec3.atBottomCenterOf(bp), toB = bc.subtract(lp); double d = toB.length();
                if (d < 1.0 || d > maxR || (fl && toB.normalize().dot(look) < 0.70)) continue;
                double bdx = toB.x / d, bdz = toB.z / d;
                // Only cast floor shadow if the side away from the light has open floor to stretch onto
                BlockPos frontFloor = BlockPos.containing(bc.x + bdx * 0.85, bp.getY() - 0.5, bc.z + bdz * 0.85);
                if (lvl.isEmptyBlock(frontFloor) && lvl.getBlockState(frontFloor.below()).isSolidRender(lvl, frontFloor.below())) {
                    any = true;
                    float str = Mth.clamp((float) ((fl ? 2.8 : 2.2) + (2.0 / Math.max(0.6, d * 0.4))), 1.8F, 4.6F);
                    float a = Mth.clamp((float) ((fl ? 0.64 : 0.54) * (1.0 - d / (maxR + 1.0))), 0.10F, 0.64F);
                    drawStretchedShadow(buf, m, bc.x + bdx * 0.48, bp.getY() + 0.006F, bc.z + bdz * 0.48, bdx, bdz, 0.52F, str, a);
                }
            }
        }
        if (any) tes.end();
        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void drawStretchedShadow(BufferBuilder b, Matrix4f m, double sx, float y, double sz, double dx, double dz, float hw, float len, float a) {
        double px = -dz * hw, pz = dx * hw, ex = sx + dx * len, ez = sz + dz * len;
        b.vertex(m, (float)(sx - px), y, (float)(sz - pz)).uv(0F, 0F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, (float)(sx + px), y, (float)(sz + pz)).uv(1F, 0F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, (float)(ex + px * 1.35), y, (float)(ez + pz * 1.35)).uv(1F, 1F).color(1F, 1F, 1F, a * 0.25F).endVertex();
        b.vertex(m, (float)(ex - px * 1.35), y, (float)(ez - pz * 1.35)).uv(0F, 1F).color(1F, 1F, 1F, a * 0.25F).endVertex();
    }
}