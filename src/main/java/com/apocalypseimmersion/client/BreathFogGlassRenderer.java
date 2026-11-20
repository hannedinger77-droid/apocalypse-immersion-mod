package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BreathFogGlassRenderer {
    private static final Map<BlockPos, Float> FOG = new HashMap<>();
    private static int ticks = 0;

    private static boolean isGlass(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof IronBarsBlock) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
            return id != null && (id.getPath().contains("glass") || id.getPath().contains("window") || id.getPath().contains("pane"));
        }
        return false;
    }

    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !ApocalypseImmersionConfig.ENABLE_BREATH_FOG_GLASS.get()) { FOG.clear(); return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        ticks++;
        if ((ticks % (mc.player.isSprinting() ? 32 : 56)) < 24 && mc.player.pick(2.8D, 0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos p = hit.getBlockPos().offset(x, y, z);
                if (isGlass(mc.level.getBlockState(p))) FOG.put(p.immutable(), Math.min(1F, FOG.getOrDefault(p, 0F) + (x == 0 && y == 0 && z == 0 ? 0.18F : 0.09F)));
            }
        }
        FOG.entrySet().removeIf(en -> { float cur = en.getValue(); float v = cur - (0.0012F + cur * 0.0045F); en.setValue(v); return v <= 0.002F || !isGlass(mc.level.getBlockState(en.getKey())); });
    }
    @SubscribeEvent public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !ApocalypseImmersionConfig.ENABLE_BREATH_FOG_GLASS.get() || FOG.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
        float maxA = ApocalypseImmersionConfig.BREATH_FOG_VISIBILITY.get().floatValue();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack(); ps.pushPose(); ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, new ResourceLocation("apocalypseimmersion", "textures/misc/breath_fog.png"));
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        if (buf.building()) return; buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = ps.last().pose();
        for (var en : FOG.entrySet()) {
            BlockPos p = en.getKey(); float raw = en.getValue(); float smooth = raw * raw * (3.0F - 2.0F * raw); float a = Math.min(0.95F, smooth * maxA); if (a <= 0.003F) continue;
            float x = p.getX(), y = p.getY(), z = p.getZ();
            if (mc.level.getBlockState(p).getBlock() instanceof IronBarsBlock) {
                q(buf, m, x, y, z + 0.42F, x + 1, y + 1, z + 0.42F, a, true); q(buf, m, x, y, z + 0.58F, x + 1, y + 1, z + 0.58F, a, true);
                q(buf, m, x + 0.42F, y, z, x + 0.42F, y + 1, z + 1, a, false); q(buf, m, x + 0.58F, y, z, x + 0.58F, y + 1, z + 1, a, false);
            } else {
                for (Direction d : Direction.values()) if (!isGlass(mc.level.getBlockState(p.relative(d)))) f(buf, m, x, y, z, d, a);
            }
        }
        tes.end(); RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }
    private static void q(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float a, boolean xy) {
        b.vertex(m, x1, y1, z1).uv(0F, 1F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, xy ? x2 : x1, y1, xy ? z1 : z2).uv(1F, 1F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, x2, y2, z2).uv(1F, 0F).color(1F, 1F, 1F, a).endVertex();
        b.vertex(m, xy ? x1 : x2, y2, xy ? z2 : z1).uv(0F, 0F).color(1F, 1F, 1F, a).endVertex();
    }
    private static void f(BufferBuilder b, Matrix4f m, float x, float y, float z, Direction d, float a) {
        float e = 0.008F;
        switch (d) {
            case NORTH -> q(b, m, x, y, z - e, x + 1, y + 1, z - e, a, true);
            case SOUTH -> q(b, m, x, y, z + 1 + e, x + 1, y + 1, z + 1 + e, a, true);
            case WEST -> q(b, m, x - e, y, z, x - e, y + 1, z + 1, a, false);
            case EAST -> q(b, m, x + 1 + e, y, z, x + 1 + e, y + 1, z + 1, a, false);
            case UP -> { b.vertex(m, x, y + 1 + e, z).uv(0F, 0F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x + 1, y + 1 + e, z).uv(1F, 0F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x + 1, y + 1 + e, z + 1).uv(1F, 1F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x, y + 1 + e, z + 1).uv(0F, 1F).color(1F, 1F, 1F, a).endVertex(); }
            case DOWN -> { b.vertex(m, x, y - e, z).uv(0F, 0F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x + 1, y - e, z).uv(1F, 0F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x + 1, y - e, z + 1).uv(1F, 1F).color(1F, 1F, 1F, a).endVertex(); b.vertex(m, x, y - e, z + 1).uv(0F, 1F).color(1F, 1F, 1F, a).endVertex(); }
        }
    }
}