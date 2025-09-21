package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.List;

import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;
import com.yukami.backpacktab.client.config.TabConfig;

/**
 * Simplified tab manager that handles all tab logic in one place.
 * Ported from optimized 1.20.1 version to 1.21.1 NeoForge.
 */
@EventBusSubscriber(modid = "yukamibackpacktab", value = Dist.CLIENT)
public class TabManager {
    
    // State variables
    private static AbstractContainerScreen<?> currentScreen = null;
    private static BlockPos storedBlockPos = null;
    private static Block storedBlockType = null;
    private static boolean isTabSwitching = false;
    private static final List<InventoryTab> activeTabs = new ArrayList<>();
    
    // Config caching
    private static List<? extends String> cachedAdditionalTabBlocks = null;
    
    /**
     * Gets all available tabs for the current context.
     * Core logic: Always 2 tabs maximum - either Player+Backpack or Container+Backpack
     */
    public static List<InventoryTab> getAvailableTabs(Player player, AbstractContainerScreen<?> screen) {
        List<InventoryTab> tabs = new ArrayList<>();
        
        // Get equipped backpack - if none, show no tabs
        BackpackTab equippedBackpack = getEquippedBackpackTab(player);
        if (equippedBackpack == null) {
            return tabs; // Critical rule: no equipped backpack = no tabs at all
        }
        
        // Special case: viewing backpack block while having equipped backpack
        if (storedBlockPos != null && isBackpackBlock(player.level(), storedBlockPos)) {
            tabs.add(new ContainerTab(storedBlockPos)); // Block backpack first
            tabs.add(equippedBackpack);                  // Equipped backpack second
            return tabs; // No player tab in this case
        }
        
        // Normal case: current screen tab + equipped backpack
        if (screen instanceof InventoryScreen) {
            tabs.add(new PlayerTab());
        } else if (storedBlockPos != null) {
            tabs.add(new ContainerTab(storedBlockPos));
        } else {
            tabs.add(new PlayerTab()); // Fallback
        }
        
        tabs.add(equippedBackpack);
        return tabs;
    }
    
    /**
     * Sets which tab should be active based on current screen.
     */
    public static void updateActiveStates(List<InventoryTab> tabs, AbstractContainerScreen<?> screen) {
        tabs.forEach(tab -> tab.setActive(tab.matchesCurrentScreen(screen)));
    }
    
    /**
     * Gets equipped backpack using Sophisticated Backpacks API.
     */
    private static BackpackTab getEquippedBackpackTab(Player player) {
        try {
            BackpackTab[] foundTab = {null};
            
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
                if ("main".equals(inventoryName)) {
                    return false; // Skip main inventory
                }
                foundTab[0] = new BackpackTab(backpack);
                return true; // Found equipped backpack
            });
            
