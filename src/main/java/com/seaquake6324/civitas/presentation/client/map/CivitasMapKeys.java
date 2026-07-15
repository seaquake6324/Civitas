package com.seaquake6324.civitas.presentation.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.seaquake6324.civitas.infrastructure.network.ToggleRegionDebugPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.Minecraft;
import com.seaquake6324.civitas.presentation.client.RegionDebugOverlay;
import org.lwjgl.glfw.GLFW;

public final class CivitasMapKeys {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "maps"));
    public static final KeyMapping TOGGLE_CITY_MAP = new KeyMapping(
            "key.civitas.toggle_city_map", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping TOGGLE_REGION_DEBUG = new KeyMapping(
            "key.civitas.toggle_region_debug", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_CITY_MAP);
        event.register(TOGGLE_REGION_DEBUG);
    }

    public static void tick() {
        while (TOGGLE_CITY_MAP.consumeClick()) CityMapOverlayController.toggle(true);
        while (TOGGLE_REGION_DEBUG.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (RegionDebugOverlay.isVisible() && shiftDown(minecraft)) {
                RegionDebugOverlay.toggleFrozen();
            } else if (RegionDebugOverlay.isVisible() && controlDown(minecraft)) {
                RegionDebugOverlay.cyclePage(1);
            } else if (minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(ToggleRegionDebugPayload.INSTANCE);
            }
        }
    }

    private static boolean controlDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean shiftDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private CivitasMapKeys() {}
}
