package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import java.lang.reflect.Method;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SecurityCameraBridge {
    private static final List<BlockPos> CAMERAS = new ArrayList<>();
    private static int activeCamIndex = -1;
    private static boolean nightVisionBoost = false;

    public static void cycleNextCamera() {
        if (!ApocalypseImmersionConfig.ENABLE_SC_SHADER_CAM.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        scanNearbyCameras(mc);
        if (CAMERAS.isEmpty()) {
            activeCamIndex = -1;
            mc.player.displayClientMessage(Component.literal("\u00A7c[CCTV] No SecurityCraft cameras found within 64 blocks."), true);
            return;
        }
        activeCamIndex++;
        if (activeCamIndex >= CAMERAS.size()) {
            activeCamIndex = -1;
            mc.player.displayClientMessage(Component.literal("\u00A7a[CCTV] Disconnected from camera feed."), true);
        } else {
            BlockPos pos = CAMERAS.get(activeCamIndex);
            mc.player.displayClientMessage(Component.literal("\u00A7b[CCTV CAM #" + (activeCamIndex + 1) + "/" + CAMERAS.size() + "] \u00A7f" + pos.toShortString() + " (Press V to cycle)"), true);
        }
    }

    public static void toggleNightVision() { if (activeCamIndex >= 0) nightVisionBoost = !nightVisionBoost; }
    public static boolean isViewingCamera() { return ApocalypseImmersionConfig.ENABLE_SC_SHADER_CAM.get() && activeCamIndex >= 0 && activeCamIndex < CAMERAS.size(); }

    private static void scanNearbyCameras(Minecraft mc) {
        CAMERAS.clear();
        BlockPos center = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-64, -32, -64), center.offset(64, 32, 64))) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(mc.level.getBlockState(p).getBlock());
            if (id != null && id.getNamespace().equals("securitycraft") && id.getPath().contains("camera")) {
                CAMERAS.add(p.immutable());
            }
        }
    }

    @SubscribeEvent public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!isViewingCamera()) return;
        Minecraft mc = Minecraft.getInstance();
        Entity camEntity = mc.getCameraEntity();
        // If SecurityCraft switched cameraEntity while shaders are active, keep player as shader root and position camera cleanly
        if (camEntity != null && camEntity != mc.player) {
            mc.setCameraEntity(mc.player);
        }
        BlockPos camPos = CAMERAS.get(activeCamIndex);
        try { Method m = ObfuscationReflectionHelper.findMethod(Camera.class, "m_90584_", Vec3.class); m.invoke(event.getCamera(), Vec3.atCenterOf(camPos).add(0.0, -0.25, 0.0)); } catch (Exception ignored) {}
    }

    @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && isViewingCamera()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.isShiftKeyDown()) activeCamIndex = -1;
        }
    }

    @SubscribeEvent public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!isViewingCamera()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        if (nightVisionBoost) g.fill(0, 0, w, h, 0x2818FF44);
        BlockPos pos = CAMERAS.get(activeCamIndex);
        g.drawString(mc.font, "\u00A7c\u25CF REC \u00A7fCAM-0" + (activeCamIndex + 1) + " [" + pos.toShortString() + "]", 12, 12, 0xFFFFFF);
        g.drawString(mc.font, "\u00A77[V] Next Cam  |  [N] Night-Filter  |  [SNEAK] Exit", 12, h - 20, 0xCCCCCC);
    }
}