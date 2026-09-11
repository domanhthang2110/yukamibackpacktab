package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.YukamiBackpackTab;
import com.yukami.backpacktab.client.gui.TabManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.Optional;
import java.util.UUID;

public final class BackpackTab implements InventoryTab {
    private final ItemStack backpackStack;
    private final String handlerName;
    private final String identifier;
    private final int slot;
    private final Optional<UUID> contentsUuid;
    private boolean active;
    private Component cachedHoverText;

    public BackpackTab(ItemStack backpackStack, String handlerName, String identifier, int slot) {
        this.backpackStack = backpackStack.copy();
        this.handlerName = handlerName;
        this.identifier = identifier;
        this.slot = slot;
        this.contentsUuid = BackpackWrapper.fromStack(backpackStack).getContentsUuid();
    }

    public Optional<BackpackTab> resolveCurrentLocation(Player player) {
        try {
            BackpackTab[] match = {null};
            boolean[] ambiguous = {false};

            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, handlerName, currentIdentifier, currentSlot) -> {
                if ("main".equals(handlerName) || !matchesBackpack(backpack)) {
                    return false;
                }

                if (match[0] != null) {
                    ambiguous[0] = true;
                } else if (handlerName.equals(this.handlerName)
                        && currentIdentifier.equals(identifier)
                        && currentSlot == slot) {
                    match[0] = this;
                } else {
                    match[0] = new BackpackTab(backpack, handlerName, currentIdentifier, currentSlot);
                }
                return false;
            });

            return ambiguous[0] ? Optional.empty() : Optional.ofNullable(match[0]);
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to refresh equipped backpack location", e);
            return Optional.empty();
        }
    }

    private boolean matchesBackpack(ItemStack candidate) {
        Optional<UUID> candidateUuid = BackpackWrapper.fromStack(candidate).getContentsUuid();
        if (contentsUuid.isPresent()) {
            return contentsUuid.equals(candidateUuid);
        }
        return candidateUuid.isEmpty() && ItemStack.isSameItemSameComponents(backpackStack, candidate);
    }

    @Override
    public ItemStack getTabIcon() {
        return backpackStack;
    }

    @Override
    public Component getHoverText() {
        if (cachedHoverText == null) {
            cachedHoverText = backpackStack.getHoverName();
        }
        return cachedHoverText;
    }

    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null) return;

        try {
            ClientPacketDistributor.sendToServer(new BackpackOpenPayload(slot, identifier, handlerName));
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to open equipped backpack tab", e);
        }
    }

    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof IBackpackScreen)) {
            return false;
        }

        BlockPos storedPos = TabManager.getStoredBlockPos();
        if (storedPos != null) {
            Level world = Minecraft.getInstance().level;
            if (world != null) {
                BlockState blockState = world.getBlockState(storedPos);
                if (blockState.getBlock() instanceof BackpackBlock) {
                    Optional<UUID> screenUuid = TabManager.getScreenBackpackContentsUuid(screen);
                    if (screenUuid.isEmpty()) {
                        return false;
                    }
                    return !screenUuid.equals(TabManager.getBlockBackpackContentsUuid(storedPos));
                }
            }
        }

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
