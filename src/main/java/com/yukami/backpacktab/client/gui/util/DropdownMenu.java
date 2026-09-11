package com.yukami.backpacktab.client.gui.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DropdownMenu<T> {
    private static final int TEXT_PADDING_X = 6;
    private static final int ARROW_COLOR = 0xFFA0A0A0;
    private static final int ARROW_SIZE = 3;
    private static final int TOP_HIGHLIGHT = 0x55FFFFFF;
    private static final int POPUP_BACKGROUND = 0xE0101010;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_TEXT_COLOR = 0xFF55FFFF;
    private static final int LABEL_CLIP_WIDTH = 18;

    private final List<T> options;
    private final Function<T, String> labeler;
    private final Consumer<T> onSelect;

    private int selectedIndex;
    private boolean open;

    private int x;
    private int y;
    private int width;
    private int headerHeight;
    private int optionHeight;
    private boolean opensUpward;

    public DropdownMenu(List<T> options, T initialSelection, Function<T, String> labeler, Consumer<T> onSelect) {
        this.options = new ArrayList<>(options);
        this.labeler = labeler;
        this.onSelect = onSelect;
        setSelected(initialSelection);
    }

    public T getSelected() {
        return options.get(selectedIndex);
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public void setSelected(T value) {
        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            if (option == value || option.equals(value)) {
                selectedIndex = i;
                return;
            }
        }
    }

    public void renderHeader(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            int x,
            int y,
            int width,
            int headerHeight,
            int optionHeight,
            double mouseX,
            double mouseY,
            int maxVisibleY
    ) {
        setBounds(x, y, width, headerHeight, optionHeight, maxVisibleY);

        boolean hovered = contains(mouseX, mouseY, x, y, width, headerHeight);
        int background = hovered || open ? EditorGui.BUTTON_HOVERED : EditorGui.BUTTON;
        guiGraphics.fill(x, y, x + width, y + headerHeight, background);
        guiGraphics.fill(x, y, x + width, y + 1, TOP_HIGHLIGHT);

        String label = font.plainSubstrByWidth(labeler.apply(getSelected()), width - LABEL_CLIP_WIDTH);
        guiGraphics.text(font, label, x + TEXT_PADDING_X, y + (headerHeight - 8) / 2, TEXT_COLOR, false);

        int arrowX = x + width - LABEL_CLIP_WIDTH / 2;
        int arrowY = y + (headerHeight - ARROW_SIZE) / 2;
        for (int row = 0; row < ARROW_SIZE; row++) {
            int halfWidth = (open == opensUpward) ? row : ARROW_SIZE - 1 - row;
            guiGraphics.fill(arrowX - halfWidth, arrowY + row, arrowX + halfWidth + 1, arrowY + row + 1, ARROW_COLOR);
        }
    }

    void setBounds(int x, int y, int width, int headerHeight, int optionHeight, int maxVisibleY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.headerHeight = headerHeight;
        this.optionHeight = optionHeight;

        int popupHeight = getPopupHeight();
        boolean downFits = y + headerHeight + popupHeight <= maxVisibleY;
        opensUpward = !downFits && y >= popupHeight;
    }

    public void renderPopup(GuiGraphicsExtractor guiGraphics, Font font, double mouseX, double mouseY) {
        if (!open || width <= 0) {
            return;
        }
        renderPopupList(guiGraphics, font, mouseX, mouseY);
    }

    private int getPopupHeight() {
        return options.size() * optionHeight + 2;
    }

    private void renderPopupList(GuiGraphicsExtractor guiGraphics, Font font, double mouseX, double mouseY) {
        int popupHeight = getPopupHeight();
        int popupTop = opensUpward ? y - popupHeight : y + headerHeight;

        guiGraphics.fill(x, popupTop, x + width, popupTop + popupHeight, EditorGui.PANEL_BORDER);
        guiGraphics.fill(x + 1, popupTop + 1, x + width - 1, popupTop + popupHeight - 1, POPUP_BACKGROUND);

        for (int i = 0; i < options.size(); i++) {
            int optionY = popupTop + 1 + (i * optionHeight);
            boolean hovered = contains(mouseX, mouseY, x + 1, optionY, width - 2, optionHeight);
            guiGraphics.fill(x + 1, optionY, x + width - 1, optionY + optionHeight,
                    hovered ? EditorGui.BUTTON_HOVERED : EditorGui.BUTTON);

            String label = font.plainSubstrByWidth(labeler.apply(options.get(i)), width - LABEL_CLIP_WIDTH);
            int color = i == selectedIndex ? SELECTED_TEXT_COLOR : TEXT_COLOR;
            guiGraphics.text(font, label, x + TEXT_PADDING_X,
                    optionY + (optionHeight - 8) / 2, color, false);
        }
    }

    public boolean handleMousePressed(double mouseX, double mouseY) {
        if (width <= 0) {
            return false;
        }

        boolean overHeader = contains(mouseX, mouseY, x, y, width, headerHeight);
        if (overHeader) {
            open = !open;
            return true;
        }

        if (open) {
            int chosenIndex = optionIndexAt(mouseX, mouseY);
            open = false;
            if (chosenIndex >= 0 && chosenIndex != selectedIndex) {
                selectedIndex = chosenIndex;
                onSelect.accept(options.get(chosenIndex));
            }
            return true;
        }

        return false;
    }

    private int optionIndexAt(double mouseX, double mouseY) {
        int popupHeight = getPopupHeight();
        int popupTop = opensUpward ? y - popupHeight : y + headerHeight;
        if (!contains(mouseX, mouseY, x + 1, popupTop + 1, width - 2, popupHeight - 2)) {
            return -1;
        }
        int index = (int) ((mouseY - popupTop - 1) / optionHeight);
        return index >= 0 && index < options.size() ? index : -1;
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
