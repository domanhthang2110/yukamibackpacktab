package com.yukami.backpacktab.client.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * Utility for temporarily stashing and restoring carried items during inventory operations,
 * such as tab switches, to prevent item loss.
 */
public class CarriedItemUtil {
    private static int stashedSlotIndex = -1; // Stores the inventory slot index where the item was stashed

    /**
     * Stashes the item currently held on the player's cursor into an empty inventory slot.
     * This is typically called before an action that might clear the cursor.
     *
     * @param player The local player.
     * @param gameMode The player's interaction manager.
     * @param containerMenu The currently open container menu.
     */
    public static void stashCarriedItem(LocalPlayer player, MultiPlayerGameMode gameMode, AbstractContainerMenu containerMenu) {
        if (!containerMenu.getCarried().isEmpty()) {
            int emptyInventorySlot = player.getInventory().getFreeSlot();

            if (emptyInventorySlot != -1) {
                int screenSlotIndex = findScreenSlotIndex(player, containerMenu, emptyInventorySlot);
                
                if (screenSlotIndex != -1) {
                    gameMode.handleInventoryMouseClick(
                            containerMenu.containerId,
                            screenSlotIndex,
                            0,
                            ClickType.PICKUP,
                            player
                    );
                    stashedSlotIndex = emptyInventorySlot;
                    // Item stashed successfully
                }
            }
        }
    }
    
    /**
     * Attempts to restore a stashed item back to the player's cursor.
     * This is typically called after an action that might have cleared the cursor.
     *
     * @param player The local player.
     * @param gameMode The player's interaction manager.
     * @param containerMenu The currently open container menu.
     */
    public static void unstashCarriedItem(LocalPlayer player, MultiPlayerGameMode gameMode, AbstractContainerMenu containerMenu) {
        if (stashedSlotIndex != -1) {
            int screenSlotIndex = findScreenSlotIndex(player, containerMenu, stashedSlotIndex);
            
            if (screenSlotIndex != -1) {
                gameMode.handleInventoryMouseClick(
                        containerMenu.containerId,
                        screenSlotIndex,
                        0,
                        ClickType.PICKUP,
                        player
                );
                // Item unstashed successfully
            }
            stashedSlotIndex = -1;
        }
    }
    
    /**
     * Resets the utility's internal state, forgetting any stashed item.
     * This should be called when the inventory context changes significantly (e.g., screen closes).
     */
    public static void reset() {
        // Reset stashed item state
        stashedSlotIndex = -1;
    }

    /**
     * Checks if an item is currently stashed.
     * @return True if an item is stashed, false otherwise.
     */
    public static boolean isStashed() {
        return stashedSlotIndex != -1;
    }

    /**
     * Helper method to find the screen slot index for a given player inventory slot index.
     * @param player The local player.
     * @param containerMenu The currently open container menu.
     * @param inventorySlotIndex The index of the slot in the player's inventory.
     * @return The corresponding screen slot index, or -1 if not found.
     */
    private static int findScreenSlotIndex(LocalPlayer player, AbstractContainerMenu containerMenu, int inventorySlotIndex) {
        for (int i = 0; i < containerMenu.slots.size(); i++) {
            Slot slot = containerMenu.slots.get(i);
            // Check if the slot belongs to the player's inventory and matches the inventory slot index
            if (slot.container == player.getInventory() && slot.getSlotIndex() == inventorySlotIndex) {
                return i;
            }
        }
        return -1;
    }
}
