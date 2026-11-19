package com.apocalypseimmersion.client;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public class ModCompatBridge {
    public static boolean isLoaded(String modid) {
        return ApocalypseImmersionConfig.UNIVERSAL_MOD_COMPAT.get() && ModList.get().isLoaded(modid);
    }

    /** Reads normalized stamina (0.0 = empty, 1.0 = full) across ParCool, Feathers, and generic stamina NBT/caps */
    public static float getNormalizedStamina(Player player) {
        if (player == null) return 1.0F;
        CompoundTag data = player.getPersistentData();
        // Check ForgeCaps & persistent NBT keys used by ParCool, Feathers, and stamina mods
        float found = scanTagForStamina(data);
        if (found >= 0.0F) return found;

        CompoundTag fullTag = new CompoundTag();
        player.saveWithoutId(fullTag);
        if (fullTag.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
            float capVal = scanTagForStamina(fullTag.getCompound("ForgeCaps"));
            if (capVal >= 0.0F) return capVal;
        }
        // Fallback: if vanilla food/sprint exhaustion is maxed while sprinting/jumping
        return (player.getFoodData().getFoodLevel() <= 6) ? 0.0F : 1.0F;
    }

    private static float scanTagForStamina(CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            String k = key.toLowerCase();
            Tag sub = tag.get(key);
            if (sub instanceof CompoundTag child) {
                if (k.contains("parcool") || k.contains("stamina") || k.contains("feathers") || k.contains("endurance")) {
                    float cur = extractNumeric(child, "stamina", "current", "feathers", "value", "energy");
                    float max = extractNumeric(child, "maxstamina", "max", "maxfeathers", "capacity");
                    if (cur >= 0.0F) return max > 0.0F ? Math.min(1.0F, cur / max) : (cur <= 0.05F ? 0.0F : 1.0F);
                }
                float deep = scanTagForStamina(child);
                if (deep >= 0.0F) return deep;
            } else if ((k.contains("stamina") || k.contains("feathers")) && sub instanceof NumericTag num) {
                float val = num.getAsFloat();
                return val <= 1.0F ? Math.max(0.0F, val) : (val <= 2.0F ? 0.0F : 1.0F);
            }
        }
        return -1.0F;
    }

    private static float extractNumeric(CompoundTag tag, String... names) {
        for (String key : tag.getAllKeys()) {
            String low = key.toLowerCase();
            for (String n : names) {
                if (low.equals(n) || low.contains(n)) {
                    Tag t = tag.get(key);
                    if (t instanceof NumericTag num) return num.getAsFloat();
                }
            }
        }
        return -1.0F;
    }
}