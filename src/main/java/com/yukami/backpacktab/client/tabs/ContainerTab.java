package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.YukamiBackpackTab;
import com.yukami.backpacktab.client.gui.TabManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.IBackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;

import java.util.Optional;
import java.util.UUID;

public final class ContainerTab implements InventoryTab {
    private final BlockPos containerPos;
    private boolean active;
    private ItemStack cachedIcon;
    private Component cachedHoverText;

    public ContainerTab(BlockPos containerPos) {
        this.containerPos = containerPos;
    }

    @Override
    public ItemStack getTabIcon() {
        if (cachedIcon == null) {
            cachedIcon = resolveIcon();
        }
        return cachedIcon;
    }

    private ItemStack resolveIcon() {
        if (containerPos != null) {
            Level world = Minecraft.getInstance().level;
            if (world != null) {
                BlockState blockState = world.getBlockState(containerPos);

                if (blockState.getBlock() instanceof BackpackBlock) {
                    try {
                        var blockEntity = world.getBlockEntity(containerPos);
                        if (blockEntity instanceof BackpackBlockEntity backpackBE) {
                            ItemStack backpackStack = backpackBE.getBackpackWrapper().getBackpack();
                            if (!backpackStack.isEmpty()) {
                                return backpackStack;
                            }
                        }
                    } catch (Exception e) {
                        YukamiBackpackTab.LOGGER.debug("Failed to read backpack block icon at {}", containerPos, e);
                    }
                }

                return new ItemStack(blockState.getBlock());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getHoverText() {
        if (cachedHoverText == null) {
            ItemStack icon = getTabIcon();
            cachedHoverText = icon.isEmpty() ? Component.literal("Container") : icon.getHoverName();
        }
        return cachedHoverText;
    }

    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null || !(player instanceof LocalPlayer localPlayer)) return;

        try {
            if (containerPos != null) {
                BlockState blockState = world.getBlockState(containerPos);
                Direction face = getFrontFace(blockState, containerPos, localPlayer);
                Vec3 hitVec = new Vec3(
                        containerPos.getX() + 0.5 + face.getStepX() * 0.5,
                        containerPos.getY() + 0.5 + face.getStepY() * 0.5,
                        containerPos.getZ() + 0.5 + face.getStepZ() * 0.5
                );
                BlockHitResult hitResult = new BlockHitResult(hitVec, face, containerPos, false);

                gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, hitResult);
            } else {
                Minecraft.getInstance().setScreen(null);
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to open container tab at {}", containerPos, e);
            Minecraft.getInstance().setScreen(null);
        }
    }

    private static Direction getFrontFace(BlockState blockState, BlockPos blockPos, Player player) {
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            return blockState.getValue(BlockStateProperties.FACING);
        }

        double x = player.getX() - (blockPos.getX() + 0.5);
        double z = player.getZ() - (blockPos.getZ() + 0.5);
        if (x == 0.0 && z == 0.0) {
            return player.getDirection().getOpposite();
        }
        return Direction.getApproximateNearest(x, 0.0, z);
    }

    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        if (containerPos == null) {
            return false;
        }

        Level world = Minecraft.getInstance().level;
        if (world != null) {
            BlockState blockState = world.getBlockState(containerPos);
            if (blockState.getBlock() instanceof BackpackBlock) {
                if (!(screen instanceof IBackpackScreen)) {
                    return false;
                }

                Optional<UUID> screenUuid = TabManager.getScreenBackpackContentsUuid(screen);
                if (screenUuid.isEmpty()) {
                    return true;
                }
                return screenUuid.equals(TabManager.getBlockBackpackContentsUuid(containerPos));
            }
        }

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
