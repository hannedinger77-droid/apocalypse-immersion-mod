package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WolfBuddyGeoRenderer {
    private static final ResourceLocation CHARM_GEO = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "models/charm/wolf_buddy_charm.geo.json");
    private record GeoCube(String bone, float ox, float oy, float oz, float sx, float sy, float sz, float rotZ, float px, float py, float pz, List<Float> u0, List<Float> v0, List<Float> u1, List<Float> v1) {}
    private static List<GeoCube> CUBES = null;
    private static float swingZ = 0f, swingZVel = 0f, swingX = 0f, swingXVel = 0f, lastYaw = 0f, lastPitch = 0f;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        float dy = Mth.wrapDegrees(p.getYRot() - lastYaw), dp = p.getXRot() - lastPitch;
        lastYaw = p.getYRot(); lastPitch = p.getXRot();
        float bob = Mth.sin(p.tickCount * 0.55f) * (float) p.getDeltaMovement().horizontalDistance() * 18.0f;
        swingZVel = (swingZVel - dy * 0.45f + bob * 0.35f - swingZ * 0.16f) * 0.84f;
        swingXVel = (swingXVel + dp * 0.40f - (float) p.getDeltaMovement().y * 6.5f - swingX * 0.16f) * 0.84f;
        swingZ = Mth.clamp(swingZ + swingZVel, -3.0f, 40.0f);
        swingX = Mth.clamp(swingX + swingXVel, -32.0f, 36.0f);
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
                JsonObject b = be.getAsJsonObject();
                String bName = b.get("name").getAsString();
                if (!b.has("cubes")) continue;
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject();
                    JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size");
                    float rz = c.has("rotation") ? c.getAsJsonArray("rotation").get(2).getAsFloat() : 0f;
                    float px = c.has("pivot") ? c.getAsJsonArray("pivot").get(0).getAsFloat() : 0f;
                    float py = c.has("pivot") ? c.getAsJsonArray("pivot").get(1).getAsFloat() : 0f;
                    float pz = c.has("pivot") ? c.getAsJsonArray("pivot").get(2).getAsFloat() : 0f;
                    JsonObject uvObj = c.getAsJsonObject("uv");
                    List<Float> u0 = new ArrayList<>(), v0 = new ArrayList<>(), u1 = new ArrayList<>(), v1 = new ArrayList<>();
                    for (String face : faces) {
                        JsonObject f = uvObj.getAsJsonObject(face);
                        JsonArray u = f.getAsJsonArray("uv"), us = f.getAsJsonArray("uv_size");
                        u0.add(u.get(0).getAsFloat() / 256f);
                        v0.add(u.get(1).getAsFloat() / 256f);
                        u1.add((u.get(0).getAsFloat() + us.get(0).getAsFloat()) / 256f);
                        v1.add((u.get(1).getAsFloat() + us.get(1).getAsFloat()) / 256f);
                    }
                    CUBES.add(new GeoCube(bName, o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat(), s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat(), rz, px, py, pz, u0, v0, u1, v1));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (!WolfBuddyCharmSlot.isCharmEquipped(p.getMainHandItem())) return;

        ensureGeoLoaded();
        if (CUBES == null || CUBES.isEmpty()) return;

        boolean aiming = mc.options.keyUse.isDown();
        Vec3 camPos = event.getCamera().getPosition();
        Vec3 eye = p.getEyePosition(event.getPartialTick());
        PoseStack ps = event.getPoseStack();

        ps.pushPose();
        ps.translate(eye.x - camPos.x, eye.y - camPos.y, eye.z - camPos.z);
        ps.mulPose(Axis.YP.rotationDegrees(-p.getViewYRot(event.getPartialTick())));
        ps.mulPose(Axis.XP.rotationDegrees(p.getViewXRot(event.getPartialTick())));

        if (aiming) { ps.translate(-0.105f, -0.045f, 0.38f); ps.mulPose(Axis.YP.rotationDegrees(20f)); }
        else { ps.translate(-0.215f, -0.085f, 0.44f); ps.mulPose(Axis.YP.rotationDegrees(28f)); }
        ps.scale(0.028f, 0.028f, 0.028f);

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, WolfBuddyCharmSlot.CHARM_TEX);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (GeoCube c : CUBES) {
            ps.pushPose();
            if (!c.bone.equals("root")) {
                float mul = c.bone.equals("star_dangler") ? 1.50f : 0.90f;
                ps.mulPose(Axis.ZP.rotationDegrees(swingZ * mul));
                ps.mulPose(Axis.XP.rotationDegrees(swingX * mul));
            }
            if (c.rotZ != 0f) { ps.translate(c.px, c.py, c.pz); ps.mulPose(Axis.ZP.rotationDegrees(c.rotZ)); ps.translate(-c.px, -c.py, -c.pz); }
            emitCube(buf, ps.last().pose(), c);
            ps.popPose();
        }
        tes.end();
        RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void emitCube(BufferBuilder b, Matrix4f m, GeoCube c) {
        float x0 = c.ox, y0 = c.oy, z0 = c.oz, x1 = c.ox + c.sx, y1 = c.oy + c.sy, z1 = c.oz + c.sz;
        v(b,m,x1,y0,z0,c.u0.get(0),c.v1.get(0)); v(b,m,x0,y0,z0,c.u1.get(0),c.v1.get(0)); v(b,m,x0,y1,z0,c.u1.get(0),c.v0.get(0)); v(b,m,x1,y1,z0,c.u0.get(0),c.v0.get(0));
        v(b,m,x0,y0,z1,c.u0.get(1),c.v1.get(1)); v(b,m,x1,y0,z1,c.u1.get(1),c.v1.get(1)); v(b,m,x1,y1,z1,c.u1.get(1),c.v0.get(1)); v(b,m,x0,y1,z1,c.u0.get(1),c.v0.get(1));
        v(b,m,x0,y0,z0,c.u0.get(2),c.v1.get(2)); v(b,m,x0,y0,z1,c.u1.get(2),c.v1.get(2)); v(b,m,x0,y1,z1,c.u1.get(2),c.v0.get(2)); v(b,m,x0,y1,z0,c.u0.get(2),c.v0.get(2));
        v(b,m,x1,y0,z1,c.u0.get(3),c.v1.get(3)); v(b,m,x1,y0,z0,c.u1.get(3),c.v1.get(3)); v(b,m,x1,y1,z0,c.u1.get(3),c.v0.get(3)); v(b,m,x1,y1,z1,c.u0.get(3),c.v0.get(3));
        v(b,m,x0,y1,z1,c.u0.get(4),c.v1.get(4)); v(b,m,x1,y1,z1,c.u1.get(4),c.v1.get(4)); v(b,m,x1,y1,z0,c.u1.get(4),c.v0.get(4)); v(b,m,x0,y1,z0,c.u0.get(4),c.v0.get(4));
        v(b,m,x0,y0,z0,c.u0.get(5),c.v1.get(5)); v(b,m,x1,y0,z0,c.u1.get(5),c.v1.get(5)); v(b,m,x1,y0,z1,c.u1.get(5),c.v0.get(5)); v(b,m,x0,y0,z1,c.u0.get(5),c.v0.get(5));
    }
    private static void v(BufferBuilder b, Matrix4f m, float x, float y, float z, float u, float v) { b.vertex(m, x, y, z).uv(u, v).color(1f, 1f, 1f, 1f).endVertex(); }
}