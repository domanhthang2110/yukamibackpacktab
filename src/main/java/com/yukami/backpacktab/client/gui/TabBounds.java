package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.config.TabConfig;

public record TabBounds(int x, int y, int width, int height) {
    static final int WIDTH = 26;
    private static final int ACTIVE_HEIGHT = 32;

    static TabBounds at(int tabIndex, TabConfig.TabPosition position, int screenWidth,
                        int screenHeight, TabConfig.ScreenOffset offset, boolean active) {
        int x = (position.isRight() ? screenWidth - WIDTH - tabIndex * WIDTH : tabIndex * WIDTH) + offset.x;
        int y = (position.isBottom()
                ? screenHeight - 5 + (active ? 0 : 3)
                : -ACTIVE_HEIGHT + 4 + (active ? 0 : 1)) + offset.y;
        int height = active ? ACTIVE_HEIGHT : (position.isBottom() ? 29 : 27);
        return new TabBounds(x, y, WIDTH, height);
    }

    public boolean contains(double xPos, double yPos) {
        return xPos >= x && xPos < x + width && yPos >= y && yPos < y + height;
    }
}
