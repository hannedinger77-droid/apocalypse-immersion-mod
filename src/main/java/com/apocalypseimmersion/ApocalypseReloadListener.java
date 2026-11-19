package com.apocalypseimmersion;

import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ApocalypseReloadListener implements ResourceManagerReloadListener {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ApocalypseReloadListener());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ApocalypseImmersionConfig.SPEC.save();
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(
                Component.literal("\u00A7a[Apocalypse Immersion] \u00A7fSettings & acoustic reverb reloaded!"), false
            );
        }
    }
}