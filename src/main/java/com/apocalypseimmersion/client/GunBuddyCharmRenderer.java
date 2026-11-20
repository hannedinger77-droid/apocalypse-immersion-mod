package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunBuddyCharmRenderer {

    private static final ResourceLocation WOLF_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/misc/chibi_wolf_buddy.png");
    private static float starRoll = 0f, starRollVel = 0f, starPitch = 0f, starPitchVel = 0f, prevYaw = 0f, prevPitch = 0f;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;

        float dYaw = Mth.wrapDegrees(p.getYRot() - prevYaw), dPitch = p.getXRot() - prevPitch;
        prevYaw = p.getYRot(); prevPitch = p.getXRot();
        float walkBob = Mth.sin(p.tickCount * 0.55f) * (float) p.getDeltaMovement().horizontalDistance() * 18.0f;

        starRollVel = (starRollVel - dYaw * 0.45f + walkBob * 0.35f - starRoll * 0.16f) * 0.84f;
        starPitchVel = (starPitchVel + dPitch * 0.40f - (float) p.getDeltaMovement().y * 6.5f - starPitch * 0.16f) * 0.84f;
        starRoll += starRollVel; starPitch += starPitchVel;

        // Gun side-rail collision: star bounces off the weapon and never clips through it
        if (starRoll < -6.0f) { starRoll = -6.0f; starRollVel = Math.abs(starRollVel) * 0.55f; }
        if (starRoll > 42.0f) { starRoll = 42.0f; starRollVel = -Math.abs(starRollVel) * 0.45f; }
        starPitch = Mth.clamp(starPitch, -34.0f, 38.0f);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !GunBuddyVoiceManager.hasCharm(event.getItemStack())) return;
        Minecraft mc = Minecraft.getInstance();
        boolean aiming = mc.options.keyUse.isDown();
        PoseStack ps = event.getPoseStack();

        ps.pushPose();
        if (aiming) { ps.translate(0.115f, -0.095f, -0.44f); ps.mulPose(Axis.YP.rotationDegrees(-24.0f)); }
        else { ps.translate(0.245f, -0.155f, -0.52f); ps.mulPose(Axis.YP.rotationDegrees(-32.0f)); }
        ps.mulPose(Axis.XP.rotationDegrees(4.0f));

        float boxW = 0.046f, boxH = 0.046f, boxD = 0.016f;
        float pulse = GunBuddyVoiceManager.talkGlowTicks > 0 ? (0.85f + 0.15f * Mth.sin((mc.player.tickCount + event.getPartialTick()) * 1.4f)) : 0.94f;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();

        // 1. Purple square box casing (#6A1B9A)
        drawBox(buf, m, -boxW, -boxH, -boxD, boxW, boxH, boxD, 0.42f, 0.10f, 0.68f, 1.0f);

        // 2. Full-bright Glowing Neon-Red outer frame
        float fw = boxW + 0.0055f, fh = boxH + 0.0055f, fd = boxD + 0.002f;
        drawBox(buf, m, -fw, fh - 0.005f, -fd, fw, fh, fd, 1.0f, 0.06f * pulse, 0.22f * pulse, 1.0f);
        drawBox(buf, m, -fw, -fh, -fd, fw, -fh + 0.005f, fd, 1.0f, 0.06f * pulse, 0.22f * pulse, 1.0f);
        drawBox(buf, m, -fw, -fh, -fd, -fw + 0.005f, fh, fd, 1.0f, 0.06f * pulse, 0.22f * pulse, 1.0f);
        drawBox(buf, m, fw - 0.005f, -fh, -fd, fw, fh, fd, 1.0f, 0.06f * pulse, 0.22f * pulse, 1.0f);

        // 3. Chain link + Dangling 3D Glowing Neon-Red & Purple Star Charm
        ps.pushPose();
        ps.translate(0.0f, -fh - 0.003f, 0.0f);
        ps.mulPose(Axis.ZP.rotationDegrees(starRoll));
        ps.mulPose(Axis.XP.rotationDegrees(starPitch));
        Matrix4f sm = ps.last().pose();
        drawBox(buf, sm, -0.0025f, -0.014f, -0.0025f, 0.0025f, 0.0f, 0.0025f, 0.58f, 0.18f, 0.85f, 1.0f);
        float cy = -0.028f, rOut = 0.015f, rIn = 0.0065f;
        for (int i = 0; i < 10; i++) {
            double a1 = Math.toRadians(i * 36.0 - 90.0), a2 = Math.toRadians((i + 1) * 36.0 - 90.0);
            float rad1 = (i % 2 == 0) ? rOut : rIn, rad2 = ((i + 1) % 2 == 0) ? rOut : rIn;
            buf.vertex(sm, 0f, cy, 0.003f).color(0.55f, 0.12f, 0.85f, 1.0f).endVertex();
            buf.vertex(sm, (float) Math.cos(a1) * rad1, cy - (float) Math.sin(a1) * rad1, 0.002f).color(1.0f, 0.05f, 0.22f, 1.0f).endVertex();
            buf.vertex(sm, (float) Math.cos(a2) * rad2, cy - (float) Math.sin(a2) * rad2, 0.002f).color(1.0f, 0.05f, 0.22f, 1.0f).endVertex();
            buf.vertex(sm, 0f, cy, -0.003f).color(0.55f, 0.12f, 0.85f, 1.0f).endVertex();
        }
        ps.popPose();
        tes.end();

        // 4. Additive Neon-Red Bloom Halo (glows brightly in the dark!)
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float gw = fw + 0.0095f, gh = fh + 0.0095f;
        buf.vertex(m, -gw, -gh, fd + 0.001f).color(1.0f, 0.04f, 0.20f, 0.38f * pulse).endVertex();
        buf.vertex(m, gw, -gh, fd + 0.001f).color(1.0f, 0.04f, 0.20f, 0.38f * pulse).endVertex();
        buf.vertex(m, gw, gh, fd + 0.001f).color(1.0f, 0.04f, 0.20f, 0.38f * pulse).endVertex();
        buf.vertex(m, -gw, gh, fd + 0.001f).color(1.0f, 0.04f, 0.20f, 0.38f * pulse).endVertex();
        tes.end();

        // 5. Your exact Chibi Wolf texture rendered full-bright on the front face
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, WOLF_TEX);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float iw = boxW - 0.002f, ih = boxH - 0.002f, iz = boxD + 0.0015f;
        buf.vertex(m, -iw, -ih, iz).uv(0f, 1f).color(1f, 1f, 1f, 1f).endVertex();
        buf.vertex(m, iw, -ih, iz).uv(1f, 1f).color(1f, 1f, 1f, 1f).endVertex();
        buf.vertex(m, iw, ih, iz).uv(1f, 0f).color(1f, 1f, 1f, 1f).endVertex();
        buf.vertex(m, -iw, ih, iz).uv(0f, 0f).color(1f, 1f, 1f, 1f).endVertex();
        tes.end();

        RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void drawBox(BufferBuilder b, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float bl, float a) {
        b.vertex(m, x0, y0, z1).color(r, g, bl, a).endVertex(); b.vertex(m, x1, y0, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex(); b.vertex(m, x0, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x1, y0, z0).color(r, g, bl, a).endVertex(); b.vertex(m, x0, y0, z0).color(r, g, bl, a).endVertex();
        b.vertex(m, x0, y1, z0).color(r, g, bl, a).endVertex(); b.vertex(m, x1, y1, z0).color(r, g, bl, a).endVertex();
        b.vertex(m, x0, y1, z1).color(r, g, bl, a).endVertex(); b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x1, y1, z0).color(r, g, bl, a).endVertex(); b.vertex(m, x0, y1, z0).color(r, g, bl, a).endVertex();
        b.vertex(m, x0, y0, z0).color(r, g, bl, a).endVertex(); b.vertex(m, x1, y0, z0).color(r, g, bl, a).endVertex();
        b.vertex(m, x1, y0, z1).color(r, g, bl, a).endVertex(); b.vertex(m, x0, y0, z1).color(r, g, bl, a).endVertex();
    }
}