package com.seaquake6324.civitas.compat.xaero.mixin.worldmap;

import com.seaquake6324.civitas.compat.xaero.worldmap.CityMapToggleButton;
import com.seaquake6324.civitas.infrastructure.network.CityMapRecord;
import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import com.seaquake6324.civitas.presentation.client.map.ClientCityMapStore;
import com.seaquake6324.civitas.presentation.screen.CivitasGuiTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class GuiMapMixin extends Screen {
    @Shadow private int mouseBlockPosX;
    @Shadow private int mouseBlockPosZ;
    @Shadow private ResourceKey<Level> mouseBlockDim;

    protected GuiMapMixin() { super(Component.empty()); }

    @Inject(method = "init", at = @At("TAIL"), require = 0, remap = false)
    private void civitas$addCityToggle(CallbackInfo ci) {
        addRenderableWidget(new CityMapToggleButton(0, height - 140));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0, remap = false)
    private void civitas$renderCityTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!CityMapOverlayController.isEnabled() || mouseBlockDim == null) return;
        CityMapRecord city = ClientCityMapStore.cityAt(mouseBlockDim, mouseBlockPosX >> 4, mouseBlockPosZ >> 4).orElse(null);
        if (city == null) return;
        Component lord = Component.translatable("civitas.map.lord", city.lordName().isBlank()
                ? Component.translatable("civitas.map.unknown") : Component.literal(city.lordName()));
        int boxWidth = Math.max(font.width(city.name()), font.width(lord)) + 16;
        int boxHeight = 34;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + boxWidth > width - 4) x = mouseX - boxWidth - 12;
        if (y + boxHeight > height - 4) y = mouseY - boxHeight - 12;
        x = Mth.clamp(x, 4, Math.max(4, width - boxWidth - 4));
        y = Mth.clamp(y, 4, Math.max(4, height - boxHeight - 4));
        CivitasGuiTextures.stoneCityPanel(graphics, x, y, boxWidth, boxHeight);
        graphics.text(font, Component.literal(city.name()), x + 8, y + 7, 0xFF000000 | city.color());
        graphics.text(font, lord, x + 8, y + 19, 0xFFD8D0BE);
    }
}
