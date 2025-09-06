package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.client.util.CarriedItemUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;

public class ContainerTab implements InventoryTab {
    
    private final BlockPos containerPos;
    private boolean active = false;
        
    public ContainerTab(BlockPos containerPos) {
        this.containerPos = containerPos;
    }
    
    @Override
    public ItemStack getTabIcon() {
        if (containerPos != null) {
            Level world = Minecraft.getInstance().level;
            if (world != null) {
                BlockState blockState = world.getBlockState(containerPos);
                
                // Special handling for BackpackBlock - get the actual backpack item from block entity
                if (blockState.getBlock() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock) {
                    try {
                        var blockEntity = world.getBlockEntity(containerPos);
                        if (blockEntity instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity backpackBE) {
                            ItemStack backpackStack = backpackBE.getBackpackWrapper().getBackpack();
                            if (!backpackStack.isEmpty()) {
                                return backpackStack;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[BackpackTab] Error getting backpack from block entity: " + e.getMessage());
                    }
                }
                
                // Fallback to block item
                ItemStack icon = new ItemStack(blockState.getBlock());
                return icon;
            }
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public Component getHoverText() {
        ItemStack icon = getTabIcon();
        return icon.isEmpty() ? Component.literal("Container") : icon.getHoverName();
    }
    
    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null || !(player instanceof LocalPlayer localPlayer)) return;
        
        try {
            if (containerPos != null) {
                // Step 1: Stash carried item in inventory slot before closing container
                CarriedItemUtil.stashCarriedItem(localPlayer, gameMode, handler);
                
                // Step 2: Inform the server that the current container is being closed.
                // This is necessary to prevent desynchronization issues.
                if (handler != null && localPlayer.connection != null) {
                    localPlayer.connection.send(new ServerboundContainerClosePacket(handler.containerId));
                }
                
                // Step 3: Simulate a right-click on the target block to open its GUI.
                // We create a "hit result" to specify which block we are "clicking".
                Vec3 hitVec = Vec3.atCenterOf(containerPos);
                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, containerPos, false);
                
                // The gameMode's useItemOn method sends the interaction packet to the server.
                gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, hitResult);
                
            } else {
                // If the container position is null, close the screen.
                Minecraft.getInstance().setScreen(null);
            }
            
        } catch (Exception e) {
            Minecraft.getInstance().setScreen(null);
        }
    }
    
    @Override
    public void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        // Container closing is handled by the server
    }
    
    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        if (containerPos == null) {
            return false;
        }
        
        // Check if this is a backpack block
        Level world = Minecraft.getInstance().level;
        if (world != null) {
            BlockState blockState = world.getBlockState(containerPos);
            if (blockState.getBlock() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock) {
                // For backpack blocks, match if the screen is a backpack screen
                return screen instanceof IBackpackScreen;
            }
        }
        
        // For regular container blocks, match if it's not a backpack screen
        return !(screen instanceof IBackpackScreen);
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
