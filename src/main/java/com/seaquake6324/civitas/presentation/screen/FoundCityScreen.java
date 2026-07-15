package com.seaquake6324.civitas.presentation.screen;

import com.seaquake6324.civitas.domain.CityName;
import com.seaquake6324.civitas.infrastructure.network.SubmitFoundingPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class FoundCityScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 224;
    private final BlockPos corePos;
    private EditBox nameBox;
    private EditBox hexBox;
    private Button createButton;
    private int selectedColor = 0x808080;
    private int left;
    private int top;
    private Component notice = Component.empty();
    private int noticeColor = 0xFFFF7777;
    private int noticeTicks;
    private boolean waiting;

    public FoundCityScreen(BlockPos corePos) {
        super(Component.translatable("civitas.gui.found_city"));
        this.corePos = corePos;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        nameBox = new EditBox(font, left + 24, top + 46, 232, 20, Component.translatable("civitas.gui.city_name"));
        nameBox.setMaxLength(40);
        nameBox.setHint(Component.translatable("civitas.gui.name_hint"));
        addRenderableWidget(nameBox);
        hexBox = new EditBox(font, left + 174, top + 155, 82, 20, Component.translatable("civitas.gui.hex_color"));
        hexBox.setMaxLength(7);
        hexBox.setValue("#808080");
        hexBox.setResponder(this::onHexChanged);
        addRenderableWidget(hexBox);
        createButton = Button.builder(Component.translatable("civitas.gui.create"), button -> submit())
                .bounds(left + 70, top + 190, 140, 20).build();
        addRenderableWidget(createButton);
        setInitialFocus(nameBox);
    }

    private void onHexChanged(String text) {
        String value = text.startsWith("#") ? text.substring(1) : text;
        if (value.matches("[0-9a-fA-F]{6}")) {
            selectedColor = Integer.parseInt(value, 16);
            if (noticeTicks > 0 && notice.getString().equals(Component.translatable("civitas.gui.invalid_hex").getString())) noticeTicks = 0;
        }
    }

    private boolean hasValidHex() {
        String value = hexBox.getValue();
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("[0-9a-fA-F]{6}");
    }

    private void submit() {
        if (waiting) return;
        CityName.Validation name = CityName.validate(nameBox.getValue());
        if (!name.valid()) {
            showNotice(Component.translatable(name.errorKey()), false);
            return;
        }
        if (!hasValidHex()) {
            showNotice(Component.translatable("civitas.gui.invalid_hex"), false);
            return;
        }
        waiting = true;
        createButton.active = false;
        showNotice(Component.translatable("civitas.gui.validating"), true);
        ClientPacketDistributor.sendToServer(new SubmitFoundingPayload(corePos, name.normalized(), selectedColor));
    }

    public void handleResult(boolean success, String messageKey) {
        waiting = false;
        if (success) {
            minecraft.popGuiLayer();
        } else {
            createButton.active = true;
            showNotice(Component.translatable(messageKey), false);
        }
    }

    private void showNotice(Component message, boolean neutral) {
        notice = message;
        noticeColor = neutral ? 0xFFD8D0BE : 0xFFFF7777;
        noticeTicks = 80;
    }

    @Override
    public void tick() {
        super.tick();
        if (noticeTicks > 0) noticeTicks--;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        CivitasGuiTextures.stoneCityPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.centeredText(font, title, width / 2, top + 14, 0xFFF4ECD9);
        graphics.text(font, Component.translatable("civitas.gui.city_name"), left + 24, top + 34, 0xFFD8D0BE);
        graphics.text(font, Component.translatable("civitas.gui.city_color"), left + 24, top + 74, 0xFFD8D0BE);
        drawSpectrum(graphics);
        graphics.fill(left + 142, top + 155, left + 164, top + 175, 0xFF000000 | selectedColor);
        graphics.outline(left + 142, top + 155, 22, 20, 0xFFC6BCA8);
        if (!hasValidHex() && hexBox != null && hexBox.isFocused()) {
            graphics.text(font, Component.translatable("civitas.gui.invalid_hex"), left + 174, top + 178, 0xFFFF7777);
        } else if (noticeTicks > 0) {
            graphics.centeredText(font, notice, width / 2, top + 178, noticeColor);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSpectrum(GuiGraphicsExtractor graphics) {
        int x0 = left + 24;
        int y0 = top + 88;
        int columns = 46;
        int rows = 10;
        for (int x = 0; x < columns; x++) {
            float hue = x / (float) columns;
            for (int y = 0; y < rows; y++) {
                float p = y / (float)(rows - 1);
                float saturation = p < 0.5F ? p * 2.0F : 1.0F;
                float brightness = p < 0.5F ? 1.0F : 1.0F - (p - 0.5F) * 1.6F;
                int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
                graphics.fill(x0 + x * 5, y0 + y * 6, x0 + x * 5 + 5, y0 + y * 6 + 6, 0xFF000000 | rgb);
            }
        }
        graphics.outline(x0, y0, columns * 5, rows * 6, 0xFF6E6254);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x0 = left + 24;
        int y0 = top + 88;
        if (event.button() == 0 && event.x() >= x0 && event.x() < x0 + 230 && event.y() >= y0 && event.y() < y0 + 60) {
            float hue = (float)(event.x() - x0) / 230.0F;
            float p = (float)(event.y() - y0) / 60.0F;
            float saturation = p < 0.5F ? p * 2.0F : 1.0F;
            float brightness = p < 0.5F ? 1.0F : 1.0F - (p - 0.5F) * 1.6F;
            selectedColor = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
            hexBox.setValue(String.format(Locale.ROOT, "#%06X", selectedColor));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
