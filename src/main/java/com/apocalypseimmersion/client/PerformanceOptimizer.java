package com.apocalypseimmersion.client;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.Minecraft;

public class PerformanceOptimizer {
    public static boolean isLowFps() {
        if (!ApocalypseImmersionConfig.AUTO_LOW_FPS_OPTIMIZER.get()) return false;
        int fps = Minecraft.getInstance().getFps();
        return fps > 0 && fps < 45;
    }
    public static int getEffectiveRadius(int baseRadius) {
        int r = Math.min(baseRadius, ApocalypseImmersionConfig.EFFECT_SCAN_RADIUS.get().intValue());
        return isLowFps() ? Math.max(4, r / 2) : r;
    }
    public static int getCacheIntervalTicks() {
        int base = Math.max(1, ApocalypseImmersionConfig.CACHE_UPDATE_TICKS.get().intValue());
        return isLowFps() ? base * 2 : base;
    }
    public static float getParticleMultiplier() {
        float base = ApocalypseImmersionConfig.PARTICLE_SCALE.get().floatValue();
        return isLowFps() ? base * 0.5f : base;
    }
}
