package com.seaquake6324.civitas.compat.xaero.worldmap;

import com.seaquake6324.civitas.compat.xaero.XaeroHighlightColors;
import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.highlight.ChunkHighlighter;

public final class CivitasWorldMapHighlighter extends ChunkHighlighter {
    public CivitasWorldMapHighlighter() { super(false); }

    @Override public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return CityMapOverlayController.isEnabled() ? ClientCityMapStore.regionHash(dimension, regionX, regionZ) : 0;
    }

    @Override public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return CityMapOverlayController.isEnabled() && ClientCityMapStore.regionHasCities(dimension, regionX, regionZ);
    }

    @Override protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return XaeroHighlightColors.worldMapColors(dimension, chunkX, chunkZ, resultStore);
    }

    @Override public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return CityMapOverlayController.isEnabled() && ClientCityMapStore.cityAt(dimension, chunkX, chunkZ).isPresent();
    }

    @Override public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) { return null; }
    @Override public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) { return null; }
    @Override public void addMinimapBlockHighlightTooltips(java.util.List<Component> tooltips, ResourceKey<Level> dimension, int x, int z, int width) {}
}
