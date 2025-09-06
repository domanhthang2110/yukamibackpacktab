package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface InventoryTab {
    
    /**
     * Gets the item stack to display as the tab icon
     */
    ItemStack getTabIcon();
    
    /**
     * Gets the hover text for the tab
     */
    Component getHoverText();
    
    /**
     * Opens the inventory associated with this tab
     */
    void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);
    
    /**
     * Closes the inventory associated with this tab
     */
    void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);
    
    /**
     * Checks if this tab matches the currently open screen
     */
    boolean matchesCurrentScreen(AbstractContainerScreen<?> screen);

    /**
     * Returns true if this tab is currently active
     */
    boolean isActive();
    
    /**
     * Sets the active state of this tab
     */
    void setActive(boolean active);

    /**
     * Returns true if this tab represents an "instant" client-side screen (like the player inventory)
     * that doesn't require server interaction to open.
     */
    default boolean isInstant() {
        return false;
    }
}
