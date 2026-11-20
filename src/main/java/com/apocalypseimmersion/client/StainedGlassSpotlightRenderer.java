package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StainedGlassSpotlightRenderer {

    private static DyeColor getStainedGlassColor(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof BeaconBeamBlock beamBlock) {
            return beamBlock.getColor();
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Level lvl = mc.level;
        if (lvl == null || mc.player == null) return;

        Vec3 cam = event.getCamera().getPosition();
        BlockPos pc = mc.player.blockPosition();

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
        boolean any = false;

        int r = 20;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -6; dy <= 12; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos glassPos = pc.offset(dx, dy, dz);
                    BlockState glassState = lvl.getBlockState(glassPos);
                    DyeColor dye = getStainedGlassColor(glassState);
                    if (dye == null) continue;

                    // Works for EVERY light source adjacent to stained glass (especially underneath a light!)
                    for (Direction inward : Direction.values()) {
                        BlockPos lightPos = glassPos.relative(inward);
                        int emission = lvl.getLightEmission(lightPos);
                        if (emission < 8) continue;

                        Direction beamDir = inward.getOpposite();

                        // Trace beam through open air until it lands on a floor/wall surface (up to 16 blocks)
                        int reach = 1;
                        BlockPos probe = glassPos.relative(beamDir);
                        while (reach < 16 && !lvl.getBlockState(probe).isSolidRender(lvl, probe)) {
                            reach++;
                            probe = glassPos.relative(beamDir, reach);
                        }

                        float[] rgb = dye.getTextureDiffuseColors();
                        float intensity = Math.min(1.35F, emission / 12.0F);
                        float cr = Math.min(1.0F, rgb[0] * intensity);
                        float cg = Math.min(1.0F, rgb[1] * intensity);
                        float cb = Math.min(1.0F, rgb[2] * intensity);

                        Vec3 dirVec = Vec3.atLowerCornerOf(beamDir.getNormal());
                        Vec3 start = Vec3.atCenterOf(glassPos).add(dirVec.scale(0.46));
                        Vec3 end = Vec3.atCenterOf(glassPos).add(dirVec.scale(reach - 0.505));

                        Vec3 axisA = (beamDir.getAxis() == Direction.Axis.Y) ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                        Vec3 axisB = dirVec.cross(axisA).normalize();

                        float rStart = 0.42F;
                        float rOuter = Math.min(2.8F, 0.62F + reach * 0.18F);
                        float rInner = rOuter * 0.52F;
                        int segs = 14;

                        // Dual-layer volumetric spotlight beam (bright core + soft colored wash like reference photos)
                        for (int i = 0; i < segs; i++) {
                            double a1 = (Math.PI * 2.0 * i) / segs;
                            double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
                            Vec3 rad1 = axisA.scale(Math.cos(a1)).add(axisB.scale(Math.sin(a1)));
                            Vec3 rad2 = axisA.scale(Math.cos(a2)).add(axisB.scale(Math.sin(a2)));

                            // Outer soft colored wash
                            Vec3 s1 = start.add(rad1.scale(rStart)), s2 = start.add(rad2.scale(rStart));
                            Vec3 e1 = end.add(rad1.scale(rOuter)), e2 = end.add(rad2.scale(rOuter));
                            buf.vertex(m, (float) s1.x, (float) s1.y, (float) s1.z).color(cr, cg, cb, 0.24F).endVertex();
                            buf.vertex(m, (float) s2.x, (float) s2.y, (float) s2.z).color(cr, cg, cb, 0.24F).endVertex();
                            buf.vertex(m, (float) e2.x, (float) e2.y, (float) e2.z).color(cr, cg, cb, 0.03F).endVertex();
                            buf.vertex(m, (float) e1.x, (float) e1.y, (float) e1.z).color(cr, cg, cb, 0.03F).endVertex();

                            // Inner hot colored spotlight core
                            Vec3 ie1 = end.add(rad1.scale(rInner)), ie2 = end.add(rad2.scale(rInner));
                            buf.vertex(m, (float) s1.x, (float) s1.y, (float) s1.z).color(Math.min(1.0F, cr + 0.18F), Math.min(1.0F, cg + 0.18F), Math.min(1.0F, cb + 0.18F), 0.34F).endVertex();
                            buf.vertex(m, (float) s2.x, (float) s2.y, (float) s2.z).color(Math.min(1.0F, cr + 0.18F), Math.min(1.0F, cg + 0.18F), Math.min(1.0F, cb + 0.18F), 0.34F).endVertex();
                            buf.vertex(m, (float) ie2.x, (float) ie2.y, (float) ie2.z).color(cr, cg, cb, 0.06F).endVertex();
                            buf.vertex(m, (float) ie1.x, (float) ie1.y, (float) ie1.z).color(cr, cg, cb, 0.06F).endVertex();
                        }

                        // Rich colored floor/wall glow pool & reflection splash where the beam hits
                        if (lvl.getBlockState(probe).isSolidRender(lvl, probe)) {
                            for (int i = 0; i < segs; i++) {
                                double a1 = (Math.PI * 2.0 * i) / segs;
                                double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
                                Vec3 p1 = end.add(axisA.scale(Math.cos(a1) * rOuter)).add(axisB.scale(Math.sin(a1) * rOuter));
                                Vec3 p2 = end.add(axisA.scale(Math.cos(a2) * rOuter)).add(axisB.scale(Math.sin(a2) * rOuter));

                                buf.vertex(m, (float) end.x, (float) end.y, (float) end.z).color(cr, cg, cb, 0.58F).endVertex();
                                buf.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(cr, cg, cb, 0.0F).endVertex();
                                buf.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(cr, cg, cb, 0.0F).endVertex();
                                buf.vertex(m, (float) end.x, (float) end.y, (float) end.z).color(cr, cg, cb, 0.58F).endVertex();
                            }
                        }
                        any = true;
                    }
                }
            }
        }

        if (any) tes.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }
}