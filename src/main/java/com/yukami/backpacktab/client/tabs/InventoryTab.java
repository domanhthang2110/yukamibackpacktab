package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface InventoryTab {
    ItemStack getTabIcon();

    Component getHoverText();

    void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);

    boolean matchesCurrentScreen(AbstractContainerScreen<?> screen);

    boolean isActive();

    void setActive(boolean active);
}
