package com.seaquake6324.civitas.compat.xaero.mixin.minimap;

import com.mojang.logging.LogUtils;
import com.seaquake6324.civitas.compat.xaero.minimap.CivitasMinimapInfoDisplay;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.Minimap;

@Mixin(value = Minimap.class, remap = false)
public abstract class MinimapInfoDisplayMixin {
    private static final Logger CIVITAS_LOGGER = LogUtils.getLogger();

    @Inject(method = "<init>", at = @At("TAIL"), require = 0, remap = false)
    private void civitas$registerPersistentInfo(CallbackInfo callback) {
        try {
            Minimap minimap = (Minimap)(Object)this;
            minimap.getInfoDisplays().getManager().add(CivitasMinimapInfoDisplay.create());
            CIVITAS_LOGGER.info("Civitas registered Xaero's Minimap civilization information row");
        } catch (LinkageError | RuntimeException exception) {
            CIVITAS_LOGGER.warn("Civitas disabled Xaero's Minimap civilization information row", exception);
        }
    }
}
