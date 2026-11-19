package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeafDappleRenderer {
    private static final ResourceLocation WINDOW_TEX = new ResourceLocation("apocalypseimmersion", "textures/misc/window_light_shape.png");
    private static final ResourceLocation LEAF_TEX = new ResourceLocation("apocalypseimmersion", "textures/misc/leaf_dapple.png");

    private static int getApertureType(BlockState s) {
        Block b = s.getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        String p = id != null ? id.getPath() : "";
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof DoorBlock || p.contains("window") || p.contains("glass") || p.contains("grate")) return 1;
        if (b instanceof IronBarsBlock || b instanceof TrapDoorBlock || b instanceof FenceBlock || s.is(BlockTags.LEAVES) || p.contains("vent") || p.contains("bars")) return 2;
        return 0;
    }

    @SubscribeEvent public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance(); Level lvl = mc.level;
        if (lvl == null || mc.player == null) return;
        Vec3 cam = e.getCamera().getPosition(); BlockPos center = mc.player.blockPosition();
        float sunAng = lvl.getSunAngle(1.0F);
        double sunX = -Math.sin(sunAng) * 0.75, sunY = -Math.max(0.35, Math.cos(sunAng));

        PoseStack ps = e.getPoseStack(); ps.pushPose(); ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        projectPass(lvl, center, ps.last().pose(), 1, WINDOW_TEX, sunX, sunY);
        projectPass(lvl, center, ps.last().pose(), 2, LEAF_TEX, sunX, sunY);

        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend(); ps.popPose();
    }

    private static void projectPass(Level lvl, BlockPos center, Matrix4f m, int targetType, ResourceLocation tex, double sunX, double sunY) {
        RenderSystem.setShaderTexture(0, tex);
        Tesselator tes = Tesselator.getInstance(); BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean any = false;

        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) for (int dy = 0; dy <= 5; dy++) {
            BlockPos pos = center.offset(dx, dy, dz); BlockState st = lvl.getBlockState(pos);
            if (getApertureType(st) != targetType) continue;
            int bright = lvl.getMaxLocalRawBrightness(pos);
            if (bright < 10) continue;

            float alpha = Math.min(0.22F, (bright - 9) * 0.032F);
            float r = 1.0F, g = 0.95F, b = 0.82F;
            if (st.getBlock() instanceof BeaconBeamBlock beam) {
                float[] rgb = beam.getColor().getTextureDiffuseColors();
                r = rgb[0]; g = rgb[1]; b = rgb[2];
            }

            // Project crisp window/grate/leaf pattern onto side walls & floor inside rooms (like Photo 1 & 3)
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (lvl.getMaxLocalRawBrightness(pos.relative(dir)) >= 11) {
                    Direction inDir = dir.getOpposite();
                    for (int step = 1; step <= 3; step++) {
                        BlockPos hit = pos.relative(inDir, step);
                        if (lvl.getBlockState(hit).isSolidRender(lvl, hit)) {
                            any = true; addWallQuad(buf, m, hit, dir, r, g, b, alpha * (1.0F - step * 0.22F));
                            break;
                        }
                        if (lvl.getBlockState(hit.below()).isSolidRender(lvl, hit.below())) {
                            any = true; float fx = hit.getX(), fy = hit.getY() + 0.006F, fz = hit.getZ(), fa = alpha * (1.0F - step * 0.20F);
                            buf.vertex(m, fx, fy, fz).uv(0F, 0F).color(r, g, b, fa).endVertex(); buf.vertex(m, fx + 1, fy, fz).uv(1F, 0F).color(r, g, b, fa).endVertex();
                            buf.vertex(m, fx + 1, fy, fz + 1).uv(1F, 1F).color(r, g, b, fa).endVertex(); buf.vertex(m, fx, fy, fz + 1).uv(0F, 1F).color(r, g, b, fa).endVertex();
                        }
                    }
                }
            }
        }
        if (any) tes.end();
    }

    private static void addWallQuad(BufferBuilder buf, Matrix4f m, BlockPos w, Direction f, float r, float g, float b, float a) {
        float x = w.getX(), y = w.getY(), z = w.getZ(), e = 0.008F;
        if (f == Direction.NORTH || f == Direction.SOUTH) {
            float zf = (f == Direction.NORTH) ? z - e : z + 1 + e;
            buf.vertex(m, x, y, zf).uv(0F, 1F).color(r, g, b, a).endVertex(); buf.vertex(m, x + 1, y, zf).uv(1F, 1F).color(r, g, b, a).endVertex();
            buf.vertex(m, x + 1, y + 1, zf).uv(1F, 0F).color(r, g, b, a).endVertex(); buf.vertex(m, x, y + 1, zf).uv(0F, 0F).color(r, g, b, a).endVertex();
        } else {
            float xf = (f == Direction.WEST) ? x - e : x + 1 + e;
            buf.vertex(m, xf, y, z).uv(0F, 1F).color(r, g, b, a).endVertex(); buf.vertex(m, xf, y, z + 1).uv(1F, 1F).color(r, g, b, a).endVertex();
            buf.vertex(m, xf, y + 1, z + 1).uv(1F, 0F).color(r, g, b, a).endVertex(); buf.vertex(m, xf, y + 1, z).uv(0F, 0F).color(r, g, b, a).endVertex();
        }
    }
}