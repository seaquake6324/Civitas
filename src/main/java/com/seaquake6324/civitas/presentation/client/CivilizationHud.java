package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.infrastructure.network.CivilizationHudPayload;
import com.seaquake6324.civitas.presentation.animation.FoundingAnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Large centered transition HUD. Xaero owns the separate persistent information row. */
public final class CivilizationHud {
    private static final int TRANSITION_TICKS = 100;
    private static CivilizationHudPayload current;
    private static String lastIdentity;
    private static int visibleTicks;

    public static void accept(CivilizationHudPayload payload) {
        boolean identityChanged = !payload.cityIdentity().equals(lastIdentity);
        current = payload;
        lastIdentity = payload.cityIdentity();
        if (identityChanged) visibleTicks = TRANSITION_TICKS;
    }

    public static CivilizationHudPayload currentPayload() { return current; }
    public static void tick() { if (!FoundingAnimationManager.isActive() && visibleTicks > 0) visibleTicks--; }
    public static void clear() { current = null; lastIdentity = null; visibleTicks = 0; }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || visibleTicks <= 0 || FoundingAnimationManager.isActive() || minecraft.player == null || minecraft.options.hideGui) return;
        int center = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int commonAlpha = visibleTicks < 20 ? visibleTicks * 255 / 20 : 255;
        Component name = current.cityIdentity().equals("wild")
                ? Component.translatable("civitas.hud.wilderness") : Component.literal(current.cityName());
        graphics.pose().pushMatrix();
        graphics.pose().translate(center, centerY - 10);
        graphics.pose().scale(2.5f, 2.5f);
        graphics.text(minecraft.font, name, -minecraft.font.width(name) / 2, 0,
                withAlpha(current.cityColor(), commonAlpha), true);
        graphics.pose().popMatrix();
    }

    private static int withAlpha(int rgb, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF); }
    private CivilizationHud() {}
}
