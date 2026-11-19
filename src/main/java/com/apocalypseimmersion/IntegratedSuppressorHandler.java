package com.apocalypseimmersion;

import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IntegratedSuppressorHandler {

    private static final Map<UUID, Long> SILENT_UNTIL = new ConcurrentHashMap<>();
    private static final String[] BUILTIN_KEYS = {
        "vss", "as_val", "asval", "mp5sd", "honey_badger", "honeybadger", "spr15hb",
        "dvl", "welrod", "delisle", "sten_mk2s", "suppress", "silenc", "integral", "subsonic"
    };

    public static boolean isIntegrallySuppressed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        StringBuilder sb = new StringBuilder();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) sb.append(id.toString()).append(' ');
        sb.append(stack.getHoverName().getString()).append(' ');
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("GunId")) sb.append(tag.getString("GunId"));

        String check = sb.toString().toLowerCase(Locale.ROOT);
        for (String key : BUILTIN_KEYS) {
            if (check.contains(key)) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSoundAtPos(PlayLevelSoundEvent.AtPosition event) {
        if (!ApocalypseImmersionConfig.INTEGRATED_SUPPRESSORS.get() || event.getSound() == null) return;
        String sPath = event.getSound().value().getLocation().toString().toLowerCase(Locale.ROOT);
        Vec3 pos = event.getPosition();
        Player shooter = event.getLevel().getNearestPlayer(pos.x, pos.y, pos.z, 3.0D, false);

        if (shooter != null && isIntegrallySuppressed(shooter.getMainHandItem())) {
            if (sPath.contains("tacz") || sPath.contains("pointblank") || sPath.contains("gun") || sPath.contains("shoot")) {
                SILENT_UNTIL.put(shooter.getUUID(), event.getLevel().getGameTime() + 45L);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!ApocalypseImmersionConfig.INTEGRATED_SUPPRESSORS.get() || event.getSound() == null) return;
        String sPath = event.getSound().value().getLocation().toString().toLowerCase(Locale.ROOT);
        if (event.getEntity() instanceof Player p && isIntegrallySuppressed(p.getMainHandItem())) {
            if (sPath.contains("tacz") || sPath.contains("pointblank") || sPath.contains("gun") || sPath.contains("shoot")) {
                SILENT_UNTIL.put(p.getUUID(), event.getLevel().getGameTime() + 45L);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!ApocalypseImmersionConfig.INTEGRATED_SUPPRESSORS.get()) return;
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || !(event.getNewTarget() instanceof Player p)) return;

        Long until = SILENT_UNTIL.get(p.getUUID());
        if (until != null && mob.level().getGameTime() <= until) {
            if (mob.distanceToSqr(p) > 36.0D || !mob.hasLineOfSight(p)) {
                event.setCanceled(true);
            }
        }
    }
}