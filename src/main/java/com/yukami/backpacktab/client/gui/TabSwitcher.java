package com.yukami.backpacktab.client.gui;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;

/**
 * Handles the technical aspects of switching between tabs.
 * Manages container closing, carried items, and tab opening coordination.
 */
public class TabSwitcher {

    /**
     * Switches to the specified tab, handling all necessary container management.
     * This includes stashing carried items, closing containers, and opening the new tab.
     */
    public static void switchToTab(InventoryTab targetTab, Player player, MultiPlayerGameMode gameMode) {
        if (!(player instanceof LocalPlayer localPlayer)) return;
        if (gameMode == null) return;

        // Step 1: Prepare for container switch
        prepareContainerSwitch(localPlayer, gameMode);

        // Step 2: Open the target tab
        targetTab.open(player, player.level(), player.containerMenu, gameMode);
    }

    /**
     * Prepares for a container switch by handling carried items and closing current container.
     */
    private static void prepareContainerSwitch(LocalPlayer localPlayer, MultiPlayerGameMode gameMode) {
        // Stash carried item before switching
        CarriedItemUtil.stashCarriedItem(localPlayer, gameMode, localPlayer.containerMenu);
        
        // Send close packet for current container like inventory tabs does
        if (localPlayer.connection != null) {
            localPlayer.connection.send(new ServerboundContainerClosePacket(localPlayer.containerMenu.containerId));
        }
    }

    /**
     * Restores carried items after a screen has opened.
     * Called by InventoryTabManager after screen initialization.
     */
    public static void restoreCarriedItems(LocalPlayer localPlayer, MultiPlayerGameMode gameMode) {
        if (gameMode != null && localPlayer.containerMenu != null) {
            CarriedItemUtil.unstashCarriedItem(localPlayer, gameMode, localPlayer.containerMenu);
        }
    }
}