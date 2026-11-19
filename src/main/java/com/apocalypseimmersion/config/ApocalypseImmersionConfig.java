package com.apocalypseimmersion.config;
import net.minecraftforge.common.ForgeConfigSpec;

public class ApocalypseImmersionConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue FIX_LOST_CITIES_FACING, LOG_LOST_CITIES_FIXES, ENABLE_TORCH_HOLD_ANIM, ENABLE_WAIST_LIGHT, ENABLE_CURIOS_BELT, RESTRICT_FLASHLIGHT_PISTOLS, ENABLE_BREATH_FOG_GLASS, ENABLE_VOICE_PARANOIA, ENABLE_STAMINA_BREATHING, ENABLE_SC_SHADER_CAM, UNIVERSAL_MOD_COMPAT, INTEGRATED_SUPPRESSORS, ENABLE_DISTANT_GUN_ECHO, ENABLE_INDOOR_GUN_ECHO, ENABLE_NPC_VOICE_ECHO, ENABLE_WATER_BULLET_SPLASH, ENABLE_FLASHLIGHT_AGGRO, ENABLE_SOUND_RADAR_HUD, ENABLE_WARZONE_AMBIENCE, ENABLE_MUZZLE_LIGHT, ENABLE_SHOOTABLE_LIGHTS;
    public static final ForgeConfigSpec.DoubleValue WAIST_LIGHT_SIDE_OFFSET, WAIST_LIGHT_HEIGHT_OFFSET, BREATH_FOG_VISIBILITY, PARANOIA_INTENSITY, STAMINA_BREATH_VOLUME, DISTANT_GUN_RANGE, NPC_ECHO_INTENSITY, WARZONE_FREQUENCY;

    static {
        BUILDER.push("Compatibility");
        UNIVERSAL_MOD_COMPAT = BUILDER.define("enableUniversalModCompat", true);
        FIX_LOST_CITIES_FACING = BUILDER.define("fixLostCitiesDoomsdayFacing", true);
        LOG_LOST_CITIES_FIXES = BUILDER.define("logFixedLostCitiesBlocks", false);
        INTEGRATED_SUPPRESSORS = BUILDER.define("enableIntegratedSuppressorStealth", true);
        ENABLE_CURIOS_BELT = BUILDER.define("enableCuriosBeltSupport", true);
        BUILDER.pop();

        BUILDER.push("TacticalLight");
        ENABLE_TORCH_HOLD_ANIM = BUILDER.define("enableTorchHoldAnimation", true);
        ENABLE_WAIST_LIGHT = BUILDER.define("enableWaistLightAttachment", true);
        RESTRICT_FLASHLIGHT_PISTOLS = BUILDER.define("restrictFlashlightToPistols", true);
        ENABLE_FLASHLIGHT_AGGRO = BUILDER.define("enableFlashlightZombieAttraction", true);
        WAIST_LIGHT_SIDE_OFFSET = BUILDER.defineInRange("waistLightSideOffset", 0.35, -1.0, 1.0);
        WAIST_LIGHT_HEIGHT_OFFSET = BUILDER.defineInRange("waistLightHeightOffset", -0.15, -1.0, 1.0);
        BUILDER.pop();

        BUILDER.push("AtmosphereAndAudio");
        ENABLE_BREATH_FOG_GLASS = BUILDER.define("enableBreathFogGlass", true);
        BREATH_FOG_VISIBILITY = BUILDER.defineInRange("breathFogVisibility", 0.82, 0.10, 0.95);
        ENABLE_VOICE_PARANOIA = BUILDER.define("enableVoiceParanoia", true);
        PARANOIA_INTENSITY = BUILDER.defineInRange("paranoiaIntensity", 1.0, 0.1, 2.0);
        ENABLE_STAMINA_BREATHING = BUILDER.define("enableStaminaEmptyBreathing", true);
        STAMINA_BREATH_VOLUME = BUILDER.defineInRange("staminaBreathingVolume", 0.75, 0.1, 1.5);
        ENABLE_DISTANT_GUN_ECHO = BUILDER.define("enableDistantGunshotEcho", true);
        ENABLE_INDOOR_GUN_ECHO = BUILDER.define("enableIndoorGunshotReverb", true);
        DISTANT_GUN_RANGE = BUILDER.defineInRange("distantGunshotMaxRange", 260.0, 64.0, 512.0);
        ENABLE_NPC_VOICE_ECHO = BUILDER.define("enableNpcVoiceEcho", true);
        NPC_ECHO_INTENSITY = BUILDER.defineInRange("npcVoiceEchoIntensity", 0.85, 0.0, 2.0);
        ENABLE_WATER_BULLET_SPLASH = BUILDER.define("enableWaterBulletSplashes", true);
        ENABLE_SOUND_RADAR_HUD = BUILDER.define("enableBottomRightSoundRadar", true);
        ENABLE_WARZONE_AMBIENCE = BUILDER.define("enableDistantWarzoneAmbience", true);
        WARZONE_FREQUENCY = BUILDER.defineInRange("warzoneAmbienceFrequency", 1.0, 0.1, 3.0);
        ENABLE_MUZZLE_LIGHT = BUILDER.define("enableGunfireMuzzleLight", true);
        ENABLE_SHOOTABLE_LIGHTS = BUILDER.define("enableShootableLightsAndGlass", true);
        BUILDER.pop();

        BUILDER.push("SecurityCraftShaders");
        ENABLE_SC_SHADER_CAM = BUILDER.define("enableSecurityCraftShaderCam", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}