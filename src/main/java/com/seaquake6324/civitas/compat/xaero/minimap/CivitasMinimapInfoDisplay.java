package com.seaquake6324.civitas.compat.xaero.minimap;

import com.seaquake6324.civitas.infrastructure.network.CivilizationHudPayload;
import com.seaquake6324.civitas.presentation.client.CivilizationHud;
import net.minecraft.network.chat.Component;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.codec.InfoDisplayCommonStateCodecs;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;

/** Xaero-native, always-on row rendered in the information area below coordinates. */
public final class CivitasMinimapInfoDisplay {
    public static InfoDisplay<Boolean> create() {
        return InfoDisplay.Builder.<Boolean>begin()
                .setId("civitas_status")
                .setName(Component.translatable("civitas.xaero.info"))
                .setDefaultState(true)
                .setCodec(InfoDisplayCommonStateCodecs.BOOLEAN)
                .setWidgetFactory(InfoDisplayCommonWidgetFactories.ALWAYS_ON)
                .setCompiler((display, compiler, session, width, pos) -> {
                    if (!Boolean.TRUE.equals(display.getEffectiveState())) return;
                    CivilizationHudPayload data = CivilizationHud.currentPayload();
                    if (data == null) return;
                    Component city = data.cityIdentity().equals("wild")
                            ? Component.translatable("civitas.hud.wilderness") : Component.literal(data.cityName());
                    compiler.addLine(city.copy().withStyle(style -> style.withColor(data.cityColor())));
                    compiler.addLine(Component.translatable("civitas.xaero.civility",
                            (data.civilityTier()+1)+" / 5")
                            .withStyle(style -> style.withColor(0xFFFFFF)));
                    compiler.addLine(Component.translatable("civitas.xaero.activity",
                            (data.activityTier()+1)+" / 5")
                            .withStyle(style -> style.withColor(0xFFFFFF)));
                })
                .build();
    }

    private CivitasMinimapInfoDisplay() {}
}
