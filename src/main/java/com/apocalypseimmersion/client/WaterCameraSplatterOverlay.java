package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaterCameraSplatterOverlay {

    private static final ResourceLocation DROP_TEX = new ResourceLocation("apocalypseimmersion", "textures/misc/water_splatter.png");
    private record ScreenDrop(float xPct, float yPct, int size, int ttl, int maxTtl) {}
    private static final List<ScreenDrop> DROPS = new CopyOnWriteArrayList<>();

    private static boolean isBullet(Entity e) {
        if (e instanceof Projectile) return true;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id != null && (id.getPath().contains("bullet") || id.getPath().contains("projectile"));
    }

    @SubscribeEvent
    public static void onClientBullet(EntityJoinLevelEvent event) {
        if (!ApocalypseImmersionConfig.ENABLE_WATER_BULLET_SPLASH.get() || !event.getLevel().isClientSide()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isBullet(event.getEntity())) return;
        if (mc.player.distanceTo(event.getEntity()) > 4.5F) return;

        Vec3 start = event.getEntity().position();
        Vec3 vel = event.getEntity().getDeltaMovement();
        if (vel.lengthSqr() == 0.0) return;
        Vec3 end = start.add(vel.normalize().scale(14.0));
        BlockHitResult hit = event.getLevel().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.WATER, event.getEntity()));
        if (hit.getType() == HitResult.Type.BLOCK && event.getLevel().isWaterAt(hit.getBlockPos())) {
            int count = 5 + event.getLevel().random.nextInt(5);
            for (int i = 0; i < count; i++) {
                float xp = event.getLevel().random.nextFloat() * 0.90F + 0.03F;
                float yp = event.getLevel().random.nextFloat() * 0.85F + 0.05F;
                int sz = 18 + event.getLevel().random.nextInt(26);
                int life = 45 + event.getLevel().random.nextInt(30);
                DROPS.add(new ScreenDrop(xp, yp, sz, life, life));
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DROPS.isEmpty()) return;
        for (int i = DROPS.size() - 1; i >= 0; i--) {
            ScreenDrop d = DROPS.get(i);
            if (d.ttl <= 1) DROPS.remove(i);
            else DROPS.set(i, new ScreenDrop(d.xPct, d.yPct + 0.0018F, d.size, d.ttl - 1, d.maxTtl));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (DROPS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (ScreenDrop d : DROPS) {
            float alpha = Math.min(0.78F, (float) d.ttl / (float) d.maxTtl);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            int x = (int) (d.xPct * sw);
            int y = (int) (d.yPct * sh);
            g.blit(DROP_TEX, x, y, 0, 0, d.size, d.size, d.size, d.size);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}