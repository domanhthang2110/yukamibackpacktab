package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;

import java.util.ArrayList;
import java.util.List;

import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;

@Mod.EventBusSubscriber(modid = "yukamibackpacktab", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class InventoryTabManager {
    
    private static AbstractContainerScreen<?> currentScreen = null;
    private static BlockPos storedBlockPos = null; // Store block pos when block GUI opens
    private static boolean isTabSwitching = false; // Track if we're in the middle of a tab switch
    private static final List<InventoryTab> activeTabs = new ArrayList<>(); // List of tabs to render

    /**
     * Checks if the given block position corresponds to a container block.
     * @param world The world instance.
     * @param pos The block position.
     * @return True if the block is a container, false otherwise.
     */
    private static boolean isContainerBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        try {
            return blockState.getMenuProvider(world, pos) != null;
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
     * Determines if the given screen is an instance of IBackpackScreen.
     */
    private static boolean isBackpackScreen(AbstractContainerScreen<?> screen) {
        return screen instanceof IBackpackScreen;
    }

    /**
     * Attempts to get the BlockPos of the targeted block if it's a container.
     * Returns null if no block is targeted, or if the targeted block is not a container.
     */
    private static BlockPos getTargetedBlockPos() {
        var hitResult = Minecraft.getInstance().hitResult;
        if (!(hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHitResult)) {
            return null;
        }

        BlockPos hitBlockPos = blockHitResult.getBlockPos();
        Level world = Minecraft.getInstance().level;

        if (world != null && isContainerBlock(world, hitBlockPos)) {
            return hitBlockPos;
        } else {
            return null;
        }
    }

    /**
     * Updates the current screen state and clears previous context if not tab switching.
     */
    private static void updateScreenState(AbstractContainerScreen<?> screen) {
        currentScreen = screen;
        if (!isTabSwitching) {
            storedBlockPos = null; // Clear stored block pos if not tab switching
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

        if (!isBackpackScreen(screen)) {
            // This covers initial container open and switching to container
            BlockPos hitBlockPos = getTargetedBlockPos();
            if (hitBlockPos != null) {
                storedBlockPos = hitBlockPos;
                return new ContainerTab(storedBlockPos);
            } else {
                return new PlayerTab(); // Fallback if no valid block is targeted
            }
        }

        // If a backpack is opened directly or switched to, the base tab depends on the previous context
        if (storedBlockPos != null) {
            return new ContainerTab(storedBlockPos);
        } else {
            return new PlayerTab(); // Default to player tab as base
        }
    }

    /**
     * Rebuilds the list of active tabs based on the base tab and equipped backpack.
     */
    private static void rebuildTabList(InventoryTab baseTab) {
        activeTabs.clear(); // Clear before rebuilding to ensure correct order and prevent duplicates
        if (baseTab != null) {
            activeTabs.add(baseTab);
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            BackpackTab backpackTab = TabRenderer.getEquippedBackpackTab(player);
            if (backpackTab != null) {
                activeTabs.add(backpackTab);
            }
        }
    }

    /**
     * Resets all static state variables of the tab manager.
     */
    private static void resetState() {
        storedBlockPos = null;
        currentScreen = null;
        isTabSwitching = false;
        activeTabs.clear(); // Clear active tabs on reset
        CarriedItemUtil.reset(); // Also reset any stashed items
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        updateScreenState(containerScreen);
        
        InventoryTab baseTab = determineBaseTab(containerScreen);
        rebuildTabList(baseTab);
        
        // Reset tab switching flag after screen opens
        isTabSwitching = false;
        
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
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        
        // Allow rendering for either the current tracked screen OR the survival inventory
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
            return blockState.getMenuProvider(world, pos) != null;
        } catch (Exception e) {
            return false; // If any error occurs, consider it invalid
        }
    }

    public static List<InventoryTab> getActiveTabs() {
        return activeTabs;
    }
}
