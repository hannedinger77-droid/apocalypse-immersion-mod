package com.apocalypseimmersion.client;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Locale;

public class ApocalypseConfigScreen extends Screen {
    private final Screen parent;
    public ApocalypseConfigScreen(Screen parent) { super(Component.literal("Apocalypse Immersion Settings")); this.parent = parent; }

    @Override protected void init() {
        int l = this.width / 2 - 155, r = this.width / 2 + 5, y = 12, h = 16;
        addRenderableWidget(tog(l, y, 150, "Universal Compat", ApocalypseImmersionConfig.UNIVERSAL_MOD_COMPAT));
        addRenderableWidget(tog(r, y, 150, "Curios Belt Slot", ApocalypseImmersionConfig.ENABLE_CURIOS_BELT));
        addRenderableWidget(tog(l, y + h, 150, "Lost Cities Fix", ApocalypseImmersionConfig.FIX_LOST_CITIES_FACING));
        addRenderableWidget(tog(r, y + h, 150, "Built-In Silencers", ApocalypseImmersionConfig.INTEGRATED_SUPPRESSORS));
        addRenderableWidget(tog(l, y + h * 2, 150, "Torch Hold Anim", ApocalypseImmersionConfig.ENABLE_TORCH_HOLD_ANIM));
        addRenderableWidget(tog(r, y + h * 2, 150, "Waist Light Attach", ApocalypseImmersionConfig.ENABLE_WAIST_LIGHT));
        addRenderableWidget(tog(l, y + h * 3, 150, "Pistol Flashlight", ApocalypseImmersionConfig.RESTRICT_FLASHLIGHT_PISTOLS));
        addRenderableWidget(tog(r, y + h * 3, 150, "Flashlight Aggro", ApocalypseImmersionConfig.ENABLE_FLASHLIGHT_AGGRO));
        addRenderableWidget(tog(l, y + h * 4, 150, "Breath Fog Glass", ApocalypseImmersionConfig.ENABLE_BREATH_FOG_GLASS));
        addRenderableWidget(tog(r, y + h * 4, 150, "Voice Paranoia", ApocalypseImmersionConfig.ENABLE_VOICE_PARANOIA));
        addRenderableWidget(tog(l, y + h * 5, 150, "Stamina Breathing", ApocalypseImmersionConfig.ENABLE_STAMINA_BREATHING));
        addRenderableWidget(tog(r, y + h * 5, 150, "SC Shader CCTV", ApocalypseImmersionConfig.ENABLE_SC_SHADER_CAM));
        addRenderableWidget(tog(l, y + h * 6, 150, "Outdoor Gun Echo", ApocalypseImmersionConfig.ENABLE_DISTANT_GUN_ECHO));
        addRenderableWidget(tog(r, y + h * 6, 150, "Indoor Gun Reverb", ApocalypseImmersionConfig.ENABLE_INDOOR_GUN_ECHO));
        addRenderableWidget(tog(l, y + h * 7, 150, "NPC Voice Echo", ApocalypseImmersionConfig.ENABLE_NPC_VOICE_ECHO));
        addRenderableWidget(tog(r, y + h * 7, 150, "Water Bullet Splash", ApocalypseImmersionConfig.ENABLE_WATER_BULLET_SPLASH));
        addRenderableWidget(tog(l, y + h * 8, 150, "Acoustic Radar HUD", ApocalypseImmersionConfig.ENABLE_SOUND_RADAR_HUD));
        addRenderableWidget(tog(r, y + h * 8, 150, "Warzone Ambience", ApocalypseImmersionConfig.ENABLE_WARZONE_AMBIENCE));

        addRenderableWidget(new Sld(l, y + h * 9, 150, 14, "Distant Gun Range", ApocalypseImmersionConfig.DISTANT_GUN_RANGE, 64.0, 512.0));
        addRenderableWidget(new Sld(r, y + h * 9, 150, 14, "NPC Echo Strength", ApocalypseImmersionConfig.NPC_ECHO_INTENSITY, 0.0, 2.0));
        addRenderableWidget(new Sld(l, y + h * 10, 150, 14, "Glass Fog Alpha", ApocalypseImmersionConfig.BREATH_FOG_VISIBILITY, 0.1, 0.95));
        addRenderableWidget(new Sld(r, y + h * 10, 150, 14, "Warzone Freq", ApocalypseImmersionConfig.WARZONE_FREQUENCY, 0.1, 3.0));
        addRenderableWidget(new Sld(l, y + h * 11, 150, 14, "Waist Side", ApocalypseImmersionConfig.WAIST_LIGHT_SIDE_OFFSET, -1.0, 1.0));
        addRenderableWidget(new Sld(r, y + h * 11, 150, 14, "Waist Height", ApocalypseImmersionConfig.WAIST_LIGHT_HEIGHT_OFFSET, -1.0, 1.0));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(this.width / 2 - 75, this.height - 18, 150, 15).build());
    }
    private Button tog(int x, int y, int w, String lbl, ForgeConfigSpec.BooleanValue v) {
        return Button.builder(Component.literal(lbl + ": " + (v.get() ? "\u00A7aON" : "\u00A7cOFF")), b -> {
            v.set(!v.get()); ApocalypseImmersionConfig.SPEC.save();
            b.setMessage(Component.literal(lbl + ": " + (v.get() ? "\u00A7aON" : "\u00A7cOFF")));
        }).bounds(x, y, w, 15).build();
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) { renderBackground(g); g.drawCenteredString(font, title, width / 2, 3, 0xFFFFFF); super.render(g, mx, my, pt); }
    @Override public void onClose() { ApocalypseImmersionConfig.SPEC.save(); if (minecraft != null) minecraft.setScreen(parent); }
    private static class Sld extends AbstractSliderButton {
        private final String lbl; private final ForgeConfigSpec.DoubleValue v; private final double min, max;
        Sld(int x, int y, int w, int h, String lbl, ForgeConfigSpec.DoubleValue v, double min, double max) { super(x, y, w, h, Component.empty(), (v.get() - min) / (max - min)); this.lbl = lbl; this.v = v; this.min = min; this.max = max; updateMessage(); }
        @Override protected void updateMessage() { setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", lbl, min + value * (max - min)))); }
        @Override protected void applyValue() { v.set(Math.round((min + value * (max - min)) * 100.0) / 100.0); ApocalypseImmersionConfig.SPEC.save(); }
    }
}