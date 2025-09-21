package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.tabs.InventoryTab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
public class TabRenderer {

    private static final ResourceLocation CREATIVE_INVENTORY_TABS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tabs.png");
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int ITEM_OFFSET_X = 5;
    private static final int ITEM_OFFSET_Y = 7;
    
    private static final int TOP_TAB_BASE_OFFSET = 4;
    private static final int TOP_ACTIVE_Y_ADJUST = 0;
    private static final int TOP_INACTIVE_Y_ADJUST = 1;
    private static final int TOP_INACTIVE_HEIGHT = 27;
    
    private static final int BOTTOM_TAB_BASE_OFFSET = -4;
    private static final int BOTTOM_ACTIVE_Y_ADJUST = 0;
    private static final int BOTTOM_INACTIVE_Y_ADJUST = 3;
    private static final int BOTTOM_INACTIVE_HEIGHT = 30;
    
    private static TabConfig.TabPosition cachedTabPosition = null;
    
    private static TabConfig.TabPosition getTabPosition() {
        if (cachedTabPosition == null) {
            cachedTabPosition = TabConfig.getTabPosition();
        }
        return cachedTabPosition;
    }
    
    private static int getTabX(int tabIndex, TabConfig.TabPosition position, AbstractContainerScreen<?> screen) {
        int screenLeft = screen.getGuiLeft();
        int screenWidth = screen.getXSize();
        
        return switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> screenLeft + (tabIndex * TAB_WIDTH);
            case TOP_RIGHT, BOTTOM_RIGHT -> screenLeft + screenWidth - TAB_WIDTH - (tabIndex * TAB_WIDTH);
        };
    }
    
    private static int getTabY(TabConfig.TabPosition position, AbstractContainerScreen<?> screen, boolean active) {
        int screenTop = screen.getGuiTop();
        int screenHeight = screen.getYSize();
        
        return switch (position) {
            case TOP_LEFT, TOP_RIGHT -> 
                screenTop - TAB_HEIGHT + TOP_TAB_BASE_OFFSET + (active ? TOP_ACTIVE_Y_ADJUST : TOP_INACTIVE_Y_ADJUST);
            case BOTTOM_LEFT, BOTTOM_RIGHT -> 
                screenTop + screenHeight + BOTTOM_TAB_BASE_OFFSET + (active ? BOTTOM_ACTIVE_Y_ADJUST : BOTTOM_INACTIVE_Y_ADJUST);
        };
    }
    
    private static int getTabHeight(TabConfig.TabPosition position, boolean active) {
        if (active) return TAB_HEIGHT;
        return position.isBottom() ? BOTTOM_INACTIVE_HEIGHT : TOP_INACTIVE_HEIGHT;
    }

    public static void invalidateCache() {
        cachedTabPosition = null;
    }

    public static void renderTabs(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        var tabs = TabManager.getActiveTabs();
        if (tabs.isEmpty()) return;
        
        TabConfig.TabPosition position = getTabPosition();
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();
            
            int x = getTabX(i, position, screen);
            int y = getTabY(position, screen, active);
            int height = getTabHeight(position, active);
            
            TabConfig.SpriteCoords coords = TabConfig.getSpriteCoords(position, active, i == 0);
            int spriteV = coords.v;
            if (!active && position.isBottom()) {
                spriteV += (TAB_HEIGHT - height);
            }
            guiGraphics.blit(CREATIVE_INVENTORY_TABS, x, y, coords.u, spriteV, TAB_WIDTH, height);
            guiGraphics.renderItem(tab.getTabIcon(), x + ITEM_OFFSET_X, y + ITEM_OFFSET_Y);
            
            // Tooltip
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + height) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, tab.getHoverText(), mouseX, mouseY);
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
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            boolean active = tab.isActive();
            
            int x = getTabX(i, position, screen);
            int y = getTabY(position, screen, active);
            int height = getTabHeight(position, active);
            
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + height) {
                tabs.forEach(t -> t.setActive(t == tab));
                TabSwitcher.switchToTab(tab, player, minecraft.gameMode);
                return true;
            }
        }
        return false;
    }

    // getEquippedBackpackTab moved to TabManager
}