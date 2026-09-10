package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.config.TabConfig;
import com.yukami.backpacktab.client.tabs.InventoryTab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TabRenderer {
    private TabRenderer() {}
    private static final int ITEM_OFFSET_X = 5;
    private static final int ITEM_OFFSET_Y = 8;
    private static final int ICON_SIZE = 16;

    private static TabConfig.TabPosition cachedTabPosition;

    private static TabConfig.TabPosition getTabPosition() {
        if (cachedTabPosition == null) {
            cachedTabPosition = TabConfig.getTabPosition();
        }
        return cachedTabPosition;
    }

    private static TabConfig.TabPosition resolveTabPosition(AbstractContainerScreen<?> screen) {
        TabConfig.TabPosition override = TabConfig.getScreenPositionOverride(screen.getClass().getSimpleName());
        return override != null ? override : getTabPosition();
    }

    private static Identifier getTabSprite(TabConfig.TabPosition position, boolean active, boolean isFirstTab) {
        String state = active ? "selected" : "unselected";
        String tabNumber = isFirstTab ? (position.isRight() ? "7" : "1") : "2";
        String positionName = position.isBottom() ? "bottom" : "top";
        return Identifier.withDefaultNamespace("container/creative_inventory/tab_" + positionName + "_" + state + "_" + tabNumber);
    }

    private static TabConfig.ScreenOffset getScreenOffset(AbstractContainerScreen<?> screen, TabConfig.TabPosition position) {
        String screenClassName = screen.getClass().getSimpleName();
        return TabConfig.getScreenOffset(screenClassName, position);
    }

    public static TabBounds getTabBounds(
            int tabIndex,
            TabConfig.TabPosition position,
            AbstractContainerScreen<?> screen,
            TabConfig.ScreenOffset offset,
            boolean active
    ) {
        return TabBounds.at(tabIndex, position, screen.getImageWidth(), screen.getImageHeight(), offset, active);
    }

    public static void invalidateCache() {
        cachedTabPosition = null;
    }

    public static void renderTabs(GuiGraphicsExtractor guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(screen.getLeftPos(), screen.getTopPos());
        try {
            renderTabsLocal(guiGraphics, screen, mouseX, mouseY);
        } finally {
            guiGraphics.pose().popMatrix();
        }
    }

    private static void renderTabsLocal(GuiGraphicsExtractor guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (TabOffsetEditor.isEnabledFor(screen)) {
            TabOffsetEditor.render(guiGraphics, screen, mouseX, mouseY);
            return;
        }

        var tabs = TabManager.getActiveTabsView();
        if (tabs.isEmpty()) return;

        TabConfig.TabPosition position = resolveTabPosition(screen);
        renderTabsAtPosition(guiGraphics, screen, mouseX, mouseY, position, getScreenOffset(screen, position), false);
    }

    static void renderTabsAtPosition(
            GuiGraphicsExtractor guiGraphics,
            AbstractContainerScreen<?> screen,
            int mouseX,
            int mouseY,
            TabConfig.TabPosition position,
            TabConfig.ScreenOffset offset,
            boolean selectedPreview
    ) {
        var tabs = TabManager.getActiveTabsView();
        if (tabs.isEmpty()) return;

        int localMouseX = mouseX - screen.getLeftPos();
        int localMouseY = mouseY - screen.getTopPos();

        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();

            TabBounds bounds = getTabBounds(i, position, screen, offset, active);
            int x = bounds.x();
            int y = bounds.y();
            int height = bounds.height();

            Identifier sprite = getTabSprite(position, active, i == 0);
            boolean dimmedPreview = !selectedPreview && TabOffsetEditor.isEnabledFor(screen);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, bounds.width(), height,
                    dimmedPreview ? 0xA6737373 : 0xFFFFFFFF);

            int itemX = x + ITEM_OFFSET_X;
            int itemY = position.isBottom()
                    ? (y + height) - ITEM_OFFSET_Y - ICON_SIZE
                    : y + ITEM_OFFSET_Y;
            guiGraphics.item(tab.getTabIcon(), itemX, itemY);

            if (!TabOffsetEditor.isEnabledFor(screen) &&
                    bounds.contains(localMouseX, localMouseY)) {
                guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tab.getHoverText(), mouseX, mouseY);
            }
        }
    }

    public static boolean handleTabClick(double mouseX, double mouseY, int button, AbstractContainerScreen<?> screen) {
        if (TabOffsetEditor.isEnabled()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return false;

        var tabs = TabManager.getActiveTabsView();
        if (tabs.isEmpty()) return false;

        TabConfig.TabPosition position = resolveTabPosition(screen);

        TabConfig.ScreenOffset offset = getScreenOffset(screen, position);

        double localMouseX = mouseX - screen.getLeftPos();
        double localMouseY = mouseY - screen.getTopPos();

        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();

            TabBounds bounds = getTabBounds(i, position, screen, offset, active);
            if (bounds.contains(localMouseX, localMouseY)) {
                if (button == 0 && !active && !tab.matchesCurrentScreen(screen)) {
                    TabSwitcher.switchToTab(tab, player, minecraft.gameMode);
                }
                return true;
            }
        }
        return false;
    }
}
