package com.apocalypseimmersion;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaterBulletSplashHandler {
    private static boolean isBullet(Entity e) {
        if (e instanceof Projectile) return true;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id != null && (id.getPath().contains("bullet") || id.getPath().contains("projectile") || id.getPath().contains("shell"));
    }
    @SubscribeEvent public static void onImpact(ProjectileImpactEvent e) {
        if (!ApocalypseImmersionConfig.ENABLE_WATER_BULLET_SPLASH.get() || !(e.getProjectile().level() instanceof ServerLevel sl)) return;
        Vec3 hit = e.getRayTraceResult().getLocation(); BlockPos bp = BlockPos.containing(hit);
        if (sl.isWaterAt(bp) || sl.isWaterAt(bp.above()) || e.getProjectile().isInWater()) splash(sl, hit);
    }
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent e) {
        if (!ApocalypseImmersionConfig.ENABLE_WATER_BULLET_SPLASH.get() || !(e.getLevel() instanceof ServerLevel sl) || !isBullet(e.getEntity())) return;
        Vec3 st = e.getEntity().position(), vel = e.getEntity().getDeltaMovement();
        if (vel.lengthSqr() == 0.0) return;
        Vec3 dir = vel.normalize();
        for (int i = 1; i != 48; i++) {
            Vec3 pt = st.add(dir.scale(i * 0.75)); BlockPos bp = BlockPos.containing(pt);
            if (sl.isWaterAt(bp)) { splash(sl, pt); break; }
            if (!sl.isEmptyBlock(bp)) break;
        }
    }
    private static void splash(ServerLevel sl, Vec3 p) {
        sl.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.15, p.z, 26, 0.22, 0.45, 0.22, 0.18);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y + 0.05, p.z, 12, 0.25, 0.15, 0.25, 0.06);
        sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.25, p.z, 4, 0.12, 0.20, 0.12, 0.02);
        sl.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_SPLASH, SoundSource.AMBIENT, 0.85F, 1.35F + sl.random.nextFloat() * 0.25F);
    }
}