package com.apocalypseimmersion.client;

import com.apocalypseimmersion.ApocalypseImmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DistantGunshotEchoHandler {
    private record DelayedEcho(SoundEvent sound, Vec3 pos, int delayTicks, float volume, float pitch) {}
    private static final List<DelayedEcho> ECHO_QUEUE = new CopyOnWriteArrayList<>();

    private static boolean isIndoors(Level lvl, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 1.2, pos.z);
        if (!lvl.canSeeSky(bp)) return true;
        int walls = 0;
        for (int dx = -4; dx <= 4; dx += 4) for (int dz = -4; dz <= 4; dz += 4) {
            if ((dx != 0 || dz != 0) && lvl.getBlockState(bp.offset(dx, 1, dz)).isSolidRender(lvl, bp.offset(dx, 1, dz))) walls++;
        }
        return walls >= 4;
    }

    @SubscribeEvent
    public static void onSoundPlayed(PlaySoundEvent event) {
        SoundInstance orig = event.getSound();
        if (orig == null || orig instanceof DirectionalAcousticWrapper) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String id = orig.getLocation().toString().toLowerCase();
        SoundSource srcType = orig.getSource();
        Vec3 src = new Vec3(orig.getX(), orig.getY(), orig.getZ());
        double dist = mc.player.position().distanceTo(src);
        boolean indoors = isIndoors(mc.level, src);

        boolean isGun = (id.contains("tacz") || id.contains("pointblank") || id.contains("gun") || id.contains("simpleenemy"))
                && (id.contains("shoot") || id.contains("fire") || id.contains("shot"));
        if (isGun && !id.contains("silence") && !id.contains("suppress")) {
            if (dist > 6.0) {
                float factor = (float) Mth.clamp((dist - 6.0) / 140.0, 0.0, 1.0);
                event.setSound(new DirectionalAcousticWrapper(orig, Mth.lerp(factor, 1.0f, 0.24f), Mth.lerp(factor, 1.0f, 0.78f)));
            }
            if (indoors && ApocalypseImmersionConfig.ENABLE_INDOOR_GUN_ECHO.get() && dist <= 48.0) {
                float vol = (float) Math.max(0.15, 0.85 * (1.0 - dist / 52.0));
                ECHO_QUEUE.add(new DelayedEcho(SoundEvents.FIREWORK_ROCKET_BLAST, src, 1, vol * 0.75F, 1.45F));
            } else if (!indoors && ApocalypseImmersionConfig.ENABLE_DISTANT_GUN_ECHO.get()) {
                double maxR = ApocalypseImmersionConfig.DISTANT_GUN_RANGE.get();
                if (dist >= 22.0 && dist <= maxR) {
                    float atten = (float) Math.max(0.10, 1.0 - (dist / (maxR + 15.0)));
                    ECHO_QUEUE.add(new DelayedEcho(SoundEvents.GENERIC_EXPLODE, src, Math.min(20, Math.max(4, (int) (dist / 16.0))), atten * 0.65F, 0.54F));
                }
            }
            return;
        }

        boolean isFootstep = id.contains("step") || id.contains("footstep") || id.contains("walk") || id.contains("run");
        boolean isEnemyVoice = (srcType == SoundSource.HOSTILE || srcType == SoundSource.NEUTRAL || srcType == SoundSource.VOICE)
                && (id.contains("ambient") || id.contains("hurt") || id.contains("death") || id.contains("say") || id.contains("voice")
                || id.contains("simpleenemy") || id.contains("bandit") || id.contains("zombie"));

        if ((isFootstep || isEnemyVoice) && dist > 2.5 && dist <= 48.0) {
            float norm = (float) Mth.clamp((dist - 2.5) / (isFootstep ? 26.0f : 42.0f), 0.0, 1.0);
            event.setSound(new DirectionalAcousticWrapper(orig, Mth.lerp(norm, isFootstep ? 1.25f : 1.15f, 0.14f), Mth.lerp(norm, 1.0f, 0.86f)));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ECHO_QUEUE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (int i = ECHO_QUEUE.size() - 1; i >= 0; i--) {
            DelayedEcho echo = ECHO_QUEUE.get(i);
            if (echo.delayTicks <= 1) { mc.level.playLocalSound(echo.pos.x, echo.pos.y, echo.pos.z, echo.sound, SoundSource.AMBIENT, echo.volume, echo.pitch, false); ECHO_QUEUE.remove(i); }
            else { ECHO_QUEUE.set(i, new DelayedEcho(echo.sound, echo.pos, echo.delayTicks - 1, echo.volume, echo.pitch)); }
        }
    }

    private static class DirectionalAcousticWrapper implements SoundInstance {
        private final SoundInstance d; private final float v, p;
        DirectionalAcousticWrapper(SoundInstance d, float v, float p) { this.d = d; this.v = v; this.p = p; }
        @Override public ResourceLocation getLocation() { return d.getLocation(); }
        @Override public @Nullable WeighedSoundEvents resolve(SoundManager m) { return d.resolve(m); }
        @Override public Sound getSound() { return d.getSound(); }
        @Override public SoundSource getSource() { return d.getSource(); }
        @Override public boolean isLooping() { return d.isLooping(); }
        @Override public boolean isRelative() { return false; }
        @Override public int getDelay() { return d.getDelay(); }
        @Override public float getVolume() { return d.getVolume() * v; }
        @Override public float getPitch() { return d.getPitch() * p; }
        @Override public double getX() { return d.getX(); }
        @Override public double getY() { return d.getY(); }
        @Override public double getZ() { return d.getZ(); }
        @Override public Attenuation getAttenuation() { return Attenuation.LINEAR; }
    }
}