package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.client.util.CarriedItemUtil;

import static com.yukami.backpacktab.YukamiBackpackTab.LOGGER;

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
    private boolean active = false;
    
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
            LOGGER.error("Error opening backpack: {}", e.getMessage());
        }
    }
    
    @Override
    public void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        // Backpack closing is handled by the server
    }
    
    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof IBackpackScreen)) {
            return false;
        }
        
        // Check if we're in a block context - if so, this equipped backpack tab should NOT be active
        // The block's ContainerTab should be active instead
        net.minecraft.core.BlockPos storedPos = com.yukami.backpacktab.client.gui.InventoryTabManager.getStoredBlockPos();
        if (storedPos != null) {
            net.minecraft.world.level.Level world = net.minecraft.client.Minecraft.getInstance().level;
            if (world != null) {
                net.minecraft.world.level.block.state.BlockState blockState = world.getBlockState(storedPos);
                if (blockState.getBlock() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock) {
                    // We're viewing a backpack block, so this equipped backpack tab should NOT be active
                    return false;
                }
            }
        }
        
        // No block context, so this equipped backpack tab should be active
        return true;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
}
