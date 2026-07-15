package com.seaquake6324.civitas.compat.xaero.mixin.worldmap;

import com.mojang.logging.LogUtils;
import com.seaquake6324.civitas.compat.xaero.worldmap.CivitasWorldMapHighlighter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class WorldMapHighlighterRegistryMixin {
    private static final Logger CIVITAS_LOGGER = LogUtils.getLogger();
    @Inject(method = "end", at = @At("HEAD"), require = 0, remap = false)
    private void civitas$registerHighlighter(CallbackInfo ci) {
        ((HighlighterRegistry)(Object)this).register(new CivitasWorldMapHighlighter());
        CIVITAS_LOGGER.info("Civitas registered Xaero's World Map territory highlighter");
    }
}
