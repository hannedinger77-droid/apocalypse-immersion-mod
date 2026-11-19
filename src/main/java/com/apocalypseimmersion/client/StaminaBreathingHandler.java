package com.apocalypseimmersion.client;
import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StaminaBreathingHandler {
    private static int breathCooldown = 0;
    private static boolean wasExhausted = false;

    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !ApocalypseImmersionConfig.ENABLE_STAMINA_BREATHING.get()) return;
        Minecraft mc = Minecraft.getInstance(); Player p = mc.player;
        if (mc.level == null || p == null || mc.isPaused() || p.isCreative() || p.isSpectator()) return;

        if (breathCooldown > 0) breathCooldown--;
        float stamina = ModCompatBridge.getNormalizedStamina(p);
        boolean exhausted = stamina <= 0.12F;

        if (exhausted && breathCooldown == 0) {
            float vol = ApocalypseImmersionConfig.STAMINA_BREATH_VOLUME.get().floatValue();
            // Double-beat inhale/exhale panting cue
            mc.level.playLocalSound(p.getX(), p.getEyeY(), p.getZ(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, vol, 0.78F + mc.level.random.nextFloat() * 0.10F, false);
            breathCooldown = wasExhausted ? 26 : 18;
        }
        wasExhausted = exhausted;
    }
}