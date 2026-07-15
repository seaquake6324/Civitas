package com.seaquake6324.civitas.compat.xaero.worldmap;

import com.seaquake6324.civitas.presentation.client.map.CityMapOverlayController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xaero.map.gui.TooltipButton;

public final class CityMapToggleButton extends TooltipButton {
    // These are the exact tint values used by Xaero's GuiTexturedButton.
    private static final int NORMAL = 0xFFFCFCFC;
    private static final int HOVERED = 0xFFE6E6E6;
    private static final int DISABLED = 0xFF404040;
    private static final int OFF_GLYPH = 0xFF787878;
    private static final String[] CASTLE = {
            "#.#......#.#",
            "###..##..###",
            "###..##..###",
            "#.#..##..#.#",
            "###.####.###",
            "############",
            "############",
            "#####..#####",
            "#####..#####",
            "#####..#####",
            "#####..#####",
            "############"
    };

    public CityMapToggleButton(int x, int y) {
        super(x, y, 20, 20, Component.empty(), button -> CityMapOverlayController.toggle(false),
                () -> new xaero.lib.client.gui.widget.Tooltip(Component.translatable("civitas.map.button")));
    }

    @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX() + 4;
        int y = getY() + 4 - (active && isHovered ? 1 : 0);
        int color = !active ? DISABLED : isHovered ? HOVERED : NORMAL;
        drawCastle(graphics, x, y, CityMapOverlayController.isEnabled() ? color : OFF_GLYPH);
        if (!CityMapOverlayController.isEnabled()) drawDisabledSlash(graphics, x, y, color);
    }

    private static void drawCastle(GuiGraphicsExtractor graphics, int x, int y, int color) {
        // Exactly mirrored 12x12 bitmap: the towers, windows, battlements and gate share one centre axis.
        for (int row = 0; row < CASTLE.length; row++) {
            String pixels = CASTLE[row];
            for (int column = 0; column < pixels.length(); column++) {
                if (pixels.charAt(column) == '#') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    private static void drawDisabledSlash(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int offset = 0; offset < 10; offset++) {
            graphics.fill(x + 1 + offset, y + 10 - offset, x + 2 + offset, y + 11 - offset, color);
        }
    }
}
