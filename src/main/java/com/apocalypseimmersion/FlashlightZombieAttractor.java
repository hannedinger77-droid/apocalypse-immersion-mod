package com.apocalypseimmersion;

import com.apocalypseimmersion.client.TacticalLightHelper;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlashlightZombieAttractor {

    private static int tickCounter = 0;
    private static final Map<UUID, Boolean> WAS_AIRBORNE = new HashMap<>();

    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to) {
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null);
        return level.clip(ctx).getType() == HitResult.Type.MISS;
    }

    private static boolean isAttractingLightBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.GLOWSTONE)
                || state.is(Blocks.SEA_LANTERN) || state.is(Blocks.SHROOMLIGHT)) {
            return true;
        }
        return state.getLightEmission(level, pos) >= 11;
    }

    private static void alertNearbyEnemiesToSound(ServerLevel level, Vec3 soundPos, double openRange, double walledRange, double speed) {
        AABB box = new AABB(soundPos, soundPos).inflate(openRange);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m instanceof Enemy && m.getTarget() == null);
        for (Mob mob : mobs) {
            double dist = mob.position().distanceTo(soundPos);
            boolean clear = hasLineOfSight(level, mob.getEyePosition(), soundPos.add(0, 0.5, 0));
            double maxAllowed = clear ? openRange : walledRange;
            if (dist <= maxAllowed && dist > 1.5) {
                mob.getNavigation().moveTo(soundPos.x, soundPos.y, soundPos.z, speed);
            }
        }
    }

    // Hear weapon reloads, magazine clicks, bolt racking, and tactical gear from close range
    @SubscribeEvent
    public static void onServerSound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getEntity() instanceof Player player) || player.isCreative() || player.isSpectator()) return;
        if (!ApocalypseImmersionConfig.ENABLE_FLASHLIGHT_AGGRO.get()) return;

        String id = event.getSound().get().getLocation().toString().toLowerCase();
        boolean isReloadOrGear = id.contains("reload") || id.contains("mag") || id.contains("bolt")
                || id.contains("cock") || id.contains("pump") || id.contains("chamber") || id.contains("pin");
        if (isReloadOrGear) {
            alertNearbyEnemiesToSound(serverLevel, player.position(), 13.0, 6.5, 1.22D);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!ApocalypseImmersionConfig.ENABLE_FLASHLIGHT_AGGRO.get()) return;

        // 1. Jump-landing stomp detection (landing from a jump alerts nearby enemies)
        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable();
        boolean wasAir = WAS_AIRBORNE.getOrDefault(player.getUUID(), false);
        if (wasAir && player.onGround() && player.fallDistance > 0.35F) {
            double stompRange = Math.min(16.0, 10.0 + player.fallDistance * 2.0);
            alertNearbyEnemiesToSound(serverLevel, player.position(), stompRange, stompRange * 0.55, 1.25D);
        }
        WAS_AIRBORNE.put(player.getUUID(), airborne);

        // Scan every 10 ticks (0.5s)
        if (++tickCounter % 10 != 0) return;

        double maxSightRange = 42.0;
        Vec3 playerEye = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        boolean hasFlashlight = TacticalLightHelper.hasAnyActiveFlashlight(player);

        // 2. Close-proximity footstep hearing (crouching stays stealthy!)
        double horizSpeed = player.getDeltaMovement().horizontalDistance();
        if (!player.isCrouching() && horizSpeed > 0.04) {
            double stepRange = player.isSprinting() ? 12.0 : 6.5;
            alertNearbyEnemiesToSound(serverLevel, player.position(), stepRange, stepRange * 0.5, 1.15D);
        }

        // 3. Scan mobs around player for:
        //    (a) Focused Flashlight Beam hit (ONLY when flashlight is active AND beam points right onto them + LOS)
        //    (b) Any visible placed light source (torches, lanterns, campfires) the mob can actually SEE (even far away if clear LOS!)
        AABB scanBox = player.getBoundingBox().inflate(maxSightRange);
        List<Mob> nearbyMobs = serverLevel.getEntitiesOfClass(Mob.class, scanBox, mob ->
                mob.isAlive() && (mob instanceof Enemy) && mob.getTarget() == null);

        for (Mob mob : nearbyMobs) {
            Vec3 mobEye = mob.getEyePosition();

            // (a) Focused Flashlight Beam Cone ONLY (no 360-degree aggro!)
            if (hasFlashlight) {
                Vec3 toMob = mobEye.subtract(playerEye);
                double dist = toMob.length();
                if (dist <= maxSightRange && dist > 1.5) {
                    double alignment = lookVec.dot(toMob.normalize());
                    if (alignment >= 0.94 && hasLineOfSight(serverLevel, playerEye, mobEye)) {
                        mob.setTarget(player);
                        mob.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.25D);
                        continue;
                    }
                }
            }

            // (b) Any placed light source (torches, lanterns, campfires) the mob can SEE via unobstructed raycast
            BlockPos mPos = mob.blockPosition();
            BlockPos seenLight = null;
            double bestDistSq = Double.MAX_VALUE;
            int r = 24;
            for (BlockPos pos : BlockPos.betweenClosed(mPos.offset(-r, -6, -r), mPos.offset(r, 8, r))) {
                if (!isAttractingLightBlock(serverLevel, pos)) continue;
                Vec3 lightCenter = Vec3.atCenterOf(pos);
                double dSq = mobEye.distanceToSqr(lightCenter);
                if (dSq < bestDistSq && dSq > 4.0 && hasLineOfSight(serverLevel, mobEye, lightCenter)) {
                    bestDistSq = dSq;
                    seenLight = pos.immutable();
                }
            }

            if (seenLight != null) {
                Vec3 target = Vec3.atCenterOf(seenLight);
                mob.getNavigation().moveTo(target.x, target.y, target.z, 1.12D);
            }
        }
    }
}