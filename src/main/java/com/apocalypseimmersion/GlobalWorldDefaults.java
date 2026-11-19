package com.apocalypseimmersion;

import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalWorldDefaults {

    private static boolean initialized = false;

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!initialized) {
            initialized = true;
            ApocalypseImmersionConfig.SPEC.save();
        }
    }
}