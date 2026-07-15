package com.seaquake6324.civitas.presentation.client.map;

import com.seaquake6324.civitas.infrastructure.config.CivitasClientConfig;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CityMapOverlayController {
    private static final String BRIDGE = "com.seaquake6324.civitas.compat.xaero.XaeroRefreshBridge";

    public static boolean isEnabled() { return CivitasClientConfig.CITY_MAP_OVERLAY.get(); }

    public static void setEnabled(boolean enabled, boolean actionbarFeedback) {
        if (isEnabled() == enabled) return;
        CivitasClientConfig.CITY_MAP_OVERLAY.set(enabled);
        dataChanged();
        if (actionbarFeedback) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) minecraft.gui.setOverlayMessage(Component.translatable(
                    enabled ? "civitas.map.overlay.enabled" : "civitas.map.overlay.disabled"), false);
        }
    }

    public static void toggle(boolean actionbarFeedback) { setEnabled(!isEnabled(), actionbarFeedback); }

    public static void dataChanged() {
        try {
            Class<?> type = Class.forName(BRIDGE, false, CityMapOverlayController.class.getClassLoader());
            Method refresh = type.getMethod("refresh");
            refresh.invoke(null);
        } catch (ClassNotFoundException | LinkageError ignored) {
            // Xaero support classes are deliberately optional.
        } catch (ReflectiveOperationException ignored) {
            // An absent or unsupported Xaero runtime must never break Civitas.
        }
    }

    private CityMapOverlayController() {}
}
