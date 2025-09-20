package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.List;

import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;

import static com.yukami.backpacktab.YukamiBackpackTab.LOGGER;

/**
 * Provides appropriate tabs for any given game context.
 * Handles tab creation, validation, and organization.
 */
public class TabProvider {

    /**
     * Gets the tab that matches the current screen.
     * This determines which tab should be "active" (highlighted).
     */
    public static InventoryTab getCurrentTab(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) {
            return new PlayerTab();
        }
        
        // For container screens, try to match with stored block position
        BlockPos storedPos = InventoryTabManager.getStoredBlockPos();
        if (storedPos != null) {
            return new ContainerTab(storedPos);
        }
        
        // Fallback to player tab
        return new PlayerTab();
    }

    /**
     * Gets all available tabs for the current context.
     * Only shows tabs if there are at least 2 (player + something else).
     */
    public static List<InventoryTab> getAvailableTabs(Player player) {
        List<InventoryTab> tabs = new ArrayList<>();
        
        // Get equipped backpack tab
        BackpackTab equippedBackpackTab = getEquippedBackpackTab(player);
        if (equippedBackpackTab == null) {
            return tabs; // No backpack = no tabs to show
        }
        
        // Always add player tab first
        tabs.add(new PlayerTab());
        
        // Add equipped backpack tab
        tabs.add(equippedBackpackTab);
        
        return tabs;
    }

    /**
     * Gets all available tabs for a specific screen context.
     * Handles special cases like backpack blocks.
     */
    public static List<InventoryTab> getAvailableTabsForScreen(Player player, AbstractContainerScreen<?> screen, BlockPos storedBlockPos) {
        List<InventoryTab> tabs = new ArrayList<>();
        
        // Get equipped backpack tab
        BackpackTab equippedBackpackTab = getEquippedBackpackTab(player);
        if (equippedBackpackTab == null) {
            return tabs; // No backpack = no tabs to show
        }
        
        // Special case: Player opens backpack block while having equipped backpack
        // Show block tab first, equipped backpack second, no player tab
        if (storedBlockPos != null && isBackpackBlock(player.level(), storedBlockPos)) {
            tabs.add(new ContainerTab(storedBlockPos)); // Block backpack tab first
            tabs.add(equippedBackpackTab); // Equipped backpack tab second
            return tabs; // Don't add player tab in this case
        }
        
        // Normal case: Add current screen tab (player or container)
        InventoryTab currentTab = getCurrentTab(screen);
        tabs.add(currentTab);
        
        // Add equipped backpack tab
        tabs.add(equippedBackpackTab);
        
        return tabs;
    }

    /**
     * Gets the currently equipped backpack tab using Sophisticated Backpacks' PlayerInventoryProvider.
     */
    public static BackpackTab getEquippedBackpackTab(Player player) {
        try {
            BackpackTab[] foundTab = {null};
            
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
                if ("main".equals(inventoryName)) {
                    return false;
                }
                foundTab[0] = new BackpackTab(backpack);
                return true;
            });
            
            return foundTab[0];
        } catch (Exception e) {
            LOGGER.error("Error getting equipped backpack tab: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Checks if the block at the stored position is still the same block type as originally clicked
     * and the player is still within interaction range.
     */
    public static boolean isBlockStillValid(BlockPos pos, net.minecraft.world.level.block.Block originalBlockType) {
        if (pos == null || originalBlockType == null) {
            return false;
        }
        
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        
        Level world = player.level();
        if (world == null) {
            return false;
        }
        
        // Check if the block is still the same type as originally clicked
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() != originalBlockType) {
            return false;
        }
        
        // Check distance (vanilla uses 8 blocks for most containers, squared is 64)
        double distanceSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return distanceSq <= 64.0; // 8 blocks squared
    }

    /**
     * Checks if the given block position corresponds to a backpack block.
     */
    private static boolean isBackpackBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        return blockState.getBlock() instanceof BackpackBlock;
    }
}