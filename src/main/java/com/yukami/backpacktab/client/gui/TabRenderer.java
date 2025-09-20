package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
// CarriedItemUtil import removed - now handled by TabSwitcher

import static com.yukami.backpacktab.YukamiBackpackTab.LOGGER;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.minecraft.client.renderer.RenderPipelines;

public class TabRenderer {

    // Individual tab sprites for 1.21.8
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    
    // Precalculated constants
    private static final int ITEM_OFFSET_X = (TAB_WIDTH - 16) / 2;  // 5
    private static final int ITEM_OFFSET_Y = (TAB_HEIGHT - 16) / 2; // 8
    private static final int INACTIVE_TAB_HEIGHT_BOTTOM = TAB_HEIGHT - 3; // 29
    private static final int INACTIVE_TAB_HEIGHT_TOP = TAB_HEIGHT - 5;    // 27
    
    // Config caching
    private static TabConfig.TabPosition cachedTabPosition = null;
    
    private static TabConfig.TabPosition getTabPosition() {
        if (cachedTabPosition == null) {
            cachedTabPosition = TabConfig.getTabPosition();
        }
        return cachedTabPosition;
    }
    
    private static int getInactiveTabHeight(TabConfig.TabPosition position) {
        return position.isBottom() ? INACTIVE_TAB_HEIGHT_BOTTOM : INACTIVE_TAB_HEIGHT_TOP;
    }

    
    private static ResourceLocation getTabSprite(boolean active, boolean isFirstTab) {
        TabConfig.TabPosition position = getTabPosition();
        String state = active ? "selected" : "unselected";
        String tabNumber = isFirstTab ? (position.isRight() ? "7" : "1") : "2";
        String positionName = position.isBottom() ? "bottom" : "top";
        return ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_" + positionName + "_" + state + "_" + tabNumber);
    }

    // Simple layout cache that only stores what we need
    private static record Layout(
        int startX,
        int startY,
        int screenWidth,
        int screenHeight
    ) {
        boolean isValid(AbstractContainerScreen<?> screen) {
            return screen.getXSize() == screenWidth 
                && screen.getYSize() == screenHeight;
        }
        int getTabX(int index, TabConfig.TabPosition position) {
            // For right alignment, start from right edge and move left
            // For left alignment, start from left edge and move right
            return position.isRight() 
                ? screenWidth - ((index + 1)  * TAB_WIDTH)  // Changed calculation
                : index * TAB_WIDTH;
        }
        int getTabY(TabConfig.TabPosition position, boolean active) {
            if (position.isBottom()) {
                return active ? screenHeight - 4 : screenHeight - 1;  // bottom + active
            } else {
                return active ? -TAB_HEIGHT + 4 : -TAB_HEIGHT + 5;  // top + inactive
            }
        }
    }

    private static Layout cachedLayout;

    private static Layout getLayout(AbstractContainerScreen<?> screen) {
        // Recalculate only if cache is invalid or dimensions changed
        if (cachedLayout == null || !cachedLayout.isValid(screen)) {
            cachedLayout = new Layout(
                0, // startX
                getTabPosition().isBottom() ? screen.getYSize() : 0, // startY
                screen.getXSize(),
                screen.getYSize()
            );
        }
        return cachedLayout;
    }

    public static void invalidateCache() {
        cachedLayout = null;
    }

    public static void renderTabs(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        // Early validation
        var tabs = InventoryTabManager.getActiveTabs();
        if (tabs.isEmpty()) return;  // No tabs to render
        
        Layout layout = getLayout(screen);
        TabConfig.TabPosition position = getTabPosition();
        
        // Render tabs first
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            int tabX = layout.getTabX(i, position);
            int tabY = layout.getTabY(position, tab.isActive());
            renderTab(guiGraphics, tab, tabX, tabY, position, i == 0);
        }
        
        // Render tooltips after all tabs
        renderTooltips(guiGraphics, screen, tabs, layout, mouseX, mouseY);
    }

    private static void renderTab(GuiGraphics guiGraphics, InventoryTab tab, 
                                int x, int y, TabConfig.TabPosition position, boolean isFirstTab) {
        boolean active = tab.isActive();
        ResourceLocation sprite = getTabSprite(active, isFirstTab);
        
        // Calculate height adjustment for inactive tabs
        int renderHeight = active ? TAB_HEIGHT : getInactiveTabHeight(position);

        // Render background and item
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, renderHeight);

        
        // Center item in tab
        int itemX = x + ITEM_OFFSET_X;
        int itemY = y + ITEM_OFFSET_Y;
        if (!active && position.isBottom()) {
            itemY -= 2; // Adjust for bottom inactive tab
        } else if (!active && !position.isBottom()) {
            itemY -= 1; // Adjust for top inactive tab
        }
        guiGraphics.renderItem(tab.getTabIcon(), itemX, itemY);
    }

    private static void renderTooltips(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, 
                                    java.util.List<InventoryTab> tabs, Layout layout, int mouseX, int mouseY) {
        TabConfig.TabPosition position = getTabPosition();
        int screenLeft = screen.getGuiLeft();
        int screenTop = screen.getGuiTop();
        
        // Convert mouse coordinates to local space
        int localMouseX = mouseX - screenLeft;
        int localMouseY = mouseY - screenTop;
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            int tabX = layout.getTabX(i, position);
            int tabY = layout.getTabY(position, tab.isActive());
            
            boolean active = tab.isActive();
            int renderHeight = active ? TAB_HEIGHT : getInactiveTabHeight(position);
            
            // Check if mouse is over this tab
            if (localMouseX >= tabX && localMouseX < tabX + TAB_WIDTH && 
                localMouseY >= tabY && 
                localMouseY < tabY + renderHeight) {
                
            // Get tooltip text
            List<Component> tooltipLines = List.of(tab.getHoverText());
            
            // Set tooltip for next frame like creative inventory does
            guiGraphics.setTooltipForNextFrame(
                Minecraft.getInstance().font,
                tooltipLines,
                Optional.empty(),
                mouseX,
                mouseY
            );
            break; // Only render one tooltip at a time
            }
        }
    }

    public static boolean handleTabClick(double mouseX, double mouseY, int button, AbstractContainerScreen<?> screen) {
        // Early returns
        if (button != 0) return false;
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return false;

        var tabs = InventoryTabManager.getActiveTabs();
        if (tabs.isEmpty()) return false;  // No tabs to click

        Layout layout = getLayout(screen);
        TabConfig.TabPosition position = getTabPosition();
        
        // Convert mouse coordinates to local space
        int localMouseX = (int)mouseX - screen.getGuiLeft();
        int localMouseY = (int)mouseY - screen.getGuiTop();
        
        for (int i = 0; i < tabs.size(); i++) {
            InventoryTab tab = tabs.get(i);
            int tabX = layout.getTabX(i, position);
            int tabY = layout.getTabY(position, tab.isActive());
            
            // Calculate tab bounds with height adjustment for inactive tabs
            boolean active = tab.isActive();
            int renderHeight = active ? TAB_HEIGHT : getInactiveTabHeight(position);
            
            // Check if mouse is over this tab
            if (localMouseX >= tabX && localMouseX < tabX + TAB_WIDTH && 
                localMouseY >= tabY && 
                localMouseY < tabY + renderHeight) {
                
                // Set tab states
                tabs.forEach(t -> t.setActive(t == tab));
                
                // Use TabSwitcher to handle the complex switching logic
                TabSwitcher.switchToTab(tab, player, minecraft.gameMode);
                return true;
            }
        }
        
        return false;
    }

    // getEquippedBackpackTab moved to TabProvider
}