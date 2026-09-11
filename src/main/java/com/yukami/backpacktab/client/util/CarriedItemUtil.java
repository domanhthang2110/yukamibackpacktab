package com.yukami.backpacktab.client.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CarriedItemUtil {
    private static int stashedSlotIndex = -1;
    private static ItemStack stashedStack = ItemStack.EMPTY;

    private CarriedItemUtil() {}

    public static void stashCarriedItem(
            LocalPlayer player,
            MultiPlayerGameMode gameMode,
            AbstractContainerMenu containerMenu,
            int excludedInventorySlot
    ) {
        if (!containerMenu.getCarried().isEmpty()) {
            int emptyInventorySlot = findFreeSlot(player, excludedInventorySlot);

            if (emptyInventorySlot != -1) {
                int screenSlotIndex = findScreenSlotIndex(player, containerMenu, emptyInventorySlot);

                if (screenSlotIndex != -1) {
                    ItemStack carriedBeforeClick = containerMenu.getCarried().copy();

                    gameMode.handleContainerInput(
                            containerMenu.containerId,
                            screenSlotIndex,
                            0,
                            ContainerInput.PICKUP,
                            player
                    );
                    Slot destination = containerMenu.slots.get(screenSlotIndex);

                    if (containerMenu.getCarried().isEmpty()
                            && ItemStack.matches(destination.getItem(), carriedBeforeClick)) {
                        stashedSlotIndex = emptyInventorySlot;
                        stashedStack = carriedBeforeClick;
                    }
                }
            }
        }
    }

    public static void unstashCarriedItem(LocalPlayer player, MultiPlayerGameMode gameMode, AbstractContainerMenu containerMenu) {
        if (stashedSlotIndex != -1) {
            int screenSlotIndex = findScreenSlotIndex(player, containerMenu, stashedSlotIndex);

            if (screenSlotIndex != -1) {
                Slot source = containerMenu.slots.get(screenSlotIndex);

                if (containerMenu.getCarried().isEmpty() && ItemStack.matches(source.getItem(), stashedStack)) {
                    gameMode.handleContainerInput(
                            containerMenu.containerId,
                            screenSlotIndex,
                            0,
                            ContainerInput.PICKUP,
                            player
                    );
                }
            }
            stashedSlotIndex = -1;
            stashedStack = ItemStack.EMPTY;
        }
    }

    public static void reset() {
        stashedSlotIndex = -1;
        stashedStack = ItemStack.EMPTY;
    }

    private static int findScreenSlotIndex(LocalPlayer player, AbstractContainerMenu containerMenu, int inventorySlotIndex) {
        for (int i = 0; i < containerMenu.slots.size(); i++) {
            Slot slot = containerMenu.slots.get(i);

            if (slot.container == player.getInventory() && slot.getSlotIndex() == inventorySlotIndex) {
                return i;
            }
        }
        return -1;
    }

    private static int findFreeSlot(LocalPlayer player, int excludedInventorySlot) {
        int inventorySize = player.getInventory().getNonEquipmentItems().size();
        for (int slot = 0; slot < inventorySize; slot++) {
            if (slot != excludedInventorySlot && player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }
}
