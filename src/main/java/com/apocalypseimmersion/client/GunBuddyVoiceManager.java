package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunBuddyVoiceManager {
    private static SoundEvent sfx(String name) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", name)); }
    private static final SoundEvent GREET = sfx("buddy_greet"), CREEPER = sfx("buddy_creeper"), LOW = sfx("buddy_low_ammo"),
            EMPTY = sfx("buddy_no_ammo"), KILL = sfx("buddy_kill"), C1 = sfx("buddy_chat1"), C2 = sfx("buddy_chat2"), C3 = sfx("buddy_chat3");

    private static final Set<String> EQUIPPED_GUNS = new HashSet<>();
    private static final Path SAVE_FILE = FMLPaths.CONFIGDIR.get().resolve("apocalypseimmersion-charms.txt");
    private static boolean loaded = false;
    private static String lastGun = "";
    private static int cd = 0, creeperCd = 0, ammoCd = 0, chatTimer = 420;
    public static int talkGlowTicks = 0;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try { if (Files.exists(SAVE_FILE)) EQUIPPED_GUNS.addAll(Files.readAllLines(SAVE_FILE)); } catch (Exception ignored) {}
    }

    private static void save() {
        try { Files.write(SAVE_FILE, EQUIPPED_GUNS); } catch (Exception ignored) {}
    }

    public static boolean isFirearm(ItemStack s) {
        if (s.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        String n = (id != null ? id.toString() : "") + (s.getTag() != null ? s.getTag().toString() : "");
        n = n.toLowerCase();
        return n.contains("tacz") || n.contains("ssr") || n.contains("tp_shooting") || n.contains("gunid");
    }

    public static String gunKey(ItemStack s) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        String gid = (s.getTag() != null && s.getTag().contains("GunId")) ? s.getTag().getString("GunId") : "";
        return (id != null ? id.toString() : "gun") + ":" + gid;
    }

    public static boolean hasCharm(ItemStack s) {
        ensureLoaded();
        return isFirearm(s) && EQUIPPED_GUNS.contains(gunKey(s));
    }

    public static void toggleCharm(Player p, ItemStack s) {
        if (!isFirearm(s)) return;
        ensureLoaded();
        String k = gunKey(s);
        if (EQUIPPED_GUNS.remove(k)) {
            p.displayClientMessage(Component.literal("§d[§cWolf Buddy§d] §7Charm removed."), true);
        } else {
            EQUIPPED_GUNS.add(k);
            speak(p, GREET, "Hello new user! Sure hope you're better than my last operator.", true);
        }
        save();
    }

    public static void speak(Player p, SoundEvent sound, String sub, boolean force) {
        if (p == null || (!force && cd > 0)) return;
        cd = 85; talkGlowTicks = 32;
        p.level().playLocalSound(p.getX(), p.getY() + 1.4, p.getZ(), sound, SoundSource.PLAYERS, 0.95f, 1.0f, false);
        p.displayClientMessage(Component.literal("§d[§cWolf Buddy§d] §f" + sub), true);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String cls = event.getScreen().getClass().getName().toLowerCase();
        if (!cls.contains("mwloadout") || !(cls.contains("custom") || cls.contains("loadout") || cls.contains("attachment"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isFirearm(mc.player.getMainHandItem())) return;
        ItemStack gun = mc.player.getMainHandItem();
        event.addListener(Button.builder(Component.literal(hasCharm(gun) ? "§dWolf Charm: §cON" : "§dWolf Charm: §7OFF"), btn -> {
            toggleCharm(mc.player, gun);
            btn.setMessage(Component.literal(hasCharm(gun) ? "§dWolf Charm: §cON" : "§dWolf Charm: §7OFF"));
        }).bounds(event.getScreen().width - 134, 8, 126, 20).build());
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_B) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null) toggleCharm(mc.player, mc.player.getMainHandItem());
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player p && p.level().isClientSide() && hasCharm(p.getMainHandItem()) && p.level().random.nextFloat() < 0.35f) {
            speak(p, KILL, "Hey! What did he do to you?", false);
        }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !hasCharm(mc.player.getMainHandItem()) || event.getSound() == null) return;
        String id = event.getSound().getLocation().toString().toLowerCase();
        if (id.contains("dry")) speak(mc.player, EMPTY, "Click! Out of ammo, reload!", true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance(); Player p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        if (cd > 0) cd--; if (creeperCd > 0) creeperCd--; if (ammoCd > 0) ammoCd--; if (talkGlowTicks > 0) talkGlowTicks--;

        ItemStack main = p.getMainHandItem();
        if (!hasCharm(main)) { lastGun = ""; return; }
        String gk = gunKey(main);
        if (!gk.equals(lastGun)) { lastGun = gk; speak(p, GREET, "Hello new user! Sure hope you're better than my last operator.", true); }

        if (creeperCd <= 0 && !mc.level.getEntitiesOfClass(Creeper.class, p.getBoundingBox().inflate(14.0)).isEmpty()) {
            creeperCd = 360; speak(p, CREEPER, "Heads up! Creeper close by!", true); return;
        }
        CompoundTag tag = main.getTag();
        if (tag != null && tag.contains("GunCurrentAmmoCount")) {
            int ammo = tag.getInt("GunCurrentAmmoCount");
            if (ammo == 0 && ammoCd <= 0) { ammoCd = 140; speak(p, EMPTY, "Out of ammo! Reload!", true); return; }
            if (ammo > 0 && ammo <= 5 && ammoCd <= 0) { ammoCd = 220; speak(p, LOW, "Watch your mag, running low on ammo.", false); return; }
        }
        if (--chatTimer <= 0) {
            RandomSource r = mc.level.random; chatTimer = 460 + r.nextInt(420);
            if (p.getHealth() <= p.getMaxHealth() * 0.45f) speak(p, C3, "You don't sound so good, patch up!", false);
            else speak(p, r.nextBoolean() ? C1 : C2, r.nextBoolean() ? "Eyes on the enemy!" : "I love blowing stuff up!", false);
        }
    }
}