package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;

public final class TabSwitcher {
    private TabSwitcher() {}

    public static void switchToTab(InventoryTab targetTab, Player player, MultiPlayerGameMode gameMode) {
        if (!(player instanceof LocalPlayer localPlayer) || gameMode == null) return;

        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                && targetTab.matchesCurrentScreen(screen)) return;

        if (targetTab instanceof BackpackTab backpackTab) {
            var refreshedTarget = backpackTab.resolveCurrentLocation(player);
            if (refreshedTarget.isEmpty()) {
                TabManager.refreshTabsForCurrentScreen(player);
                return;
            }

            InventoryTab resolvedTarget = refreshedTarget.get();
            if (resolvedTarget != targetTab) {
                TabManager.replaceActiveTab(targetTab, resolvedTarget);
                targetTab = resolvedTarget;
            }
        }

        if (!TabManager.beginTabSwitch(targetTab, localPlayer.containerMenu)) return;

        prepareContainerSwitch(localPlayer, gameMode, targetTab instanceof ContainerTab);

        targetTab.open(player, player.level(), player.containerMenu, gameMode);
    }

    private static void prepareContainerSwitch(
            LocalPlayer localPlayer,
            MultiPlayerGameMode gameMode,
            boolean preserveMainHand
    ) {
        AbstractContainerMenu containerMenu = localPlayer.containerMenu;
        if (containerMenu == null) {
            return;
        }

        int excludedSlot = preserveMainHand ? localPlayer.getInventory().selected : -1;
        CarriedItemUtil.stashCarriedItem(localPlayer, gameMode, containerMenu, excludedSlot);

        if (!(containerMenu instanceof InventoryMenu) && localPlayer.connection != null) {
            localPlayer.connection.send(new ServerboundContainerClosePacket(containerMenu.containerId));
        }
    }

    public static void restoreCarriedItems(LocalPlayer localPlayer, MultiPlayerGameMode gameMode) {
        if (gameMode != null && localPlayer.containerMenu != null) {
            CarriedItemUtil.unstashCarriedItem(localPlayer, gameMode, localPlayer.containerMenu);
        }
    }
}
