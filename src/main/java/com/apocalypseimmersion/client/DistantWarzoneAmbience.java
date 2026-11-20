package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DistantWarzoneAmbience {

        private static final SoundEvent DISTANT_GUN_CRACK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "distant_gunshot_crack"));
    private record DelayedWarzoneCue(SoundEvent sound, Vec3 trueOrigin, int delayTicks, float baseVol, float pitch) {}
    private static final List<DelayedWarzoneCue> QUEUE = new CopyOnWriteArrayList<>();

    private static long lastNearGunfireMs = 0L;
    private static long lastDistantCueMs = 0L;

    private static void playDirectionalDistant(Minecraft mc, Vec3 trueOrigin, SoundEvent sound, float baseVol, float pitch) {
        if (mc.level == null || mc.player == null) return;
        // Suppress distant echo tail if close-range gunfire happened recently so near combat is never overlapped
        if (System.currentTimeMillis() - lastNearGunfireMs < 1500L) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 flatDelta = new Vec3(trueOrigin.x - eye.x, 0.0, trueOrigin.z - eye.z);
        double trueDist = Math.max(22.0, flatDelta.length());
        Vec3 dir = flatDelta.lengthSqr() > 0.001 ? flatDelta.normalize() : new Vec3(1, 0, 0);

        // Place 5.5 blocks out on eye plane in exact compass direction so Sound Physics Remastered never occludes it behind hills/walls
        Vec3 playPos = eye.add(dir.scale(5.5));
        float distAtten = (float) Mth.clamp(1.0 - ((trueDist - 22.0) / 185.0), 0.16, 0.92);
        float finalVol = Mth.clamp(baseVol * distAtten, 0.08f, 0.95f);

        mc.level.playLocalSound(playPos.x, playPos.y, playPos.z, sound, SoundSource.PLAYERS, finalVol, pitch, false);
    }

    @SubscribeEvent
    public static void onSoundPlayed(PlaySoundEvent event) {
        if (event.getSound() == null || !ApocalypseImmersionConfig.ENABLE_WARZONE_AMBIENCE.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        SoundSource srcType = event.getSound().getSource();
        if (srcType == SoundSource.AMBIENT || srcType == SoundSource.MUSIC || srcType == SoundSource.MASTER) return;

        String id = event.getSound().getLocation().toString().toLowerCase();
        boolean isGunshot = (id.contains("tacz") || id.contains("pointblank") || id.contains("gun")
                || id.contains("shoot") || id.contains("fire") || id.contains("shot")
                || id.contains("simpleenemy") || id.contains("bandit") || id.contains("pillager"));
        if (!isGunshot || id.contains("silence") || id.contains("suppress") || id.contains("reload") || id.contains("dry") || id.contains("mag") || id.contains("bolt") || id.contains("draw") || id.contains("equip") || id.contains("swap") || id.contains("inspect")) return;

        Vec3 src = new Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
        double dist = mc.player.position().distanceTo(src);
        long now = System.currentTimeMillis();

        // Track close-range gunfire (< 22 blocks) and do NOT add distant warzone layers on top of it
        if (dist < 22.0) {
            lastNearGunfireMs = now;
            QUEUE.clear();
            return;
        }

        // Only trigger for real NPC/enemy gunshots between 22 and 190 blocks away
        if (dist > 190.0) return;

        // Avoid overlapping cues during rapid automatic fire (min 180ms spacing)
        if (now - lastDistantCueMs < 180L) return;
        lastDistantCueMs = now;

        float freqScale = (float) Mth.clamp(ApocalypseImmersionConfig.WARZONE_FREQUENCY.get(), 0.25, 2.0);
        int travelDelay = Math.min(18, Math.max(2, (int) (dist / 18.0)));

        // Sharp distant ballistic crack + rolling urban/valley echo tail
        QUEUE.add(new DelayedWarzoneCue(DISTANT_GUN_CRACK, src, travelDelay, 0.72f * freqScale, 0.76f + mc.level.random.nextFloat() * 0.14f));
        QUEUE.add(new DelayedWarzoneCue(SoundEvents.GENERIC_EXPLODE, src, travelDelay + 4, 0.42f * freqScale, 0.50f + mc.level.random.nextFloat() * 0.08f));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        for (int i = QUEUE.size() - 1; i >= 0; i--) {
            DelayedWarzoneCue c = QUEUE.get(i);
            if (c.delayTicks <= 1) {
                playDirectionalDistant(mc, c.trueOrigin, c.sound, c.baseVol, c.pitch);
                QUEUE.remove(i);
            } else {
                QUEUE.set(i, new DelayedWarzoneCue(c.sound, c.trueOrigin, c.delayTicks - 1, c.baseVol, c.pitch));
            }
        }
    }
}