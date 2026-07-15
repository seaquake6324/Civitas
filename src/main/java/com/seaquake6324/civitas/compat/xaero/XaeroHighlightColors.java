package com.seaquake6324.civitas.compat.xaero;

import com.seaquake6324.civitas.infrastructure.network.CityMapRecord;
import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class XaeroHighlightColors {
    private static final int FILL_ALPHA = 64;
    private static final int BORDER_ALPHA = 217;
    private static final double MINIMAP_MIN_LUMINANCE = 0.42;
    private static final double WORLD_MAP_MIN_LUMINANCE = 0.32;

    public static int[] minimapColors(ResourceKey<Level> dimension, int chunkX, int chunkZ, int[] result) {
        return colors(dimension, chunkX, chunkZ, result, MINIMAP_MIN_LUMINANCE);
    }

    public static int[] worldMapColors(ResourceKey<Level> dimension, int chunkX, int chunkZ, int[] result) {
        return colors(dimension, chunkX, chunkZ, result, WORLD_MAP_MIN_LUMINANCE);
    }

    private static int[] colors(ResourceKey<Level> dimension, int chunkX, int chunkZ, int[] result,
                                double minimumLuminance) {
        if (!CityMapOverlayController.isEnabled()) return null;
        Optional<CityMapRecord> current = ClientCityMapStore.cityAt(dimension, chunkX, chunkZ);
        if (current.isEmpty()) return null;
        CityMapRecord city = current.get();
        int displayColor = ensureMinimumLuminance(city.color(), minimumLuminance);
        int fill = xaeroColor(displayColor, FILL_ALPHA);
        int border = xaeroColor(displayColor, BORDER_ALPHA);
        result[0] = fill;
        result[1] = sameCity(city, dimension, chunkX, chunkZ - 1) ? fill : border;
        result[2] = sameCity(city, dimension, chunkX + 1, chunkZ) ? fill : border;
        result[3] = sameCity(city, dimension, chunkX, chunkZ + 1) ? fill : border;
        result[4] = sameCity(city, dimension, chunkX - 1, chunkZ) ? fill : border;
        return result;
    }

    /**
     * Raises only the map display colour toward white until it is readable over terrain. The city's stored RGB is
     * never changed. Mixing toward white preserves the relative channel ordering (and therefore the perceived hue)
     * better than clamping channels independently.
     */
    static int ensureMinimumLuminance(int rgb, double minimumLuminance) {
        int red = rgb >>> 16 & 0xFF;
        int green = rgb >>> 8 & 0xFF;
        int blue = rgb & 0xFF;
        if (relativeLuminance(red, green, blue) >= minimumLuminance) return rgb & 0xFFFFFF;

        double low = 0.0;
        double high = 1.0;
        for (int i = 0; i < 10; i++) {
            double mix = (low + high) * 0.5;
            int mixedRed = mixWithWhite(red, mix);
            int mixedGreen = mixWithWhite(green, mix);
            int mixedBlue = mixWithWhite(blue, mix);
            if (relativeLuminance(mixedRed, mixedGreen, mixedBlue) < minimumLuminance) low = mix;
            else high = mix;
        }
        return mixWithWhite(red, high) << 16 | mixWithWhite(green, high) << 8 | mixWithWhite(blue, high);
    }

    private static int mixWithWhite(int channel, double amount) {
        return (int) Math.round(channel + (255 - channel) * amount);
    }

    private static double relativeLuminance(int red, int green, int blue) {
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue);
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static boolean sameCity(CityMapRecord city, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return ClientCityMapStore.cityAt(dimension, chunkX, chunkZ).map(other -> other.id().equals(city.id())).orElse(false);
    }

    private static int xaeroColor(int rgb, int alpha) {
        return ((rgb & 0xFF) << 24) | (((rgb >>> 8) & 0xFF) << 16) | (((rgb >>> 16) & 0xFF) << 8) | alpha;
    }

    private XaeroHighlightColors() {}
}
