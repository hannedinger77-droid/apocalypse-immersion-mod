package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InvisibleBedFixBridge {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        Level lvl = mc.level;
        if (lvl == null || mc.player == null) return;

        BlockPos center = mc.player.blockPosition();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -6; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = lvl.getBlockState(pos);
                    if (!(state.getBlock() instanceof BedBlock)) continue;
                    if (state.getValue(BedBlock.PART) != BedPart.FOOT) continue;

                    BlockEntity be = lvl.getBlockEntity(pos);
                    BedBlockEntity bedEntity = (be instanceof BedBlockEntity b) ? b : null;
                    if (bedEntity == null) {
                        bedEntity = new BedBlockEntity(pos, state);
                        bedEntity.setLevel(lvl);
                        lvl.setBlockEntity(bedEntity);
                    }

                    BlockEntityRenderer<BedBlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(bedEntity);
                    if (renderer != null) {
                        ps.pushPose();
                        ps.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
                        renderer.render(bedEntity, event.getPartialTick(), ps, buf, 15728880, OverlayTexture.NO_OVERLAY);
                        ps.popPose();
                    }
                }
            }
        }
        buf.endBatch();
    }
}