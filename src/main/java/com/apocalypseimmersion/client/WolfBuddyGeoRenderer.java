package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.google.gson.*;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WolfBuddyGeoRenderer {
    private static final ResourceLocation CHARM_GEO = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "models/charm/wolf_buddy_charm.geo.json");
    private record GeoCube(String bone, float ox, float oy, float oz, float sx, float sy, float sz, float rotZ, float px, float py, float pz, List<Float> u0, List<Float> v0, List<Float> u1, List<Float> v1) {}
    private static List<GeoCube> CUBES = null;
    private static float gunSwayX = 0f, gunSwayY = 0f, recoilKick = 0f, boxRoll = 0f, boxRollVel = 0f, boxPitch = 0f, boxPitchVel = 0f, starRoll = 0f, starRollVel = 0f, lastYaw = 0f, lastPitch = 0f, adsLerp = 0f;

    @SubscribeEvent
    public static void onGunShot(PlaySoundEvent event) {
        if (event.getSound() == null) return;
        String id = event.getSound().getLocation().toString().toLowerCase();
        if (id.contains("shoot") || id.contains("fire") || id.contains("shot")) {
            recoilKick = Math.min(1.8f, recoilKick + 1.0f); boxPitchVel += 14.0f; boxRollVel += 6.5f;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance(); Player p = mc.player;
        if (p == null) return;
        float dy = Mth.wrapDegrees(p.getYRot() - lastYaw), dp = p.getXRot() - lastPitch;
        lastYaw = p.getYRot(); lastPitch = p.getXRot();
        float speed = (float) p.getDeltaMovement().horizontalDistance(), walkCycle = p.tickCount * 0.52f;
        gunSwayX = Mth.lerp(0.28f, gunSwayX, Mth.clamp(-dy * 1.15f + Mth.cos(walkCycle) * speed * 14f, -24f, 24f));
        gunSwayY = Mth.lerp(0.28f, gunSwayY, Mth.clamp(dp * 0.95f + Math.abs(Mth.sin(walkCycle)) * speed * 10f - (float) p.getDeltaMovement().y * 12f, -20f, 20f));
        recoilKick *= 0.68f; adsLerp = Mth.lerp(0.32f, adsLerp, mc.options.keyUse.isDown() ? 1.0f : 0.0f);
        boxRollVel = (boxRollVel - dy * 0.55f + Mth.sin(walkCycle) * speed * 8.0f - boxRoll * 0.18f) * 0.84f;
        boxPitchVel = (boxPitchVel + dp * 0.48f - (float) p.getDeltaMovement().y * 7.5f - boxPitch * 0.18f) * 0.84f;
        boxRoll += boxRollVel; boxPitch += boxPitchVel;
        if (boxRoll < -3.5f) { boxRoll = -3.5f; boxRollVel = Math.abs(boxRollVel) * 0.52f; }
        else if (boxRoll > 36.0f) { boxRoll = 36.0f; boxRollVel = -Math.abs(boxRollVel) * 0.45f; }
        boxPitch = Mth.clamp(boxPitch, -28.0f, 34.0f);
        starRollVel = (starRollVel + (boxRoll - starRoll) * 0.28f - starRoll * 0.12f) * 0.82f;
        starRoll = Mth.clamp(starRoll + starRollVel, -8.0f, 46.0f);
    }
    private static void ensureGeoLoaded() {
        if (CUBES != null) return;
        CUBES = new ArrayList<>();
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(CHARM_GEO).orElseThrow();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray bones = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            List<String> faces = List.of("north", "south", "west", "east", "up", "down");
            for (JsonElement be : bones) {
                JsonObject b = be.getAsJsonObject(); String bName = b.get("name").getAsString();
                if (!b.has("cubes")) continue;
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject(); JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size");
                    float rz = c.has("rotation") ? c.getAsJsonArray("rotation").get(2).getAsFloat() : 0f;
                    float px = c.has("pivot") ? c.getAsJsonArray("pivot").get(0).getAsFloat() : 0f;
                    float py = c.has("pivot") ? c.getAsJsonArray("pivot").get(1).getAsFloat() : 0f;
                    float pz = c.has("pivot") ? c.getAsJsonArray("pivot").get(2).getAsFloat() : 0f;
                    JsonObject uvObj = c.getAsJsonObject("uv");
                    List<Float> u0 = new ArrayList<>(), v0 = new ArrayList<>(), u1 = new ArrayList<>(), v1 = new ArrayList<>();
                    for (String face : faces) {
                        JsonObject f = uvObj.getAsJsonObject(face); JsonArray u = f.getAsJsonArray("uv"), us = f.getAsJsonArray("uv_size");
                        u0.add(u.get(0).getAsFloat() / 256f); v0.add(u.get(1).getAsFloat() / 256f);
                        u1.add((u.get(0).getAsFloat() + us.get(0).getAsFloat()) / 256f); v1.add((u.get(1).getAsFloat() + us.get(1).getAsFloat()) / 256f);
                    }
                    CUBES.add(new GeoCube(bName, o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat(), s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat(), rz, px, py, pz, u0, v0, u1, v1));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance(); Player p = mc.player;
        if (p == null || mc.screen != null || !mc.options.getCameraType().isFirstPerson() || !WolfBuddyCharmSlot.isCharmEquipped(p.getMainHandItem())) return;
        ensureGeoLoaded(); if (CUBES == null || CUBES.isEmpty()) return;

        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        float baseX = Mth.lerp(adsLerp, sw * 0.635f, sw * 0.558f) + gunSwayX + recoilKick * 4.5f;
        float baseY = Mth.lerp(adsLerp, sh * 0.605f, sh * 0.555f) + gunSwayY - recoilKick * 5.5f;

        event.getGuiGraphics().flush();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        PoseStack ps = event.getGuiGraphics().pose();
        ps.pushPose(); ps.translate(baseX, baseY, 120.0f);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(adsLerp, -32f, -22f) + gunSwayX * 0.35f));
        ps.mulPose(Axis.XP.rotationDegrees(185f + recoilKick * 6.0f));
        ps.scale(8.5f, 8.5f, 8.5f);

        MultiBufferSource.BufferSource bufSrc = event.getGuiGraphics().bufferSource();
        VertexConsumer vc = bufSrc.getBuffer(RenderType.entityCutoutNoCull(WolfBuddyCharmSlot.CHARM_TEX));

        for (GeoCube c : CUBES) {
            ps.pushPose();
            if (c.bone.equals("charm")) { ps.mulPose(Axis.ZP.rotationDegrees(boxRoll)); ps.mulPose(Axis.XP.rotationDegrees(boxPitch)); }
            else if (c.bone.equals("star_dangler")) { ps.mulPose(Axis.ZP.rotationDegrees(boxRoll)); ps.mulPose(Axis.XP.rotationDegrees(boxPitch)); ps.translate(0f, -5.35f, 0f); ps.mulPose(Axis.ZP.rotationDegrees(starRoll - boxRoll)); ps.translate(0f, 5.35f, 0f); }
            if (c.rotZ != 0f) { ps.translate(c.px, c.py, c.pz); ps.mulPose(Axis.ZP.rotationDegrees(c.rotZ)); ps.translate(-c.px, -c.py, -c.pz); }
            emitCube(vc, ps.last(), c);
            ps.popPose();
        }
        bufSrc.endBatch();
        ps.popPose();
    }

    private static void emitCube(VertexConsumer vc, PoseStack.Pose pose, GeoCube c) {
        Matrix4f m = pose.pose(); Matrix3f n = pose.normal();
        float x0 = c.ox, y0 = c.oy, z0 = c.oz, x1 = c.ox + c.sx, y1 = c.oy + c.sy, z1 = c.oz + c.sz;
        quad(vc,m,n, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, c.u0.get(0),c.v0.get(0),c.u1.get(0),c.v1.get(0), 0,0,-1);
        quad(vc,m,n, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, c.u0.get(1),c.v0.get(1),c.u1.get(1),c.v1.get(1), 0,0,1);
        quad(vc,m,n, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, c.u0.get(2),c.v0.get(2),c.u1.get(2),c.v1.get(2), -1,0,0);
        quad(vc,m,n, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, c.u0.get(3),c.v0.get(3),c.u1.get(3),c.v1.get(3), 1,0,0);
        quad(vc,m,n, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, c.u0.get(4),c.v0.get(4),c.u1.get(4),c.v1.get(4), 0,1,0);
        quad(vc,m,n, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, c.u0.get(5),c.v0.get(5),c.u1.get(5),c.v1.get(5), 0,-1,0);
    }
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3, float u0,float v0,float u1,float v1, float nx,float ny,float nz) {
        int L = LightTexture.FULL_BRIGHT, O = OverlayTexture.NO_OVERLAY;
        vc.vertex(m,x0,y0,z0).color(255,255,255,255).uv(u0,v1).overlayCoords(O).uv2(L).normal(n,nx,ny,nz).endVertex();
        vc.vertex(m,x1,y1,z1).color(255,255,255,255).uv(u1,v1).overlayCoords(O).uv2(L).normal(n,nx,ny,nz).endVertex();
        vc.vertex(m,x2,y2,z2).color(255,255,255,255).uv(u1,v0).overlayCoords(O).uv2(L).normal(n,nx,ny,nz).endVertex();
        vc.vertex(m,x3,y3,z3).color(255,255,255,255).uv(u0,v0).overlayCoords(O).uv2(L).normal(n,nx,ny,nz).endVertex();
    }
}