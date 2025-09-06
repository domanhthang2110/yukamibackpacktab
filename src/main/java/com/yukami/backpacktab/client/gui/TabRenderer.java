package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.util.TabPositionCalculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public class TabRenderer {

    private static final ResourceLocation CREATIVE_INVENTORY_TABS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tabs.png");
    private static final int TAB_TEXTURE_WIDTH = 26;
    private static final int TAB_TEXTURE_HEIGHT = 32;

    // Define constants for tab dimensions
    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 32;
    public static final int TAB_SPACING = TAB_WIDTH - 2; // Keep current spacing

    /**
     * Renders all tabs with height clipping for inactive tabs
     */
    public static void renderTabs(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (screen == null) return;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // If there's a stored block and it's no longer valid, don't render any tabs
        if (InventoryTabManager.getStoredBlockPos() != null && !InventoryTabManager.isBlockStillValid(InventoryTabManager.getStoredBlockPos())) {
            return;
        }
        
        java.util.List<InventoryTab> activeTabs = InventoryTabManager.getActiveTabs();
        if (activeTabs.isEmpty()) {
            return;
        }
        
        TabConfig.TabPosition position = TabConfig.getTabPosition();
        
        int screenLeft = screen.getGuiLeft();
        int screenTop = screen.getGuiTop();
        int screenWidth = screen.getXSize();
        int screenHeight = screen.getYSize();
        
        TabPositionCalculator.TabLayout layout = TabPositionCalculator.calculateLayout(
            position, screenLeft, screenTop, screenWidth, screenHeight, TAB_WIDTH, TAB_HEIGHT
        );
        
        for (int i = 0; i < activeTabs.size(); i++) {
            InventoryTab tab = activeTabs.get(i);
            int tabX = layout.getTabX(i, TAB_SPACING);
            boolean active = tab.matchesCurrentScreen(screen);
            boolean isFirstTab = (i == 0);
            
            renderTab(guiGraphics, tab.getTabIcon(), tab.getHoverText(), 
                     tabX, layout.startY, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY, active, position, isFirstTab);
        }
    }

    /**
     * Renders a single tab with the given parameters
     */
    private static void renderTab(GuiGraphics guiGraphics, ItemStack icon, net.minecraft.network.chat.Component hoverText, 
                                 int x, int y, int width, int height, double mouseX, double mouseY, 
                                 boolean active, TabConfig.TabPosition position, boolean isFirstTab) {
        
        TabConfig.SpriteCoords coords = TabConfig.getSpriteCoords(position, active, isFirstTab);
        
        int renderHeight = TAB_TEXTURE_HEIGHT;
        int yOffset = 0;
        int vOffset = 0;

        if (!active) {
            if (position == TabConfig.TabPosition.TOP_LEFT || position == TabConfig.TabPosition.TOP_RIGHT) {
                renderHeight = 28;
            } else if (position == TabConfig.TabPosition.BOTTOM_LEFT || position == TabConfig.TabPosition.BOTTOM_RIGHT) {
                renderHeight = 28;
                yOffset = 4;
                vOffset = 4;
            }
        }

        guiGraphics.blit(CREATIVE_INVENTORY_TABS, x, y + yOffset, coords.u, coords.v + vOffset, TAB_TEXTURE_WIDTH, renderHeight);

        int itemPadding = Math.max(0, (width - 16) / 2);
        int itemX = x + itemPadding - 1;
        int itemY = y + itemPadding + 1; // Adjusted to center the icon
        guiGraphics.renderItem(icon, itemX, itemY);

        if (mouseX >= x && mouseX < x + width && mouseY >= y + yOffset && mouseY < y + yOffset + renderHeight) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, hoverText, (int) mouseX, (int) mouseY);
        }
    }

    /**
     * Handles tab click detection and opening
     */
    public static boolean handleTabClick(double mouseX, double mouseY, int button, AbstractContainerScreen<?> currentScreen) {
        if (currentScreen == null || button != 0) return false;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        // If there's a stored block and it's no longer valid, don't allow any tab clicks
        if (InventoryTabManager.getStoredBlockPos() != null && !InventoryTabManager.isBlockStillValid(InventoryTabManager.getStoredBlockPos())) {
            return false;
        }
        
        java.util.List<InventoryTab> activeTabs = InventoryTabManager.getActiveTabs();
        if (activeTabs.isEmpty()) {
            return false;
        }
        
        TabConfig.TabPosition position = TabConfig.getTabPosition();
        
        int screenLeft = currentScreen.getGuiLeft();
        int screenTop = currentScreen.getGuiTop();
        int screenWidth = currentScreen.getXSize();
        int screenHeight = currentScreen.getYSize();
        
        TabPositionCalculator.TabLayout layout = TabPositionCalculator.calculateLayout(
            position, screenLeft, screenTop, screenWidth, screenHeight, TAB_WIDTH, TAB_HEIGHT
        );
        
        for (int i = 0; i < activeTabs.size(); i++) {
            InventoryTab tab = activeTabs.get(i);
            int tabX = layout.getTabX(i, TAB_SPACING);
            
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= layout.startY && mouseY <= layout.startY + TAB_HEIGHT) {
                Level world = player.level();
                AbstractContainerMenu handler = player.containerMenu;
                MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
                
                tab.open(player, world, handler, gameMode);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets the currently equipped backpack tab using Sophisticated Backpacks' PlayerInventoryProvider
     */
    public static BackpackTab getEquippedBackpackTab(Player player) {
        try {
            BackpackTab[] foundTab = {null};
            
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
                if ("main".equals(inventoryName)) {
                    return false;
                }
                foundTab[0] = new BackpackTab(backpack);
                return true;
            });
            
            return foundTab[0];
        } catch (Exception e) {
            System.err.println("Error getting equipped backpack tab: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
