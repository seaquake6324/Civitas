package com.seaquake6324.civitas.presentation.screen;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Reusable Civitas screen surfaces. Sprite metadata supplies the nine-slice behavior. */
public final class CivitasGuiTextures {
    private static final Identifier STONE_CITY_PANEL =
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "stone_city_panel");

    private CivitasGuiTextures() {}

    public static void stoneCityPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, STONE_CITY_PANEL, x, y, width, height);
    }
}
