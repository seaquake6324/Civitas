package com.seaquake6324.civitas.infrastructure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CivitasClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CITY_MAP_OVERLAY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CITY_MAP_OVERLAY = builder.comment("Enable or disable Civitas territory display on supported map mods.")
                .define("cityMapOverlay", true);
        SPEC = builder.build();
    }

    private CivitasClientConfig() {}
}
