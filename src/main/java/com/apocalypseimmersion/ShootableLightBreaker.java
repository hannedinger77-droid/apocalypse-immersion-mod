package com.apocalypseimmersion;

import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShootableLightBreaker {

    private static boolean isBullet(Entity e) {
        if (e instanceof Projectile) return true;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id != null && (id.getPath().contains("bullet") || id.getPath().contains("projectile"));
    }

    private static boolean isFragileLightOrGlass(BlockState state, ServerLevel level, BlockPos pos) {
        Block b = state.getBlock();
        if (b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b instanceof IronBarsBlock) return true;
        if (b instanceof TorchBlock || b instanceof LanternBlock || b instanceof RedstoneLampBlock || b instanceof CampfireBlock || b instanceof CandleBlock || b instanceof EndRodBlock) return true;
        if (state.hasProperty(BlockStateProperties.LIT)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
        String path = id != null ? id.getPath().toLowerCase() : "";
        if (path.contains("glass") || path.contains("window") || path.contains("lamp") || path.contains("lantern") || path.contains("light") || path.contains("bulb") || path.contains("neon") || path.contains("torch")) {
            return state.getDestroySpeed(level, pos) >= 0.0F && state.getDestroySpeed(level, pos) <= 4.0F;
        }
        return state.getLightEmission(level, pos) >= 6 && state.getDestroySpeed(level, pos) >= 0.0F && state.getDestroySpeed(level, pos) <= 3.0F;
    }

    private static void handleBlockHit(ServerLevel sl, BlockPos pos) {
        if (!ApocalypseImmersionConfig.ENABLE_SHOOTABLE_LIGHTS.get()) return;
        BlockState state = sl.getBlockState(pos);
        if (state.isAir() || !isFragileLightOrGlass(state, sl, pos)) return;

        Vec3 c = Vec3.atCenterOf(pos);
        // If the lamp has a LIT toggle and is currently ON, shoot the bulb out (turn LIT=false) first!
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            sl.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.95F, 1.45F);
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.55F, 1.35F);
            sl.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 14, 0.25, 0.25, 0.25, 0.15);
            sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 8, 0.15, 0.15, 0.15, 0.03);
            return;
        }

        // Otherwise shatter/break the light source or glass block completely
        sl.destroyBlock(pos, false);
        sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.15F);
        sl.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 12, 0.25, 0.25, 0.25, 0.12);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel sl)) return;
        if (event.getRayTraceResult() instanceof BlockHitResult bhr) {
            BlockPos p = bhr.getBlockPos().immutable(); sl.getServer().execute(() -> handleBlockHit(sl, p));
        }
    }

    @SubscribeEvent
    public static void onBulletSpawn(EntityJoinLevelEvent event) {
        if (!ApocalypseImmersionConfig.ENABLE_SHOOTABLE_LIGHTS.get() || !(event.getLevel() instanceof ServerLevel sl)) return;
        Entity e = event.getEntity();
        if (!isBullet(e)) return;

        Vec3 start = e.position();
        Vec3 vel = e.getDeltaMovement();
        if (vel.lengthSqr() == 0.0) return;
        Vec3 end = start.add(vel.normalize().scale(64.0));
        BlockHitResult hit = sl.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, e));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos p = hit.getBlockPos().immutable(); sl.getServer().execute(() -> handleBlockHit(sl, p));
        }
    }
}