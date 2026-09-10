package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.yukami.backpacktab.YukamiBackpackTab;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;
import com.yukami.backpacktab.client.util.CarriedItemUtil;
import com.yukami.backpacktab.config.TabConfig;

@EventBusSubscriber(modid = "yukamibackpacktab", value = Dist.CLIENT)
public final class TabManager {
    private TabManager() {}

    private static AbstractContainerScreen<?> currentScreen;
    private static BlockPos storedBlockPos;
    private static Block storedBlockType;
    private static final TabSwitchState tabSwitch = new TabSwitchState();
    private static final List<InventoryTab> activeTabs = new ArrayList<>();

    static boolean beginTabSwitch(InventoryTab target, AbstractContainerMenu menu) {
        return tabSwitch.tryBegin(target.isActive(), menu, System.nanoTime());
    }

    static void refreshTabsForCurrentScreen(Player player) {
        if (currentScreen == null) return;
        activeTabs.clear();
        activeTabs.addAll(getAvailableTabs(player, currentScreen));
        updateActiveStates(activeTabs, currentScreen);
    }

    static void replaceActiveTab(InventoryTab oldTab, InventoryTab newTab) {
        int index = activeTabs.indexOf(oldTab);
        if (index >= 0) {
            activeTabs.set(index, newTab);
        }
    }

