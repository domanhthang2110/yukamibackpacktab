package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Interface representing an inventory tab that can be displayed in the GUI.
 */
public interface InventoryTab {
    
    /**
     * Gets the item stack to display as the tab icon.
     * @return The item stack for the tab icon.
     */
    ItemStack getTabIcon();
    
    /**
     * Gets the text to display when hovering over the tab.
     * @return The hover text component.
     */
    Component getHoverText();
    
    /**
     * Called when the tab is clicked to open.
     * @param player The player clicking the tab.
     * @param world The current world.
     * @param handler The current container menu.
     * @param gameMode The player's game mode manager.
     */
    void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);
    
    /**
     * Called when the tab is closed.
     * @param player The player.
     * @param world The current world.
     * @param handler The current container menu.
     * @param gameMode The player's game mode manager.
     */
    void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);
    
    /**
     * Checks if this tab matches the currently open screen.
     * @param screen The currently open container screen.
     * @return True if this tab should be considered active for the given screen.
     */
    boolean matchesCurrentScreen(AbstractContainerScreen<?> screen);
    
    /**
     * Gets whether this tab is currently active.
     * @return True if the tab is active.
     */
    boolean isActive();
    
    /**
     * Sets whether this tab is currently active.
     * @param active The active state to set.
     */
    void setActive(boolean active);
    
    /**
     * Gets whether this tab should open instantly without packet delays.
     * @return True if the tab should open instantly, false otherwise.
     */
    default boolean isInstant() {
        return false;
    }
}