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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeafDappleRenderer {
    private static final ResourceLocation WINDOW_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/misc/window_light_shape.png");
    private static final ResourceLocation LEAF_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "textures/misc/leaf_dapple.png");
    private record CachedQuad(int type, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {}
    private static final List<CachedQuad> CACHE = new ArrayList<>();
    private static int tickCounter = 0;

    private static int getApertureType(BlockState s) {
        Block b = s.getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        String p = id != null ? id.getPath() : "";
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof DoorBlock || p.contains("window") || p.contains("glass") || p.contains("grate")) return 1;
        if (b instanceof IronBarsBlock || b instanceof TrapDoorBlock || b instanceof FenceBlock || s.is(BlockTags.LEAVES) || p.contains("vent") || p.contains("bars")) return 2;
        return 0;
    }

    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!ApocalypseImmersionConfig.ENABLE_LEAF_DAPPLE.get()) { CACHE.clear(); return; }
        if (++tickCounter < PerformanceOptimizer.getCacheIntervalTicks()) return;
        tickCounter = 0;
        Minecraft mc = Minecraft.getInstance(); Level lvl = mc.level;
        if (lvl == null || mc.player == null) return;
        CACHE.clear();
        BlockPos center = mc.player.blockPosition();
        int rad = PerformanceOptimizer.getEffectiveRadius(8);
        for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) for (int dy = 0; dy <= 5; dy++) {
            BlockPos pos = center.offset(dx, dy, dz); BlockState st = lvl.getBlockState(pos);
            int type = getApertureType(st);
            if (type == 0) continue;
            int bright = lvl.getMaxLocalRawBrightness(pos);
            if (bright < 10) continue;
            float alpha = Math.min(0.22F, (bright - 9) * 0.032F), r = 1.0F, g = 0.95F, b = 0.82F;
            if (st.getBlock() instanceof BeaconBeamBlock beam) {
                float[] rgb = beam.getColor().getTextureDiffuseColors();
                r = rgb[0]; g = rgb[1]; b = rgb[2];
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (lvl.getMaxLocalRawBrightness(pos.relative(dir)) >= 11) {
                    Direction inDir = dir.getOpposite();
                    for (int step = 1; step <= 3; step++) {
                        BlockPos hit = pos.relative(inDir, step);
                        if (lvl.getBlockState(hit).isSolidRender(lvl, hit)) {
                            float x = hit.getX(), y = hit.getY(), z = hit.getZ(), ep = 0.008F, wa = alpha * (1.0F - step * 0.22F);
                            if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                                float zf = (dir == Direction.NORTH) ? z - ep : z + 1 + ep;
                                CACHE.add(new CachedQuad(type, x, y, zf, x + 1, y, zf, x + 1, y + 1, zf, x, y + 1, zf, r, g, b, wa));
                            } else {
                                float xf = (dir == Direction.WEST) ? x - ep : x + 1 + ep;
                                CACHE.add(new CachedQuad(type, xf, y, z, xf, y, z + 1, xf, y + 1, z + 1, xf, y + 1, z, r, g, b, wa));
                            }
                            break;
                        }
                        if (lvl.getBlockState(hit.below()).isSolidRender(lvl, hit.below())) {
                            float fx = hit.getX(), fy = hit.getY() + 0.006F, fz = hit.getZ(), fa = alpha * (1.0F - step * 0.20F);
                            CACHE.add(new CachedQuad(type, fx, fy, fz, fx + 1, fy, fz, fx + 1, fy, fz + 1, fx, fy, fz + 1, r, g, b, fa));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || CACHE.isEmpty() || !ApocalypseImmersionConfig.ENABLE_LEAF_DAPPLE.get()) return;
        Minecraft mc = Minecraft.getInstance(); if (mc.player == null) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack(); ps.pushPose(); ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        drawPass(ps.last().pose(), 1, WINDOW_TEX); drawPass(ps.last().pose(), 2, LEAF_TEX);
        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void drawPass(Matrix4f m, int targetType, ResourceLocation tex) {
        RenderSystem.setShaderTexture(0, tex);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        if (buf.building()) return; buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean any = false;
        for (CachedQuad q : CACHE) {
            if (q.type != targetType) continue;
            any = true;
            buf.vertex(m, q.x0, q.y0, q.z0).uv(0F, 1F).color(q.r, q.g, q.b, q.a).endVertex();
            buf.vertex(m, q.x1, q.y1, q.z1).uv(1F, 1F).color(q.r, q.g, q.b, q.a).endVertex();
            buf.vertex(m, q.x2, q.y2, q.z2).uv(1F, 0F).color(q.r, q.g, q.b, q.a).endVertex();
            buf.vertex(m, q.x3, q.y3, q.z3).uv(0F, 0F).color(q.r, q.g, q.b, q.a).endVertex();
        }
        if (any) tes.end();
    }
}
