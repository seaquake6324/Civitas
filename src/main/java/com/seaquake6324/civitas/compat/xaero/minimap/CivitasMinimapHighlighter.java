package com.seaquake6324.civitas.compat.xaero.minimap;

import com.seaquake6324.civitas.compat.xaero.XaeroHighlightColors;
import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;

public final class CivitasMinimapHighlighter extends ChunkHighlighter {
    public CivitasMinimapHighlighter() { super(false); }

    @Override public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return CityMapOverlayController.isEnabled() && ClientCityMapStore.regionHasCities(dimension, regionX, regionZ);
    }

    @Override protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return XaeroHighlightColors.minimapColors(dimension, chunkX, chunkZ, resultStore);
    }

    @Override public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return CityMapOverlayController.isEnabled() && ClientCityMapStore.cityAt(dimension, chunkX, chunkZ).isPresent();
    }

    @Override public void addChunkHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dimension, int chunkX, int chunkZ, int width) {}
}
