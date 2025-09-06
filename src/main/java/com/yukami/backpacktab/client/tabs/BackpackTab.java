package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.client.util.CarriedItemUtil;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public class BackpackTab implements InventoryTab {
    
    private final ItemStack backpackStack;
    
    public BackpackTab(ItemStack backpackStack) {
        this.backpackStack = backpackStack;
    }
    
    @Override
    public ItemStack getTabIcon() {
        return backpackStack;
    }
    
    @Override
    public Component getHoverText() {
        return backpackStack.getHoverName();
    }
    
    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null || !(player instanceof LocalPlayer localPlayer)) return;
        
        try {
            // Stash carried item in inventory slot before operations
            CarriedItemUtil.stashCarriedItem(localPlayer, gameMode, handler);
            
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
                if (ItemStack.isSameItem(backpack, backpackStack)) {
                    SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage(slot, identifier, inventoryName));
                    return true; // Stop searching
                }
                return false; // Continue searching
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        // Backpack closing is handled by the server
    }
    
    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        return screen instanceof IBackpackScreen;
    }
}
