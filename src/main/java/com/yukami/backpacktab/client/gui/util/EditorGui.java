package com.yukami.backpacktab.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public final class EditorGui {
    public static final int PANEL_BACKGROUND = 0xF0181B20;
    public static final int PANEL_BORDER = 0xFF5F6368;
    public static final int BUTTON = 0xFF3A3D42;
    public static final int BUTTON_HOVERED = 0xFF565B63;
    private static final int BUTTON_PRIMARY = 0xFF285C70;
    private static final int BUTTON_PRIMARY_HOVERED = 0xFF367B93;
    private static final int SEPARATOR = 0x40FFFFFF;
    private static final int SECTION_TITLE_GAP = 8;

    private final List<Button> buttons = new ArrayList<>();

    public void clearButtons() {
        buttons.clear();
    }

    public void panel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_BACKGROUND);
    }

    public void separator(GuiGraphicsExtractor guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width, y + 1, SEPARATOR);
    }

    public void sectionHeader(GuiGraphicsExtractor guiGraphics, String title, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        String text = font.plainSubstrByWidth(title, Math.max(0, width - SECTION_TITLE_GAP * 4));
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int rightX = textX + textWidth + SECTION_TITLE_GAP;
        if (textX - SECTION_TITLE_GAP > x) separator(guiGraphics, x, y, textX - SECTION_TITLE_GAP - x);
        if (rightX < x + width) separator(guiGraphics, rightX, y, x + width - rightX);
        label(guiGraphics, text, textX, y - font.lineHeight / 2, color);
    }

    public void label(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color) {
        guiGraphics.text(Minecraft.getInstance().font, text, x, y, color, false);
    }

    public void button(GuiGraphicsExtractor guiGraphics, String label, int x, int y, int width, int height, double mouseX, double mouseY, Runnable action) {
        button(guiGraphics, label, x, y, width, height, mouseX, mouseY, action, BUTTON, BUTTON_HOVERED);
    }

    public void primaryButton(GuiGraphicsExtractor guiGraphics, String label, int x, int y, int width, int height, double mouseX, double mouseY, Runnable action) {
        button(guiGraphics, label, x, y, width, height, mouseX, mouseY, action, BUTTON_PRIMARY, BUTTON_PRIMARY_HOVERED);
    }

    public boolean click(double mouseX, double mouseY) {
        for (Button button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                button.action().run();
                return true;
            }
        }
        return false;
    }

    private void button(
            GuiGraphicsExtractor guiGraphics,
            String label,
            int x,
            int y,
            int width,
            int height,
            double mouseX,
            double mouseY,
            Runnable action,
            int normalColor,
            int hoveredColor
    ) {
        Button button = new Button(x, y, width, height, action);
        buttons.add(button);

        boolean hovered = button.contains(mouseX, mouseY);
        int color = hovered ? hoveredColor : normalColor;
        guiGraphics.fill(x, y, x + width, y + height, color);
        guiGraphics.fill(x, y, x + width, y + 1, 0x55FFFFFF);

        int textX = x + Math.max(4, (width - Minecraft.getInstance().font.width(label)) / 2);
        int textY = y + (height - 8) / 2;
        label(guiGraphics, label, textX, textY, 0xFFFFFFFF);
    }

    private record Button(int x, int y, int width, int height, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