    public static Optional<UUID> getScreenBackpackContentsUuid(AbstractContainerScreen<?> screen) {
        try {
            if (screen.getMenu() instanceof BackpackContainer backpackContainer) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    return backpackContainer.getBackpackContext().getBackpackWrapper(player).getContentsUuid();
                }
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to resolve screen backpack identity", e);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getBlockBackpackContentsUuid(BlockPos pos) {
        try {
            Level world = Minecraft.getInstance().level;
            if (world != null && world.getBlockEntity(pos) instanceof BackpackBlockEntity backpackBE) {
                return backpackBE.getBackpackWrapper().getContentsUuid();
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to resolve block backpack identity at {}", pos, e);
        }
        return Optional.empty();
    }

    public static List<InventoryTab> getAvailableTabs(Player player, AbstractContainerScreen<?> screen) {
        List<InventoryTab> tabs = new ArrayList<>();

        BackpackTab equippedBackpack = getEquippedBackpackTab(player);
        if (equippedBackpack == null) {
            return tabs;
        }

        if (screen instanceof InventoryScreen) {
            tabs.add(new PlayerTab());
            tabs.add(equippedBackpack);
            return tabs;
        }

        if (storedBlockPos != null && !isStoredBlockValid()) {
            clearStoredBlock();
        }

        if (storedBlockPos != null && isBackpackBlock(player.level(), storedBlockPos)) {
            tabs.add(new ContainerTab(storedBlockPos));
            tabs.add(equippedBackpack);
            return tabs;
        }

        if (storedBlockPos != null) {
            tabs.add(new ContainerTab(storedBlockPos));
        } else {
            tabs.add(new PlayerTab());
        }

        tabs.add(equippedBackpack);
        return tabs;
    }

    public static void updateActiveStates(List<InventoryTab> tabs, AbstractContainerScreen<?> screen) {
        tabs.forEach(tab -> tab.setActive(tab.matchesCurrentScreen(screen)));
    }

    private static BackpackTab getEquippedBackpackTab(Player player) {
        try {
            BackpackTab[] foundTab = {null};

            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, handlerName, identifier, slot) -> {
                if ("main".equals(handlerName)) {
                    return false;
                }
                foundTab[0] = new BackpackTab(backpack, handlerName, identifier, slot);
                return true;
            });

            return foundTab[0];
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to find equipped backpack", e);
            return null;
        }
    }

    public static boolean isStoredBlockValid() {
        if (storedBlockPos == null || storedBlockType == null) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        Level world = player.level();
        if (world == null) return false;

        BlockState blockState = world.getBlockState(storedBlockPos);
        if (blockState.getBlock() != storedBlockType) {
            return false;
        }

        return player.canInteractWithBlock(storedBlockPos, 1.0);
    }

    private static boolean isContainerBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;

        BlockState blockState = world.getBlockState(pos);

        if (blockState.getBlock() instanceof BackpackBlock) {
            return true;
        }

        try {
            return blockState.getMenuProvider(world, pos) != null;
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to inspect menu provider at {}", pos, e);
            return false;
        }
    }

    private static boolean isBackpackBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;
        BlockState blockState = world.getBlockState(pos);
        return blockState.getBlock() instanceof BackpackBlock;
    }

    private static boolean isScreenAllowedForTabs(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) {
            return true;
        }

        if (screen instanceof IBackpackScreen) {
            return true;
        }

        return storedBlockPos != null && isStoredBlockValid();
    }

    private static boolean useCurrentScreen(AbstractContainerScreen<?> screen) {
        if (screen == currentScreen) return true;
        if (screen instanceof InventoryScreen) {
            currentScreen = screen;
            return true;
        }
        if (currentScreen == null) return false;
        if (!screen.getClass().equals(currentScreen.getClass()) && screen.getMenu() != currentScreen.getMenu()) {
            return false;
        }
        currentScreen = screen;
        return true;
    }

    private static void handleInvalidBlock() {
        if (currentScreen instanceof IBackpackScreen) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.closeContainer();
            }
        }
        resetState();
    }

    private static void resetState() {
        clearStoredBlock();
        currentScreen = null;
        tabSwitch.reset();
        activeTabs.clear();
        CarriedItemUtil.reset();
        TabRenderer.invalidateCache();
    }

    private static void clearStoredBlock() {
        storedBlockPos = null;
        storedBlockType = null;
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == TabConfig.CLIENT_SPEC) {
            TabRenderer.invalidateCache();
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            BlockPos clickedPos = event.getPos();
            Level world = event.getLevel();

            if (isContainerBlock(world, clickedPos)) {
                storedBlockPos = clickedPos;
                storedBlockType = world.getBlockState(clickedPos).getBlock();
            } else {
                clearStoredBlock();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        currentScreen = containerScreen;

        boolean completedSwitch = tabSwitch.complete(containerScreen.getMenu());
        if (tabSwitch.isPending()) {
            return;
        }

        if (containerScreen instanceof InventoryScreen) {
            clearStoredBlock();
        } else if (storedBlockPos != null && !isStoredBlockValid()) {
            clearStoredBlock();
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (!completedSwitch) {
                activeTabs.clear();
                activeTabs.addAll(getAvailableTabs(player, containerScreen));
            }

            updateActiveStates(activeTabs, containerScreen);
        }

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

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabRenderer.renderTabs(event.getGuiGraphics(), containerScreen, event.getMouseX(), event.getMouseY());
            return;
        }

        if (TabConfig.isScreenBlacklisted(containerScreen.getClass().getSimpleName())) {
            return;
        }

        if (!isScreenAllowedForTabs(containerScreen)) {
            return;
        }

        if (!useCurrentScreen(containerScreen)) return;

        if (storedBlockPos != null && !isStoredBlockValid()) {
            handleInvalidBlock();
            return;
        }

        TabRenderer.renderTabs(event.getGuiGraphics(), containerScreen, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen &&
                TabOffsetEditor.isEnabledFor(screen)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        if (tabSwitch.isPending()) {
            event.setCanceled(true);
            return;
        }

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMousePressed(containerScreen, event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
            return;
        }

        if (!useCurrentScreen(containerScreen)) return;

        if (TabConfig.isScreenBlacklisted(containerScreen.getClass().getSimpleName())) {
            return;
        }

        if (!isScreenAllowedForTabs(containerScreen)) {
            return;
        }

        if (TabRenderer.handleTabClick(event.getMouseX(), event.getMouseY(), event.getButton(), containerScreen)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!tabSwitch.isPending() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (event.getKeyCode() != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE &&
                !minecraft.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        if (tabSwitch.isPending()) {
            event.setCanceled(true);
            return;
        }

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMouseDragged(containerScreen, event.getMouseX(), event.getMouseY(), event.getMouseButton());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        if (tabSwitch.isPending()) {
            event.setCanceled(true);
            return;
        }

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMouseReleased(containerScreen, event.getButton());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && TabOffsetEditor.isEnabledFor(screen)) {
            TabOffsetEditor.cancel();
        }

        if (event.getScreen() == currentScreen) {
            if (!tabSwitch.isPending()) {
                resetState();
            }
            currentScreen = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!tabSwitch.isPending()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            resetState();
        } else if (tabSwitch.hasTimedOut(System.nanoTime())) {
            resetState();
            minecraft.player.clientSideCloseContainer();
            minecraft.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Tab switch timed out. Please reopen your inventory."));
        }
    }

    public static List<InventoryTab> getActiveTabsView() {
        return activeTabs;
    }

    public static BlockPos getStoredBlockPos() {
        return storedBlockPos;
    }
}