            return foundTab[0];
        } catch (Exception e) {
            // Silently handle equipped backpack errors
            return null;
        }
    }
    
    /**
     * Validates that stored block is still accessible.
     */
    public static boolean isStoredBlockValid() {
        if (storedBlockPos == null || storedBlockType == null) {
            return false;
        }
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        
        Level world = player.level();
        if (world == null) return false;
        
        // Check block type unchanged
        BlockState blockState = world.getBlockState(storedBlockPos);
        if (blockState.getBlock() != storedBlockType) {
            return false;
        }
        
        // Check distance (8 blocks)
        double distanceSq = player.distanceToSqr(storedBlockPos.getX() + 0.5, storedBlockPos.getY() + 0.5, storedBlockPos.getZ() + 0.5);
        return distanceSq <= 64.0;
    }
    
    /**
     * Checks if block is a container (backpack block, menu provider, or configured block).
     */
    private static boolean isContainerBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;
        
        BlockState blockState = world.getBlockState(pos);
        
        // BackpackBlock
        if (blockState.getBlock() instanceof BackpackBlock) {
            return true;
        }
        
        // Additional configured blocks
        if (isAdditionalTabBlock(blockState)) {
            return true;
        }
        
        // Regular container blocks
        try {
            return blockState.getMenuProvider(world, pos) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if block is specifically a backpack block.
     */
    private static boolean isBackpackBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;
        BlockState blockState = world.getBlockState(pos);
        return blockState.getBlock() instanceof BackpackBlock;
    }
    
    /**
     * Checks if block is in additional configured blocks list.
     */
    private static boolean isAdditionalTabBlock(BlockState blockState) {
        try {
            // Use NeoForge registry access (like 1.21.8)
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            String blockIdString = blockId.toString();
            
            for (String configuredBlock : getAdditionalTabBlocks()) {
                if (configuredBlock != null && configuredBlock.trim().equals(blockIdString)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets additional tab blocks from config with caching.
     */
    private static List<? extends String> getAdditionalTabBlocks() {
        if (cachedAdditionalTabBlocks == null) {
            cachedAdditionalTabBlocks = TabConfig.getAdditionalTabBlocks();
        }
        return cachedAdditionalTabBlocks;
    }
    
    /**
     * Handles invalid block cleanup.
     */
    private static void handleInvalidBlock() {
        if (currentScreen instanceof IBackpackScreen) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.closeContainer();
            }
        }
        resetState();
    }
    
    /**
     * Resets all manager state.
     */
    private static void resetState() {
        storedBlockPos = null;
        storedBlockType = null;
        currentScreen = null;
        isTabSwitching = false;
        activeTabs.clear();
        CarriedItemUtil.reset();
        TabRenderer.invalidateCache();
    }
    
    // Event Handlers
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            BlockPos clickedPos = event.getPos();
            Level world = event.getLevel();
            
            if (isContainerBlock(world, clickedPos)) {
                storedBlockPos = clickedPos;
                storedBlockType = world.getBlockState(clickedPos).getBlock();
            }
        }
    }
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        currentScreen = containerScreen;
        
        // Only clear tabs if NOT tab switching (preserve context during switches)
        if (!isTabSwitching) {
            activeTabs.clear();
        }
        
        // Get available tabs
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // Only rebuild tabs if not tab switching (preserve context)
            if (!isTabSwitching) {
                activeTabs.clear();
                activeTabs.addAll(getAvailableTabs(player, containerScreen));
                // Only update active states if not tab switching (preserve user selection)
                updateActiveStates(activeTabs, containerScreen);
            }
            // If tab switching, preserve the active states set by the click handler
        }
        
        isTabSwitching = false;
        
        // Restore carried items
        if (player instanceof LocalPlayer localPlayer) {
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) {
                TabSwitcher.restoreCarriedItems(localPlayer, gameMode);
            }
        }
    }
    
    @SubscribeEvent
    public static void onScreenRender(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        if (containerScreen != currentScreen && !(containerScreen instanceof InventoryScreen)) return;
        
        // Validate stored block
        if (storedBlockPos != null && !isStoredBlockValid()) {
            handleInvalidBlock();
            return;
        }
        
        TabRenderer.renderTabs(event.getGuiGraphics(), containerScreen, event.getMouseX(), event.getMouseY());
    }
    
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen != currentScreen) return;
        
        isTabSwitching = true;
        if (TabRenderer.handleTabClick(event.getMouseX(), event.getMouseY(), event.getButton(), containerScreen)) {
            event.setCanceled(true);
        } else {
            isTabSwitching = false;
        }
    }
    
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() == currentScreen) {
            if (!isTabSwitching) {
                resetState();
                // Handle carried item cleanup
                if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                    Player player = Minecraft.getInstance().player;
                    if (player != null && screen instanceof InventoryScreen && 
                        CarriedItemUtil.isStashed() && player.containerMenu.getCarried().isEmpty()) {
                        player.containerMenu.setCarried(ItemStack.EMPTY);
                    }
                }
                CarriedItemUtil.reset();
            }
            currentScreen = null;
        }
    }
    
    // Public getters for compatibility
    
    public static List<InventoryTab> getActiveTabs() {
        return new ArrayList<>(activeTabs); // Defensive copy
    }
    
    public static BlockPos getStoredBlockPos() {
        return storedBlockPos;
    }
    
    public static Block getStoredBlockType() {
        return storedBlockType;
    }
    
    public static void reset() {
        resetState();
    }
}