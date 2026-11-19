package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DistantWarzoneAmbience {
    private record ScheduledCue(SoundEvent sound, Vec3 worldOrigin, int delay, float baseVol, float pitch) {}
    private static final List<ScheduledCue> QUEUE = new CopyOnWriteArrayList<>();
    private static int nextSkirmishTicks = 140;

    private static boolean hasNpcOrEnemyMods() {
        ModList ml = ModList.get();
        return ml.isLoaded("simpleenemymod") || ml.isLoaded("simple_enemy_extended")
                || ml.isLoaded("chaoszproject") || ml.isLoaded("guardvillagers")
                || ml.isLoaded("villagerguardgunners") || ml.isLoaded("tacz");
    }

    private static void playDirectionalDistant(Minecraft mc, Vec3 trueOrigin, SoundEvent sound, float baseVol, float pitch) {
        if (mc.level == null || mc.player == null) return;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 delta = trueOrigin.subtract(eye);
        double trueDist = delta.length();
        Vec3 dir = trueDist > 0.01 ? delta.scale(1.0 / trueDist) : new Vec3(1, 0, 0);
        Vec3 playPos = eye.add(dir.scale(Math.min(13.5, Math.max(4.0, trueDist))));
        float finalVol = Mth.clamp(baseVol * (float) Mth.clamp(1.0 - (trueDist / 260.0), 0.14, 1.0), 0.06f, 1.0f);
        mc.level.playLocalSound(playPos.x, playPos.y, playPos.z, sound, SoundSource.AMBIENT, finalVol, pitch, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        for (int i = QUEUE.size() - 1; i >= 0; i--) {
            ScheduledCue c = QUEUE.get(i);
            if (c.delay <= 1) { playDirectionalDistant(mc, c.worldOrigin, c.sound, c.baseVol, c.pitch); QUEUE.remove(i); }
            else { QUEUE.set(i, new ScheduledCue(c.sound, c.worldOrigin, c.delay - 1, c.baseVol, c.pitch)); }
        }

        if (!ApocalypseImmersionConfig.ENABLE_WARZONE_AMBIENCE.get()) return;
        double freq = ApocalypseImmersionConfig.WARZONE_FREQUENCY.get();
        if (freq <= 0.05 || --nextSkirmishTicks > 0) return;

        RandomSource rng = mc.level.random;
        List<LivingEntity> nearbyNpcs = mc.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(mc.player.position(), mc.player.position()).inflate(140.0),
                e -> {
                    if (e == mc.player || !e.isAlive()) return false;
                    var key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
                    if (key == null) return false;
                    String id = key.toString().toLowerCase();
                    return id.contains("simpleenemy") || id.contains("simple_enemy") || id.contains("bandit")
                            || id.contains("soldier") || id.contains("raider") || id.contains("guard") || id.contains("enemy");
                });

        Vec3 origin;
        if (!nearbyNpcs.isEmpty()) {
            origin = nearbyNpcs.get(rng.nextInt(nearbyNpcs.size())).position().add((rng.nextDouble() - 0.5) * 14.0, 1.5, (rng.nextDouble() - 0.5) * 14.0);
            nextSkirmishTicks = (int) ((160 + rng.nextInt(260)) / freq);
        } else if (hasNpcOrEnemyMods()) {
            double ang = rng.nextDouble() * Math.PI * 2.0, dist = 75.0 + rng.nextDouble() * 110.0;
            origin = mc.player.position().add(Math.cos(ang) * dist, 4.0 + rng.nextDouble() * 10.0, Math.sin(ang) * dist);
            nextSkirmishTicks = (int) ((220 + rng.nextInt(380)) / freq);
        } else { nextSkirmishTicks = 200; return; }

        int mode = rng.nextInt(3);
        if (mode == 0) {
            for (int r = 0; r < 3 + rng.nextInt(3); r++) {
                float v = 0.58F + rng.nextFloat() * 0.18F;
                QUEUE.add(new ScheduledCue(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, origin, 1 + r * 3, v, 0.72F + rng.nextFloat() * 0.14F));
                QUEUE.add(new ScheduledCue(SoundEvents.GENERIC_EXPLODE, origin, 5 + r * 3, v * 0.55F, 0.50F));
            }
        } else if (mode == 1) {
            QUEUE.add(new ScheduledCue(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, origin, 1, 0.72F, 0.90F));
            QUEUE.add(new ScheduledCue(SoundEvents.GENERIC_EXPLODE, origin, 5, 0.45F, 0.56F));
        } else {
            QUEUE.add(new ScheduledCue(SoundEvents.GENERIC_EXPLODE, origin, 1, 0.68F, 0.44F));
            for (int r = 0; r < 4; r++) QUEUE.add(new ScheduledCue(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, origin.add(10, 0, -8), 12 + r * 3, 0.54F, 0.74F));
        }
    }
}