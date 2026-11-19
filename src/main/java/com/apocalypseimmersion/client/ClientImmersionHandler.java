package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientImmersionHandler {

    @Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) {
                    renderer.addLayer(new WaistLightRenderLayer(renderer));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        private static int cooldownTicks = 0;

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide) return;
            Player player = event.player;
            if (player != Minecraft.getInstance().player) return;

            if (cooldownTicks > 0) cooldownTicks--;

            if (ApocalypseImmersionConfig.RESTRICT_FLASHLIGHT_PISTOLS.get()) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (TacticalLightHelper.isFlashlight(offHand)
                        && TacticalLightHelper.isPrimaryWeapon(mainHand)
                        && !TacticalLightHelper.isOneHandedPistol(mainHand)) {

                    ItemStack moved = offHand.copy();
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    if (!player.getInventory().add(moved)) {
                        player.drop(moved, false);
                    }

                    if (cooldownTicks == 0) {
                        player.displayClientMessage(Component.literal("\u00A7e[Apocalypse Immersion] Handheld flashlights can only be used with pistols!"), true);
                        cooldownTicks = 40;
                    }
                }
            }
        }
    }
}