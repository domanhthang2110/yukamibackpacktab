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
    private static net.minecraft.world.level.block.Block storedBlockType = null; // Store original block type
    private static boolean isTabSwitching = false; // Track if we're in the middle of a tab switch
    private static boolean isFromBlockClick = false; // Track if the next screen is from a block click
    private static final List<InventoryTab> activeTabs = new ArrayList<>(); // List of tabs to render
    
    // Config caching
    private static List<? extends String> cachedAdditionalTabBlocks = null;
    
    private static List<? extends String> getAdditionalTabBlocks() {
        if (cachedAdditionalTabBlocks == null) {
            cachedAdditionalTabBlocks = TabConfig.getAdditionalTabBlocks();
        }
        return cachedAdditionalTabBlocks;
    }

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

    // Complex tab building logic moved to TabProvider


    /**
     * Resets all static state variables of the tab manager.
     */
    private static void resetState() {
        storedBlockPos = null;
        storedBlockType = null;
        currentScreen = null;
        isTabSwitching = false;
        isFromBlockClick = false;
        activeTabs.clear(); // Clear active tabs on reset
        CarriedItemUtil.reset(); // Also reset any stashed items
        TabRenderer.invalidateCache(); // Clear cached performance optimizations
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Only handle client-side events
        if (event.getLevel().isClientSide()) {
            BlockPos clickedPos = event.getPos();
            Level world = event.getLevel();
            
            if (isContainerBlock(world, clickedPos)) {
                storedBlockPos = clickedPos;
                storedBlockType = world.getBlockState(clickedPos).getBlock(); // Store the original block type
                isFromBlockClick = true;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        
        updateScreenState(containerScreen);
        
        // Use TabProvider for clean tab management
        activeTabs.clear();
        activeTabs.addAll(TabProvider.getAvailableTabsForScreen(
            Minecraft.getInstance().player, 
            containerScreen, 
            storedBlockPos
        ));
        
        // Set current tab as active based on what screen matches
        if (!activeTabs.isEmpty()) {
            activeTabs.forEach(tab -> tab.setActive(tab.matchesCurrentScreen(containerScreen)));
        }
        
        
        // Reset flags after screen opens
        isTabSwitching = false;
        isFromBlockClick = false;
        
        // Restore any stashed carried item after screen opens
        Player player = Minecraft.getInstance().player;
        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
            net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) {
                TabSwitcher.restoreCarriedItems(localPlayer, gameMode);
            }
        }
    }
    
    @SubscribeEvent
    public static void onScreenRender(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        // Allow rendering for either the current tracked screen OR the survival inventory
        if (containerScreen != currentScreen && !(containerScreen instanceof InventoryScreen)) return;
        
        // Validate stored block is still the same before rendering tabs
        if (storedBlockPos != null && storedBlockType != null) {
            if (!TabProvider.isBlockStillValid(storedBlockPos, storedBlockType)) {
                handleInvalidBlock();
                return; // Don't render tabs if block is invalid
            }
        }
        
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
    
    public static net.minecraft.world.level.block.Block getStoredBlockType() {
        return storedBlockType;
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

    public static List<InventoryTab> getActiveTabs() {
        return activeTabs;
    }
}