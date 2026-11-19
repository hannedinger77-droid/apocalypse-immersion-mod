package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientKeybinds {
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key." + ApocalypseImmersion.MODID + ".open_config", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories." + ApocalypseImmersion.MODID);
    public static final KeyMapping CYCLE_CCTV_KEY = new KeyMapping("key." + ApocalypseImmersion.MODID + ".cycle_cctv", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories." + ApocalypseImmersion.MODID);
    public static final KeyMapping CCTV_NV_KEY = new KeyMapping("key." + ApocalypseImmersion.MODID + ".cctv_nv", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories." + ApocalypseImmersion.MODID);

    @Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent public static void onRegisterKeys(RegisterKeyMappingsEvent e) { e.register(OPEN_CONFIG_KEY); e.register(CYCLE_CCTV_KEY); e.register(CCTV_NV_KEY); }
    }

    @Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_CONFIG_KEY.consumeClick()) { if (mc.screen == null) mc.setScreen(new ApocalypseConfigScreen(null)); }
            while (CYCLE_CCTV_KEY.consumeClick()) { if (mc.screen == null) SecurityCameraBridge.cycleNextCamera(); }
            while (CCTV_NV_KEY.consumeClick()) { if (mc.screen == null) SecurityCameraBridge.toggleNightVision(); }
        }
    }
}