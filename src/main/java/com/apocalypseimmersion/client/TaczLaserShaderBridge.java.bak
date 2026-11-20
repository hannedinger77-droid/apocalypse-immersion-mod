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

    private static boolean hasTaczFlashlight(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (TacticalLightHelper.hasAnyActiveFlashlight(Minecraft.getInstance().player)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().toLowerCase().contains("tacz")) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        String nbt = tag.toString().toLowerCase();
        return nbt.contains("flashlight") || nbt.contains("light") || nbt.contains("m300") || nbt.contains("m600")
                || nbt.contains("x300") || nbt.contains("surefire") || nbt.contains("inforce") || nbt.contains("streamlight");
    }

    private static float[] getLaserRgb(ItemStack stack) {
        String nbt = stack.getTag() != null ? stack.getTag().toString().toLowerCase() : "";
        if (nbt.contains("green")) return new float[]{0.15F, 1.0F, 0.25F};
        if (nbt.contains("blue") || nbt.contains("cyan")) return new float[]{0.15F, 0.65F, 1.0F};
        if (nbt.contains("ir") || nbt.contains("purple")) return new float[]{0.85F, 0.20F, 1.0F};
        return new float[]{1.0F, 0.12F, 0.12F};
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return;

        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        boolean laser = hasTaczLaser(main) || hasTaczLaser(off);
        boolean flashlight = hasTaczFlashlight(main) || hasTaczFlashlight(off);
        if (!laser && !flashlight) return;

        Vec3 eye = p.getEyePosition(event.getPartialTick());
        Vec3 look = p.getViewVector(event.getPartialTick()).normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        boolean aiming = mc.options.keyUse.isDown();

        Vec3 start = aiming
                ? eye.add(right.scale(0.03)).add(up.scale(-0.07)).add(look.scale(0.45))
                : eye.add(right.scale(0.18)).add(up.scale(-0.11)).add(look.scale(0.45));

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

        if (flashlight) {
            double beamDist = aiming ? 26.0 : 20.0;
            BlockHitResult fHit = mc.level.clip(new ClipContext(eye, eye.add(look.scale(beamDist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 fEnd = fHit.getType() != HitResult.Type.MISS ? fHit.getLocation() : eye.add(look.scale(beamDist));
            double actualLen = start.distanceTo(fEnd);

            float r0 = 0.055F;
            float rOuter = (float) Math.min(2.8, 0.22 + actualLen * (aiming ? 0.085 : 0.11));
            float rInner = rOuter * 0.48F;
            int segs = 16;

            for (int i = 0; i < segs; i++) {
                double a1 = (Math.PI * 2.0 * i) / segs;
                double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
                Vec3 dir1 = right.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
                Vec3 dir2 = right.scale(Math.cos(a2)).add(up.scale(Math.sin(a2)));

                Vec3 s1 = start.add(dir1.scale(r0)), s2 = start.add(dir2.scale(r0));
                Vec3 e1 = fEnd.add(dir1.scale(rOuter)), e2 = fEnd.add(dir2.scale(rOuter));
                buf.vertex(m, (float) s1.x, (float) s1.y, (float) s1.z).color(0.92F, 0.96F, 1.0F, aiming ? 0.16F : 0.13F).endVertex();
                buf.vertex(m, (float) s2.x, (float) s2.y, (float) s2.z).color(0.92F, 0.96F, 1.0F, aiming ? 0.16F : 0.13F).endVertex();
                buf.vertex(m, (float) e2.x, (float) e2.y, (float) e2.z).color(0.85F, 0.92F, 1.0F, 0.0F).endVertex();
                buf.vertex(m, (float) e1.x, (float) e1.y, (float) e1.z).color(0.85F, 0.92F, 1.0F, 0.0F).endVertex();

                Vec3 ie1 = fEnd.add(dir1.scale(rInner)), ie2 = fEnd.add(dir2.scale(rInner));
                buf.vertex(m, (float) s1.x, (float) s1.y, (float) s1.z).color(1.0F, 0.99F, 0.95F, aiming ? 0.24F : 0.19F).endVertex();
                buf.vertex(m, (float) s2.x, (float) s2.y, (float) s2.z).color(1.0F, 0.99F, 0.95F, aiming ? 0.24F : 0.19F).endVertex();
                buf.vertex(m, (float) ie2.x, (float) ie2.y, (float) ie2.z).color(0.95F, 0.97F, 1.0F, 0.02F).endVertex();
                buf.vertex(m, (float) ie1.x, (float) ie1.y, (float) ie1.z).color(0.95F, 0.97F, 1.0F, 0.02F).endVertex();
            }

            if (fHit.getType() != HitResult.Type.MISS) {
                Vec3 hitCenter = fEnd.subtract(look.scale(0.04));
                float spotR = rInner * 0.95F;
                for (int i = 0; i < segs; i++) {
                    double a1 = (Math.PI * 2.0 * i) / segs;
                    double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
                    Vec3 p1 = hitCenter.add(right.scale(Math.cos(a1) * spotR)).add(up.scale(Math.sin(a1) * spotR));
                    Vec3 p2 = hitCenter.add(right.scale(Math.cos(a2) * spotR)).add(up.scale(Math.sin(a2) * spotR));
                    buf.vertex(m, (float) hitCenter.x, (float) hitCenter.y, (float) hitCenter.z).color(1.0F, 0.99F, 0.96F, 0.34F).endVertex();
                    buf.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(0.92F, 0.96F, 1.0F, 0.0F).endVertex();
                    buf.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(0.92F, 0.96F, 1.0F, 0.0F).endVertex();
                    buf.vertex(m, (float) hitCenter.x, (float) hitCenter.y, (float) hitCenter.z).color(1.0F, 0.99F, 0.96F, 0.34F).endVertex();
                }
            }
        }

        if (laser) {
            ItemStack gun = hasTaczLaser(main) ? main : off;
            float[] rgb = getLaserRgb(gun);
            Vec3 maxEnd = eye.add(look.scale(64.0));
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 end = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : maxEnd;

            Vec3 perp = right.scale(0.0085);
            buf.vertex(m, (float)(start.x - perp.x), (float)(start.y - perp.y), (float)(start.z - perp.z)).color(rgb[0], rgb[1], rgb[2], 0.55F).endVertex();
            buf.vertex(m, (float)(start.x + perp.x), (float)(start.y + perp.y), (float)(start.z + perp.z)).color(rgb[0], rgb[1], rgb[2], 0.55F).endVertex();
            buf.vertex(m, (float)(end.x + perp.x), (float)(end.y + perp.y), (float)(end.z + perp.z)).color(rgb[0], rgb[1], rgb[2], 0.35F).endVertex();
            buf.vertex(m, (float)(end.x - perp.x), (float)(end.y - perp.y), (float)(end.z - perp.z)).color(rgb[0], rgb[1], rgb[2], 0.35F).endVertex();
        }

        tes.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }
}