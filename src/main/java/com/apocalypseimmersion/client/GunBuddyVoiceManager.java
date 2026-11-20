package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunBuddyVoiceManager {
    private static SoundEvent sfx(String n) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", n)); }
    private static final SoundEvent GREET = sfx("buddy_greet"), PIG_CREATIVE = sfx("buddy_pig_creative"),
            SHEEP_THREAT = sfx("buddy_sheep_threat"), KILL_ENEMY = sfx("buddy_kill_enemy"),
            CREEPER = sfx("buddy_creeper"), NO_AMMO = sfx("buddy_no_ammo"),
            WHISTLE = sfx("buddy_whistle"), SHANE = sfx("buddy_shane"),
            CELINE = sfx("buddy_celine"), CHRISTMAS = sfx("buddy_christmas");

    private static String lastArmedKey = "";
    private static int cd = 0, creeperCd = 0, emptyCd = 0, sheepCd = 0, chatTimer = 420;

    public static void triggerGreet(Player p) { speak(p, GREET, "Hi, I'm Wolf Afton! Hope you're better than my last operator.", true); }
    public static boolean hasWolfBuddyCharm(ItemStack s) { if (WolfBuddyCharmSlot.isCharmEquipped(s)) return true;
        if (s.isEmpty()) return false;
        CompoundTag tag = s.getTag();
        if (tag == null) return false;
        String nbt = tag.toString().toLowerCase();
        return nbt.contains("wolf_buddy_charm") || nbt.contains("wolf_buddy_pack");
    }

    private static void speak(Player p, SoundEvent sound, String sub, boolean force) {
        if (p == null || (!force && cd > 0)) return;
        cd = 110;
        p.level().playLocalSound(p.getX(), p.getY() + 1.4, p.getZ(), sound, SoundSource.PLAYERS, 1.0f, 1.0f, false);
        p.displayClientMessage(Component.literal("\u00A7d[\u00A7cWolf Afton\u00A7d] \u00A7f" + sub), true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || !p.level().isClientSide()) return;
        if (!hasWolfBuddyCharm(p.getMainHandItem())) return;

        if (event.getEntity() instanceof Pig && p.isCreative()) {
            speak(p, PIG_CREATIVE, "Hey! Why'd you kill that pig?! You're in creative mode!", true);
        } else if (event.getEntity() instanceof Enemy && p.level().random.nextFloat() < 0.35f) {
            speak(p, KILL_ENEMY, "Hey, what did he do to you? He just wanted a hug...", false);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || !p.level().isClientSide()) return;
        if (event.getEntity() instanceof Sheep && sheepCd <= 0 && hasWolfBuddyCharm(p.getMainHandItem())) {
            sheepCd = 240;
            speak(p, SHEEP_THREAT, "Don't kill that sheep! Cuz if you do, I'll come for you...", true);
        }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getSound() == null) return;
        String id = event.getSound().getLocation().toString().toLowerCase();
        if (id.contains("dry") && hasWolfBuddyCharm(mc.player.getMainHandItem()) && emptyCd <= 0) {
            emptyCd = 160;
            speak(mc.player, NO_AMMO, "Oh no! We're out of ammo! Oh wait, you can just reload.", true);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance(); Player p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        if (cd > 0) cd--; if (creeperCd > 0) creeperCd--; if (emptyCd > 0) emptyCd--; if (sheepCd > 0) sheepCd--;

        ItemStack main = p.getMainHandItem();
        if (!hasWolfBuddyCharm(main)) { lastArmedKey = ""; return; }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(main.getItem());
        String key = (itemId != null ? itemId.toString() : "gun") + ":" + (main.getTag() != null ? main.getTag().getString("GunId") : "");
        if (!key.equals(lastArmedKey)) {
            lastArmedKey = key;
            speak(p, GREET, "Hi, I'm Wolf Afton! Hope you're better than my last operator.", true);
            return;
        }

        if (creeperCd <= 0 && !mc.level.getEntitiesOfClass(Creeper.class, p.getBoundingBox().inflate(14.0)).isEmpty()) {
            creeperCd = 360;
            speak(p, CREEPER, "Hey, you might wanna watch out for that creeper over there...", true);
            return;
        }

        CompoundTag tag = main.getTag();
        if (tag != null && tag.contains("GunCurrentAmmoCount") && tag.getInt("GunCurrentAmmoCount") == 0 && emptyCd <= 0) {
            emptyCd = 180;
            speak(p, NO_AMMO, "Oh no! We're out of ammo! Oh wait, you can just reload.", true);
            return;
        }

        if (--chatTimer <= 0) {
            RandomSource r = mc.level.random;
            chatTimer = 520 + r.nextInt(480);
            int pick = r.nextInt(4);
            if (pick == 0) speak(p, WHISTLE, "*Whistling*", false);
            else if (pick == 1) speak(p, SHANE, "Me and Skelly are such good friends...", false);
            else if (pick == 2) speak(p, CELINE, "Of course I don't think about Celine...", false);
            else speak(p, CHRISTMAS, "One Christmas, me and Shane were hanging out...", false);
        }
    }
}
