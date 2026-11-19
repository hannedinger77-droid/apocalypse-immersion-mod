package com.apocalypseimmersion;
import com.apocalypseimmersion.client.CuriosCompatHelper;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CuriosLightEmitter {
    private static final Map<UUID, BlockPos> LIGHTS = new ConcurrentHashMap<>();

    public static int getLightLevel(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (s.is(Items.LANTERN)) return 15;
        if (s.is(Items.TORCH)) return 14;
        if (s.is(Items.SOUL_LANTERN) || s.is(Items.SOUL_TORCH)) return 10;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null) return 0;
        String p = id.getPath().toLowerCase();
        if (p.contains("soul") && (p.contains("torch") || p.contains("lantern"))) return 10;
        if (p.contains("lantern")) return 15;
        return p.contains("torch") ? 14 : 0;
    }

    @SubscribeEvent public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        Player p = e.player; Level lvl = p.level();
        int light = (!ApocalypseImmersionConfig.ENABLE_CURIOS_BELT.get() || !ApocalypseImmersionConfig.ENABLE_WAIST_LIGHT.get() || p.isSpectator()) ? 0 : getLightLevel(CuriosCompatHelper.findLightInBeltSlot(p));
        if (light <= 0) { clear(lvl, p.getUUID()); return; }
        BlockPos pos = ok(lvl, p.blockPosition().above()) ? p.blockPosition().above() : p.blockPosition();
        BlockPos prev = LIGHTS.get(p.getUUID());
        if (prev != null && !prev.equals(pos)) del(lvl, prev);
        if (ok(lvl, pos)) {
            BlockState st = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light);
            if (!lvl.getBlockState(pos).equals(st)) lvl.setBlock(pos, st, 3);
            LIGHTS.put(p.getUUID(), pos.immutable());
        }
    }

    @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { clear(e.getEntity().level(), e.getEntity().getUUID()); }
    private static boolean ok(Level l, BlockPos p) { BlockState s = l.getBlockState(p); return s.isAir() || s.is(Blocks.LIGHT); }
    private static void clear(Level l, UUID u) { BlockPos p = LIGHTS.remove(u); if (p != null) del(l, p); }
    private static void del(Level l, BlockPos p) { if (l.getBlockState(p).is(Blocks.LIGHT)) l.setBlock(p, Blocks.AIR.defaultBlockState(), 3); }
}