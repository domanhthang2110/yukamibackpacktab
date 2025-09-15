package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;

import java.util.ArrayList;
import java.util.List;

import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;
import com.yukami.backpacktab.client.config.TabConfig;

@EventBusSubscriber(modid = "yukamibackpacktab", value = Dist.CLIENT)
public class InventoryTabManager {
    
    private static AbstractContainerScreen<?> currentScreen = null;
    private static BlockPos storedBlockPos = null; // Store block pos when block GUI opens
    private static boolean isTabSwitching = false; // Track if we're in the middle of a tab switch
    private static boolean isFromBlockClick = false; // Track if the next screen is from a block click
    private static final List<InventoryTab> activeTabs = new ArrayList<>(); // List of tabs to render

    /**
     * Checks if the given block position corresponds to a container block or backpack block.
     * @param world The world instance.
     * @param pos The block position.
     * @return True if the block is a container or backpack block, false otherwise.
     */
    private static boolean isContainerBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        try {
            // Check if it's a BackpackBlock (which doesn't implement MenuProvider)
            if (blockState.getBlock() instanceof BackpackBlock) {
                return true;
            }
            
            // Check if the block is in the additional configured blocks list
            if (isAdditionalTabBlock(blockState)) {
                return true;
            }
            
            // Check for regular container blocks
            return blockState.getMenuProvider(world, pos) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the given block position corresponds to a backpack block.
     * @param world The world instance.
     * @param pos The block position.
     * @return True if the block is a backpack block, false otherwise.
     */
    private static boolean isBackpackBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        return blockState.getBlock() instanceof BackpackBlock;
    }

    /**
     * Checks if the given block state matches any of the additional configured tab blocks.
     * @param blockState The block state to check.
     * @return True if the block is in the additional tab blocks configuration, false otherwise.
     */
    private static boolean isAdditionalTabBlock(BlockState blockState) {
        try {
            // Get the block's resource location (modID:block_name)
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            String blockIdString = blockId.toString();
            
            // Check if this block ID is in the configured additional tab blocks list
            for (String configuredBlock : TabConfig.getAdditionalTabBlocks()) {
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
     * Determines if the given screen is an instance of InventoryScreen.
     */
    private static boolean isPlayerInventoryScreen(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen;
    }

    /**
     * Updates the current screen state and clears previous context if not tab switching.
     */
    private static void updateScreenState(AbstractContainerScreen<?> screen) {
        currentScreen = screen;
        if (!isTabSwitching) {
            // Don't clear storedBlockPos and isFromBlockClick here - we need them for determineBaseTab
            activeTabs.clear(); // Clear previous tabs only if not tab switching
        }
    }

    /**
     * Determines the base tab (PlayerTab or ContainerTab) based on the current screen and stored context.
     */
    private static InventoryTab determineBaseTab(AbstractContainerScreen<?> screen) {
        if (isPlayerInventoryScreen(screen)) {
            return new PlayerTab();
        }

        // If we have a stored block position from a right-click event, use it
        if (isFromBlockClick && storedBlockPos != null) {
            return new ContainerTab(storedBlockPos);
        }

        // If we have a stored block position from previous context (tab switching), use it
        if (storedBlockPos != null) {
            return new ContainerTab(storedBlockPos);
        }

        // Default to player tab (for equipped backpacks opened via hotkey/item use)
        return new PlayerTab();
    }

    /**
     * Rebuilds the list of active tabs based on the base tab and equipped backpack.
     * Only shows tabs if there's an equipped backpack.
     */
    private static void rebuildTabList(InventoryTab baseTab) {
        // Preserve existing tab active states before clearing
        ContainerTab existingContainerTab = null;
        BackpackTab existingBackpackTab = null;
        PlayerTab existingPlayerTab = null;
        for (InventoryTab tab : activeTabs) {
            if (tab instanceof ContainerTab) {
                existingContainerTab = (ContainerTab) tab;
            } else if (tab instanceof BackpackTab) {
                existingBackpackTab = (BackpackTab) tab;
            } else if (tab instanceof PlayerTab) {
                existingPlayerTab = (PlayerTab) tab;
            }
        }
        
        activeTabs.clear(); // Clear before rebuilding to ensure correct order and prevent duplicates
        
        Player player = Minecraft.getInstance().player;
        BackpackTab equippedBackpackTab = null;
        if (player != null) {
            // Get new tab from renderer
            BackpackTab newEquippedTab = TabRenderer.getEquippedBackpackTab(player);
            
            // If we have both existing and new tab, preserve the active state
            if (existingBackpackTab != null && newEquippedTab != null) {
                newEquippedTab.setActive(existingBackpackTab.isActive());
            }
            
            equippedBackpackTab = newEquippedTab;
        }
        
        // REQUIREMENT: Only show tabs if there's an equipped backpack
        if (equippedBackpackTab == null) {
            return; // No equipped backpack = no tabs at all
        }
        
        // Preserve active state for base tab
        if (baseTab instanceof ContainerTab && existingContainerTab != null) {
            baseTab.setActive(existingContainerTab.isActive());
        } else if (baseTab instanceof PlayerTab && existingPlayerTab != null) {
            baseTab.setActive(existingPlayerTab.isActive());
        }
        
        // Special case: Player opens backpack block while having equipped backpack
        // Show block tab first, equipped backpack second, no player tab
        if (baseTab instanceof ContainerTab && storedBlockPos != null && 
            isBackpackBlock(Minecraft.getInstance().level, storedBlockPos)) {
            activeTabs.add(baseTab); // Block backpack tab first
            activeTabs.add(equippedBackpackTab); // Equipped backpack tab second
            
            // Only set active states if not tab switching (initial screen open)
            if (!isTabSwitching) {
                baseTab.setActive(true); // Block backpack tab is active
                equippedBackpackTab.setActive(false); // Equipped backpack tab is inactive
            }
            return; // Don't add player tab in this case
        }
        
        // Normal case: Add base tab if it exists
        if (baseTab != null) {
            activeTabs.add(baseTab);
            // Only set active state if not tab switching
            if (!isTabSwitching) {
                baseTab.setActive(true); // Base tab is active
            }
        }

        // Add equipped backpack tab
        activeTabs.add(equippedBackpackTab);
        // Only set active state if not tab switching
        if (!isTabSwitching) {
            equippedBackpackTab.setActive(baseTab == null); // Active only if no base tab
        }
    }


    /**
     * Resets all static state variables of the tab manager.
     */
    private static void resetState() {
        storedBlockPos = null;
        currentScreen = null;
        isTabSwitching = false;
        isFromBlockClick = false;
        activeTabs.clear(); // Clear active tabs on reset
        CarriedItemUtil.reset(); // Also reset any stashed items
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Only handle client-side events
        if (event.getLevel().isClientSide()) {
            BlockPos clickedPos = event.getPos();
            Level world = event.getLevel();
            
            if (isContainerBlock(world, clickedPos)) {
                storedBlockPos = clickedPos;
                isFromBlockClick = true;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        
        updateScreenState(containerScreen);
        
        InventoryTab baseTab = determineBaseTab(containerScreen);
        rebuildTabList(baseTab);
        
        
        // Reset flags after screen opens
        isTabSwitching = false;
        isFromBlockClick = false;
        
        // Retrieve any stashed carried item after screen opens
        Player player = Minecraft.getInstance().player;
        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
            net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) {
                CarriedItemUtil.unstashCarriedItem(localPlayer, gameMode, containerScreen.getMenu());
            }
        }
    }
    
    @SubscribeEvent
    public static void onScreenRender(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen != currentScreen && !(containerScreen instanceof InventoryScreen)) return;
        TabRenderer.renderTabs(event.getGuiGraphics(), containerScreen, event.getMouseX(), event.getMouseY()); 
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen != currentScreen) return; // Only handle clicks for the managed screen
        
        isTabSwitching = true; // Mark that we're switching tabs
        if (TabRenderer.handleTabClick(event.getMouseX(), event.getMouseY(), event.getButton(), containerScreen)) {
            event.setCanceled(true);
        } else {
            isTabSwitching = false; // Reset if no tab was clicked
        }
    }

    /**
     * Handles the cleanup of carried items to prevent ghost items.
     * This is specifically for instant screens like the player inventory.
     */
    private static void handleCarriedItemCleanup(AbstractContainerScreen<?> screen) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (screen instanceof InventoryScreen && CarriedItemUtil.isStashed() && player.containerMenu.getCarried().isEmpty()) {
                player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        CarriedItemUtil.reset(); // Always reset CarriedItemUtil state on screen close, regardless of instant or buffered
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() == currentScreen) {
            if (!isTabSwitching) {
                resetState(); // Use the centralized reset method
                handleCarriedItemCleanup((AbstractContainerScreen<?>) event.getScreen());
            }
            currentScreen = null;
        }
    }

