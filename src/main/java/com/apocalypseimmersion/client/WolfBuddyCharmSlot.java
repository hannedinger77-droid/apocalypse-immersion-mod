package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WolfBuddyCharmSlot {
    public static final ResourceLocation CHARM_TEX = ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "models/charm/wolf_buddy_charm.png");

    public static boolean isFirearm(ItemStack s) {
        if (s.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        String n = ((id != null ? id.toString() : "") + " " + s.getItem().getClass().getName() + " " + (s.getTag() != null ? s.getTag().toString() : "")).toLowerCase();
        return n.contains("tacz") || n.contains("ssr") || n.contains("tp_shooting") || n.contains("pointblank")
                || n.contains("mwc") || n.contains("cgm") || n.contains("gun") || n.contains("rifle")
                || n.contains("pistol") || n.contains("smg") || n.contains("sniper") || n.contains("shotgun")
                || n.contains("carbine") || n.contains("revolver") || n.contains("lmg") || n.contains("launcher");
    }

    public static boolean isCharmEquipped(ItemStack s) {
        if (s.isEmpty() || s.getTag() == null) return false;
        if (s.getTag().getBoolean("WolfBuddyCharm")) return true;
        String nbt = s.getTag().toString().toLowerCase();
        return nbt.contains("wolf_buddy_charm") || nbt.contains("wolf_buddy_pack");
    }

    public static void toggleCharm(Player p, ItemStack s) {
        if (!isFirearm(s)) return;
        boolean next = !s.getOrCreateTag().getBoolean("WolfBuddyCharm");
        s.getOrCreateTag().putBoolean("WolfBuddyCharm", next);
        if (next) GunBuddyVoiceManager.triggerGreet(p);
        else p.displayClientMessage(Component.literal("\u00A7d[\u00A7cWolf Afton\u00A7d] \u00A77Charm unequipped."), true);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String cls = event.getScreen().getClass().getName().toLowerCase();
        if (!(cls.contains("refit") || cls.contains("tacz") || cls.contains("mwloadout"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isFirearm(mc.player.getMainHandItem())) return;
        ItemStack gun = mc.player.getMainHandItem();
        event.addListener(Button.builder(Component.literal(isCharmEquipped(gun) ? "\u00A7d\u2605" : "\u00A77\u2606"), b -> {
            toggleCharm(mc.player, gun);
            b.setMessage(Component.literal(isCharmEquipped(gun) ? "\u00A7d\u2605" : "\u00A77\u2606"));
        }).bounds(event.getScreen().width - 216, 12, 18, 18).build());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        String cls = event.getScreen().getClass().getName().toLowerCase();
        if (!(cls.contains("refit") || cls.contains("tacz") || cls.contains("mwloadout"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isFirearm(mc.player.getMainHandItem())) return;
        GuiGraphics g = event.getGuiGraphics();
        int bx = event.getScreen().width - 216, by = 12;
        g.fill(bx - 1, by - 1, bx + 19, by + 19, isCharmEquipped(mc.player.getMainHandItem()) ? 0xFFFF1038 : 0xFF6A1B9A);
        g.blit(CHARM_TEX, bx + 1, by + 1, 16, 16, 0, 0, 128, 128, 256, 256);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_B) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null) toggleCharm(mc.player, mc.player.getMainHandItem());
        }
    }
}
