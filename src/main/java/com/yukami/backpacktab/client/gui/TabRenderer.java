package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.tabs.InventoryTab;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.RenderPipelines;

public class TabRenderer {

    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SPACING = 26;
    private static final int ITEM_OFFSET_X = 5;
    private static final int ITEM_OFFSET_Y = 8;
    
    private static final int TOP_TAB_BASE_OFFSET = 4;
    private static final int TOP_ACTIVE_Y_ADJUST = 0;
    private static final int TOP_INACTIVE_Y_ADJUST = 1;
    private static final int TOP_INACTIVE_HEIGHT = 27;
    
    private static final int BOTTOM_TAB_BASE_OFFSET = -4;
    private static final int BOTTOM_ACTIVE_Y_ADJUST = 0;
    private static final int BOTTOM_INACTIVE_Y_ADJUST = 3;
    private static final int BOTTOM_INACTIVE_HEIGHT = 29;
    private static TabConfig.TabPosition cachedTabPosition = null;
    
    private static TabConfig.TabPosition getTabPosition() {
        if (cachedTabPosition == null) {
            cachedTabPosition = TabConfig.getTabPosition();
        }
        return cachedTabPosition;
    }
    
    private static int getTabX(int tabIndex, TabConfig.TabPosition position, AbstractContainerScreen<?> screen) {
        int screenWidth = screen.getXSize();
        
        return switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> tabIndex * TAB_SPACING;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - TAB_WIDTH - (tabIndex * TAB_SPACING);
        };
    }
    
    private static int getTabY(TabConfig.TabPosition position, AbstractContainerScreen<?> screen, boolean active) {
        int screenHeight = screen.getYSize();
        
        return switch (position) {
            case TOP_LEFT, TOP_RIGHT -> 
                -TAB_HEIGHT + TOP_TAB_BASE_OFFSET + (active ? TOP_ACTIVE_Y_ADJUST : TOP_INACTIVE_Y_ADJUST);
            case BOTTOM_LEFT, BOTTOM_RIGHT -> 
                screenHeight + BOTTOM_TAB_BASE_OFFSET + (active ? BOTTOM_ACTIVE_Y_ADJUST : BOTTOM_INACTIVE_Y_ADJUST);
        };
    }
    
    private static int getTabHeight(TabConfig.TabPosition position, boolean active) {
        if (active) return TAB_HEIGHT;
        return position.isBottom() ? BOTTOM_INACTIVE_HEIGHT : TOP_INACTIVE_HEIGHT;
    }
    
    private static ResourceLocation getTabSprite(boolean active, boolean isFirstTab) {
        TabConfig.TabPosition position = getTabPosition();
        String state = active ? "selected" : "unselected";
        String tabNumber = isFirstTab ? (position.isRight() ? "7" : "1") : "2";
        String positionName = position.isBottom() ? "bottom" : "top";
        return ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_" + positionName + "_" + state + "_" + tabNumber);
    }

    public static void invalidateCache() {
        cachedTabPosition = null;
    }

    public static void renderTabs(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        var tabs = TabManager.getActiveTabs();
        if (tabs.isEmpty()) return;
        
        TabConfig.TabPosition position = getTabPosition();
        int screenLeft = screen.getGuiLeft();
        int screenTop = screen.getGuiTop();
        
        int localMouseX = mouseX - screenLeft;
        int localMouseY = mouseY - screenTop;
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();
            
            int x = getTabX(i, position, screen);
            int y = getTabY(position, screen, active);
            int height = getTabHeight(position, active);
            
            ResourceLocation sprite = getTabSprite(active, i == 0);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, height);
            
            int itemX = x + ITEM_OFFSET_X;
            int itemY = y + ITEM_OFFSET_Y;
            if (!active && position.isBottom()) {
                itemY -= 2;
            } else if (!active && !position.isBottom()) {
                itemY -= 1;
            }
            guiGraphics.renderItem(tab.getTabIcon(), itemX, itemY);
            
            // Tooltip
            if (localMouseX >= x && localMouseX < x + TAB_WIDTH && localMouseY >= y && localMouseY < y + height) {
                List<Component> tooltipLines = List.of(tab.getHoverText());
                guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, Optional.empty(), mouseX, mouseY);
            }
        }
    }


    public static boolean handleTabClick(double mouseX, double mouseY, int button, AbstractContainerScreen<?> screen) {
        if (button != 0) return false;
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return false;

        var tabs = TabManager.getActiveTabs();
        if (tabs.isEmpty()) return false;
        
        TabConfig.TabPosition position = getTabPosition();
        
        int localMouseX = (int)mouseX - screen.getGuiLeft();
        int localMouseY = (int)mouseY - screen.getGuiTop();
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();
            
            int x = getTabX(i, position, screen);
            int y = getTabY(position, screen, active);
            int height = getTabHeight(position, active);
            
            if (localMouseX >= x && localMouseX < x + TAB_WIDTH && 
                localMouseY >= y && localMouseY < y + height) {
                
                tabs.forEach(t -> t.setActive(t == tab));
                TabSwitcher.switchToTab(tab, player, minecraft.gameMode);
                return true;
            }
        }
        return false;
    }

    // getEquippedBackpackTab moved to TabProvider
}