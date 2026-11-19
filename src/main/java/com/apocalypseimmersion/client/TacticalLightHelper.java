package com.apocalypseimmersion.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

public class TacticalLightHelper {

    public static boolean isTorchOrLantern(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.is(Items.TORCH) || s.is(Items.SOUL_TORCH) || s.is(Items.LANTERN) || s.is(Items.SOUL_LANTERN)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null) return false;
        String p = id.getPath().toLowerCase();
        return p.contains("torch") || p.contains("lantern");
    }

    public static boolean isPrimaryWeapon(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem || s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof TridentItem) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null) return false;
        String ns = id.getNamespace().toLowerCase();
        String p = id.getPath().toLowerCase();
        return ns.contains("tacz") || ns.contains("pointblank") || p.contains("gun") || p.contains("rifle") || p.contains("pistol") || p.contains("shotgun") || p.contains("smg") || p.contains("sniper") || p.contains("sword") || p.contains("knife") || p.contains("machete");
    }

    public static boolean isOneHandedPistol(ItemStack s) {
        if (s.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        String p = id != null ? id.getPath().toLowerCase() : "";
        CompoundTag t = s.getTag();
        String nbt = t != null ? t.toString().toLowerCase() : "";
        return p.contains("pistol") || p.contains("revolver") || p.contains("glock") || p.contains("deagle") || p.contains("1911") || p.contains("beretta") || p.contains("handgun") || nbt.contains("pistol");
    }

    public static ItemStack findWaistLightItem(AbstractClientPlayer p) {
        if (p == null) return ItemStack.EMPTY;
        ItemStack curios = CuriosCompatHelper.findLightInBeltSlot(p);
        if (!curios.isEmpty()) return curios;
        for (ItemStack s : p.getInventory().items) {
            if (isTorchOrLantern(s) || isFlashlight(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isFlashlight(ItemStack s) {
        if (s.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null) return false;
        String ns = id.getNamespace().toLowerCase();
        String p = id.getPath().toLowerCase();
        if (ns.contains("flashlight") || p.contains("flashlight") || p.contains("headlamp") || p.contains("tactical_light") || p.contains("spotlight")) return true;
        CompoundTag tag = s.getTag();
        return tag != null && tag.toString().toLowerCase().contains("flashlight");
    }

    public static boolean isFlashlightPoweredOn(ItemStack s) {
        if (!isFlashlight(s)) return false;
        CompoundTag t = s.getTag();
        if (t == null) return true;
        if (t.contains("on")) return t.getBoolean("on");
        if (t.contains("isOn")) return t.getBoolean("isOn");
        if (t.contains("active")) return t.getBoolean("active");
        if (t.contains("lit")) return t.getBoolean("lit");
        if (t.contains("enabled")) return t.getBoolean("enabled");
        if (t.contains("mode")) return t.getInt("mode") > 0;
        return true;
    }

    public static boolean hasAnyActiveFlashlight(Player p) {
        if (p == null) return false;
        if (isFlashlightPoweredOn(p.getMainHandItem()) || isFlashlightPoweredOn(p.getOffhandItem())) return true;
        for (ItemStack armor : p.getArmorSlots()) {
            if (isFlashlightPoweredOn(armor)) return true;
        }
        ItemStack curios = CuriosCompatHelper.findLightInBeltSlot(p);
        if (isFlashlightPoweredOn(curios)) return true;
        CompoundTag pData = p.getPersistentData();
        return pData.getBoolean("shoulder_flashlight_on") || pData.getBoolean("flashlight_on");
    }
}