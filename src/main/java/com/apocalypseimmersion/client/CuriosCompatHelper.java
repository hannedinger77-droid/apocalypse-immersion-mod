package com.apocalypseimmersion.client;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.*;

public class CuriosCompatHelper {
    private static boolean failed = false;
    private static Method getInv, getMap, getStacks;

    public static boolean isCuriosActive() {
        return !failed && ApocalypseImmersionConfig.UNIVERSAL_MOD_COMPAT.get() && ApocalypseImmersionConfig.ENABLE_CURIOS_BELT.get() && ModList.get().isLoaded("curios");
    }

    private static boolean isLight(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.is(Items.TORCH) || s.is(Items.SOUL_TORCH) || s.is(Items.LANTERN) || s.is(Items.SOUL_LANTERN)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        return id != null && (id.getPath().contains("torch") || id.getPath().contains("lantern") || id.getPath().contains("flashlight") || id.getPath().contains("headlamp"));
    }

    public static ItemStack findLightInBeltSlot(Player p) {
        if (!isCuriosActive() || p == null) return ItemStack.EMPTY;
        try {
            if (getInv == null) getInv = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
            Object lazy = getInv.invoke(null, p);
            Optional<?> opt = (Optional<?>) lazy.getClass().getMethod("resolve").invoke(lazy);
            if (opt.isEmpty()) return ItemStack.EMPTY;
            Object handler = opt.get();
            if (getMap == null) getMap = handler.getClass().getMethod("getCurios");
            Map<?, ?> map = (Map<?, ?>) getMap.invoke(handler);
            for (String slot : new String[]{ "belt", "charm", "body", "head", "hands" }) {
                Object sh = map.get(slot); if (sh == null) continue;
                if (getStacks == null) getStacks = sh.getClass().getMethod("getStacks");
                IItemHandlerModifiable dyn = (IItemHandlerModifiable) getStacks.invoke(sh);
                for (int i = 0; i < dyn.getSlots(); i++) if (isLight(dyn.getStackInSlot(i))) return dyn.getStackInSlot(i);
            }
        } catch (Throwable t) { failed = true; }
        return ItemStack.EMPTY;
    }
}