    // Reset the tab system (called when player disconnects or changes worlds)
    public static void reset() {
        resetState(); // Use the centralized reset method
    }

    public static BlockPos getStoredBlockPos() {
        return storedBlockPos;
    }
    
    /**
     * Handles cleanup when the stored block becomes invalid.
     */
    public static void handleInvalidBlock() {
        
        if (currentScreen instanceof IBackpackScreen) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.closeContainer(); // Use Sophisticated Backpacks' official method - this handles carried items properly
            }
        }
        resetState(); // Clear all state
    }
    
    /**
     * Validates if the stored block is still accessible by the player.
     * @param pos The position of the block to validate.
     * @return True if the block is still valid and accessible, false otherwise.
     */
    public static boolean isBlockStillValid(BlockPos pos) {
        if (pos == null) {
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
        
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }
        
        // Check distance (vanilla uses 8 blocks for most containers, squared is 64)
        double distanceSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distanceSq > 64.0) {
            return false;
        }
        
        try {
            // Check if it's a BackpackBlock (which doesn't implement MenuProvider)
            if (blockState.getBlock() instanceof BackpackBlock) {
                return true; // BackpackBlocks are always valid if they exist
            }
            
            // Check if the block is in the additional configured blocks list
            if (isAdditionalTabBlock(blockState)) {
                return true; // Additional configured blocks are always valid if they exist
            }
            
            // Check for regular container blocks
            return blockState.getMenuProvider(world, pos) != null;
        } catch (Exception e) {
            return false; // If any error occurs, consider it invalid
        }
    }

    public static List<InventoryTab> getActiveTabs() {
        return activeTabs;
    }
}