package com.yukami.backpacktab.client.gui.util;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.function.IntUnaryOperator;

public final class ResponsivePanel {
    private ResponsivePanel() {}

    public static Layout findLayout(AbstractContainerScreen<?> screen, int desiredWidth, int minimumWidth, int margin, IntUnaryOperator heightForWidth) {
        return findLayout(screen.width, screen.height, screen.getLeftPos(), screen.getTopPos(),
                screen.getImageHeight(), desiredWidth, minimumWidth, margin, heightForWidth);
    }

    static Layout findLayout(int screenWidth, int screenHeight, int guiLeft, int guiTop,
                             int guiHeight, int desiredWidth, int minimumWidth,
                             int margin, IntUnaryOperator heightForWidth) {
        int guiBottom = guiTop + guiHeight;
        int availableHeight = screenHeight - margin * 2;

        Layout left = sideLayout(margin, margin, guiLeft - (margin * 2), availableHeight, desiredWidth, minimumWidth, heightForWidth);
        if (left != null) {
            return left;
        }

        int fullWidth = Math.min(desiredWidth, Math.max(1, screenWidth - (margin * 2)));
        int fullHeight = heightForWidth.applyAsInt(fullWidth);
        if (guiTop - (margin * 2) >= fullHeight) {
            int x = clamp(guiLeft, margin, screenWidth - fullWidth - margin);
            return new Layout(x, margin, fullWidth, fullHeight);
        }

        if (screenHeight - guiBottom - (margin * 2) >= fullHeight) {
            int x = clamp(guiLeft, margin, screenWidth - fullWidth - margin);
            return new Layout(x, guiBottom + margin, fullWidth, fullHeight);
        }

        int compactWidth = Math.min(minimumWidth, fullWidth);
        int compactHeight = heightForWidth.applyAsInt(compactWidth);
        if (compactHeight > availableHeight && fullHeight <= availableHeight) {
            return new Layout(margin, margin, fullWidth, fullHeight);
        }
        return new Layout(margin, margin, compactWidth, compactHeight);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static Layout sideLayout(int x, int y, int availableWidth, int availableHeight, int desiredWidth, int minimumWidth, IntUnaryOperator heightForWidth) {
        if (availableWidth < minimumWidth) {
            return null;
        }

        int width = Math.min(desiredWidth, availableWidth);
        int height = heightForWidth.applyAsInt(width);
        return height <= availableHeight ? new Layout(x, y, width, height) : null;
    }

    public record Layout(int x, int y, int width, int height) {}
}
