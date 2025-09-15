package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.util.TabPositionCalculator;

import static com.yukami.backpacktab.YukamiBackpackTab.LOGGER;

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
import net.minecraft.client.renderer.RenderPipelines;

public class TabRenderer {

    // Individual tab sprites for 1.21.8
    private static final int TAB_TEXTURE_WIDTH = 26;
    private static final int TAB_TEXTURE_HEIGHT = 32;
    
    private static ResourceLocation getTabSprite(TabConfig.TabPosition position, boolean active, boolean isFirstTab) {
        String state = active ? "selected" : "unselected";
        String positionName;
        String tabNumber;
        
        switch (position) {
            case TOP_LEFT -> {
                positionName = "top";
                tabNumber = isFirstTab ? "1" : "2";  // 1 for first, 2 for any middle
            }
            case TOP_RIGHT -> {
                positionName = "top";
                tabNumber = isFirstTab ? "7" : "2";  // 7 for first right, 2 for any middle
            }
            case BOTTOM_LEFT -> {
                positionName = "bottom";
                tabNumber = isFirstTab ? "1" : "2";  // 1 for first, 2 for any middle
            }
            case BOTTOM_RIGHT -> {
                positionName = "bottom";
                tabNumber = isFirstTab ? "7" : "2";  // 7 for first right, 2 for any middle
            }
            default -> {
                positionName = "top";
                tabNumber = "1";
            }
        }
        
        return ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_" + positionName + "_" + state + "_" + tabNumber);
    }

    // Define constants for tab dimensions
    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 32;
    public static final int TAB_SPACING = TAB_WIDTH - 2; // Keep current spacing

    /**
     * Calculates the tab layout for the given screen.
     */
    private static TabPositionCalculator.TabLayout getTabLayout(AbstractContainerScreen<?> screen) {
        TabConfig.TabPosition position = TabConfig.getTabPosition();
        int screenWidth = screen.getXSize();
        int screenHeight = screen.getYSize();
        return TabPositionCalculator.calculateLayout(
            position, 0, 0, screenWidth, screenHeight, TAB_WIDTH, TAB_HEIGHT
        );
    }

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
        
        TabPositionCalculator.TabLayout layout = TabPositionCalculator.calculateLayout(
            position, 0, 0, screen.getXSize(), screen.getYSize(), TAB_WIDTH, TAB_HEIGHT
        );
        
        for (int i = 0; i < activeTabs.size(); i++) {
            InventoryTab tab = activeTabs.get(i);
            int tabX = layout.getTabX(i, TAB_SPACING);
            boolean active = tab.isActive();
            boolean isFirstTab = (i == 0);
            
            
            renderTab(guiGraphics, tab.getTabIcon(), tab.getHoverText(), 
                     tabX, layout.startY, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY, active, position, isFirstTab);
        }
        
        // Render tooltips after all tabs to ensure proper Z-ordering
        renderTabTooltips(guiGraphics, screen, mouseX, mouseY);
    }

    /**
     * Renders a single tab with the given parameters
     */
    private static void renderTab(GuiGraphics guiGraphics, ItemStack icon, net.minecraft.network.chat.Component hoverText, 
                                 int x, int y, int width, int height, double mouseX, double mouseY, 
                                 boolean active, TabConfig.TabPosition position, boolean isFirstTab) {
        
        // Get the appropriate sprite for this tab
        ResourceLocation tabSprite = getTabSprite(position, active, isFirstTab);
        
        int renderHeight = TAB_TEXTURE_HEIGHT;
        int yOffset = 0;

        if (!active) {
            
            if (position == TabConfig.TabPosition.TOP_LEFT || position == TabConfig.TabPosition.TOP_RIGHT) {
                yOffset = 1; // Shift up by 2px
                renderHeight = 28; // Reintroduce clipping for inactive tabs
            } else if (position == TabConfig.TabPosition.BOTTOM_LEFT || position == TabConfig.TabPosition.BOTTOM_RIGHT) {
                yOffset = 2; // Shift down by 2px
                renderHeight = 30; // Reintroduce clipping for inactive tabs
            }
        }

        // Debug: Log coordinates to see what we're getting
        LOGGER.info("Rendering tab at x={}, y={}, yOffset={}, final y={}", x, y, yOffset, y + yOffset);
        
        // Use the same method as Minecraft's creative inventory
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, tabSprite, x, y + yOffset, TAB_TEXTURE_WIDTH, renderHeight);
        int itemPadding = Math.max(0, (width - 16) / 2);
        int itemX = x + itemPadding - 1;
        int itemY = y + itemPadding + 1; // Adjusted to center the icon
        guiGraphics.renderItem(icon, itemX, itemY);

        // Note: Tooltip rendering moved to separate method to ensure proper Z-ordering
    }

    /**
     * Renders tooltips for tabs (called after all tabs are rendered for proper Z-ordering)
     */
    private static void renderTabTooltips(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (screen == null) return;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // If there's a stored block and it's no longer valid, don't render any tooltips
        if (InventoryTabManager.getStoredBlockPos() != null && !InventoryTabManager.isBlockStillValid(InventoryTabManager.getStoredBlockPos())) {
            return;
        }
        
        java.util.List<InventoryTab> activeTabs = InventoryTabManager.getActiveTabs();
        if (activeTabs.isEmpty()) {
            return;
        }
        
        TabConfig.TabPosition position = TabConfig.getTabPosition();
        
        TabPositionCalculator.TabLayout layout = TabPositionCalculator.calculateLayout(
            position, screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize(), TAB_WIDTH, TAB_HEIGHT
        );
        
        for (int i = 0; i < activeTabs.size(); i++) {
            InventoryTab tab = activeTabs.get(i);
            int tabX = layout.getTabX(i, TAB_SPACING);
            boolean active = tab.isActive();
            
            int renderHeight = TAB_TEXTURE_HEIGHT;
            int yOffset = 0;

            if (!active) {
                if (position == TabConfig.TabPosition.TOP_LEFT || position == TabConfig.TabPosition.TOP_RIGHT) {
                    renderHeight = 28;
                } else if (position == TabConfig.TabPosition.BOTTOM_LEFT || position == TabConfig.TabPosition.BOTTOM_RIGHT) {
                    renderHeight = 28;
                    yOffset = 4;
                }
            }
            
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && mouseY >= layout.startY + yOffset && mouseY < layout.startY + yOffset + renderHeight) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, java.util.List.of(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(tab.getHoverText().getVisualOrderText())), (int) (mouseX - screen.getGuiLeft()), (int) (mouseY - screen.getGuiTop()), net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
                break; // Only render one tooltip at a time
            }
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
        
        TabConfig.TabPosition position = TabConfig.getTabPosition(); // Declare position here
        TabPositionCalculator.TabLayout layout = TabPositionCalculator.calculateLayout(
            position, currentScreen.getGuiLeft(), currentScreen.getGuiTop(), currentScreen.getXSize(), currentScreen.getYSize(), TAB_WIDTH, TAB_HEIGHT
        );
        
        for (int i = 0; i < activeTabs.size(); i++) {
            InventoryTab tab = activeTabs.get(i);
            int tabX = layout.getTabX(i, TAB_SPACING);
            
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= layout.startY && mouseY <= layout.startY + TAB_HEIGHT) {
                // Set all tabs to inactive, then set clicked tab to active
                for (int j = 0; j < activeTabs.size(); j++) {
                    InventoryTab allTab = activeTabs.get(j);
                    boolean newActive = (j == i);
                    allTab.setActive(newActive);
                }
                
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
            LOGGER.error("Error getting equipped backpack tab: {}", e.getMessage());
        }
        return null;
    }
}
