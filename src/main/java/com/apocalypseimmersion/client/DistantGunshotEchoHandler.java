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

        private static final SoundEvent INDOOR_GUN_TAIL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "indoor_gun_tail"));
    private static final SoundEvent DISTANT_GUN_CRACK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("apocalypseimmersion", "distant_gunshot_crack"));
    private record DelayedEcho(SoundEvent sound, Vec3 pos, int delayTicks, float volume, float pitch) {}
    private static final List<DelayedEcho> ECHO_QUEUE = new CopyOnWriteArrayList<>();

    private static boolean isIndoorsOrEnclosed(Level lvl, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 1.2, pos.z);
        if (!lvl.canSeeSky(bp)) return true;
        int walls = 0;
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                if (dx == 0 && dz == 0) continue;
                BlockPos check = bp.offset(dx, 1, dz);
                if (lvl.getBlockState(check).isSolidRender(lvl, check)) walls++;
            }
        }
        return walls >= 3;
    }

    @SubscribeEvent
    public static void onSoundPlayed(PlaySoundEvent event) {
        SoundInstance orig = event.getSound();
        if (orig == null || orig instanceof DirectionalAcousticWrapper) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ResourceLocation loc = orig.getLocation();
        String ns = loc.getNamespace().toLowerCase();
        String fullId = loc.toString().toLowerCase();

        // Check resolved inner sound path as well (TaCZ resolves draw/reload/shoot inside GunSoundInstance)
        String resolvedPath = "";
        try {
            if (orig.getSound() != null && orig.getSound().getLocation() != null) {
                resolvedPath = orig.getSound().getLocation().toString().toLowerCase();
            }
        } catch (Exception ignored) {}
        String combined = fullId + " " + resolvedPath;

        // Strictly ignore ALL reload, magazine, bolt, draw, holster, swap & handling sounds (zero delay, zero reverb!)
        if (combined.contains("reload") || combined.contains("mag") || combined.contains("bolt")
                || combined.contains("charge") || combined.contains("cock") || combined.contains("pump")
                || combined.contains("draw") || combined.contains("equip") || combined.contains("swap")
                || combined.contains("holster") || combined.contains("inspect") || combined.contains("switch")
                || combined.contains("fire_select") || combined.contains("chamber") || combined.contains("shell")
                || combined.contains("put_away") || combined.contains("dry") || combined.contains("silence") || combined.contains("suppress")) {
            return;
        }

        // Only match actual gunfire (must contain shoot/fire/shot in resolved or outer path, OR high-volume TaCZ shot >= 1.5f)
        boolean isActualGunshot = (combined.contains("shoot") || combined.contains("fire") || combined.contains("shot"))
                && (ns.contains("tacz") || ns.contains("pointblank") || ns.contains("simpleenemy") || combined.contains("gun"));
        if (ns.equals("tacz") && resolvedPath.isEmpty()) {
            isActualGunshot = orig.getVolume() >= 1.55f;
        }

        Vec3 src = new Vec3(orig.getX(), orig.getY(), orig.getZ());
        double dist = mc.player.position().distanceTo(src);

        if (isActualGunshot) {
            if (dist > 8.0) {
                float factor = (float) Mth.clamp((dist - 8.0) / 140.0, 0.0, 1.0);
                event.setSound(new DirectionalAcousticWrapper(orig, Mth.lerp(factor, 1.0f, 0.25f), Mth.lerp(factor, 1.0f, 0.80f)));
            }
            boolean enclosed = isIndoorsOrEnclosed(mc.level, src);
            if (enclosed && ApocalypseImmersionConfig.ENABLE_INDOOR_GUN_ECHO.get() && dist <= 48.0) {
                float vol = (float) Math.max(0.14, 0.42 * (1.0 - dist / 52.0));
                // Crisp high-frequency room slapback ONLY (never GENERIC_EXPLODE!)
                ECHO_QUEUE.add(new DelayedEcho(INDOOR_GUN_TAIL, mc.player.getEyePosition(), 1, vol * 1.15F, 0.96F + mc.level.random.nextFloat() * 0.08F));
            } else if (!enclosed && ApocalypseImmersionConfig.ENABLE_DISTANT_GUN_ECHO.get() && dist >= 28.0) {
                double maxR = ApocalypseImmersionConfig.DISTANT_GUN_RANGE.get();
                if (dist <= maxR) {
                    float atten = (float) Math.max(0.08, 0.45 * (1.0 - (dist / (maxR + 15.0))));
                    int delay = Math.min(18, Math.max(4, (int) (dist / 16.0)));
                    ECHO_QUEUE.add(new DelayedEcho(DISTANT_GUN_CRACK, src, delay, atten * 1.25F, 0.94F + mc.level.random.nextFloat() * 0.12F));
                }
            }
            return;
        }

        // Dependable directional enemy footsteps & voices (never touches player reload/gun handling!)
        SoundSource srcType = orig.getSource();
        boolean isFootstep = combined.contains("step") || combined.contains("footstep") || combined.contains("walk") || combined.contains("run");
        boolean isEnemyVoice = (srcType == SoundSource.HOSTILE || srcType == SoundSource.NEUTRAL || srcType == SoundSource.VOICE)
                && (combined.contains("ambient") || combined.contains("hurt") || combined.contains("death") || combined.contains("say") || combined.contains("voice")
                || combined.contains("simpleenemy") || combined.contains("bandit") || combined.contains("zombie"));

        if ((isFootstep || isEnemyVoice) && dist > 3.0 && dist <= 48.0) {
            float norm = (float) Mth.clamp((dist - 3.0) / (isFootstep ? 26.0f : 42.0f), 0.0, 1.0);
            event.setSound(new DirectionalAcousticWrapper(orig, Mth.lerp(norm, isFootstep ? 1.22f : 1.12f, 0.14f), Mth.lerp(norm, 1.0f, 0.88f)));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ECHO_QUEUE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (int i = ECHO_QUEUE.size() - 1; i >= 0; i--) {
            DelayedEcho echo = ECHO_QUEUE.get(i);
            if (echo.delayTicks <= 1) {
                mc.level.playLocalSound(echo.pos.x, echo.pos.y, echo.pos.z, echo.sound, SoundSource.PLAYERS, echo.volume, echo.pitch, false);
                ECHO_QUEUE.remove(i);
            } else {
                ECHO_QUEUE.set(i, new DelayedEcho(echo.sound, echo.pos, echo.delayTicks - 1, echo.volume, echo.pitch));
            }
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