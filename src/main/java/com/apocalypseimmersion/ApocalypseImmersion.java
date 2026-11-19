package com.apocalypseimmersion;

import com.apocalypseimmersion.client.ApocalypseConfigScreen;
import com.apocalypseimmersion.config.ApocalypseImmersionConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ApocalypseImmersion.MODID)
public class ApocalypseImmersion {
    public static final String MODID = "apocalypseimmersion";

    public ApocalypseImmersion() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ApocalypseImmersionConfig.SPEC, "apocalypseimmersion-common.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ModLoadingContext.get().registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ApocalypseConfigScreen(parent))
                )
        );
    }
}