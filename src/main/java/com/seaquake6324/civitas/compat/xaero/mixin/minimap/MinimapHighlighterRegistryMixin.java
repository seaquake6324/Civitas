package com.seaquake6324.civitas.compat.xaero.mixin.minimap;

import com.mojang.logging.LogUtils;
import com.seaquake6324.civitas.compat.xaero.minimap.CivitasMinimapHighlighter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.highlight.HighlighterRegistry;

@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class MinimapHighlighterRegistryMixin {
    private static final Logger CIVITAS_LOGGER = LogUtils.getLogger();
    @Inject(method = "end", at = @At("HEAD"), require = 0, remap = false)
    private void civitas$registerHighlighter(CallbackInfo ci) {
        ((HighlighterRegistry)(Object)this).register(new CivitasMinimapHighlighter());
        CIVITAS_LOGGER.info("Civitas registered Xaero's Minimap territory highlighter");
    }
}
