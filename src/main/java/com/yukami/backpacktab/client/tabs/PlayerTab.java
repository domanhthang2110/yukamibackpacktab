package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import com.mojang.authlib.GameProfile;

public final class PlayerTab implements InventoryTab {
    private boolean active;

    private ItemStack cachedIcon;

    private Component cachedHoverText;

    @Override
    public ItemStack getTabIcon() {
        if (cachedIcon == null || cachedIcon.isEmpty()) {
            cachedIcon = createIcon();
        }
        return cachedIcon;
    }

    private ItemStack createIcon() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        GameProfile gameProfile = player.getGameProfile();

        if (gameProfile.getId() != null) {
            ResolvableProfile profile = new ResolvableProfile(gameProfile);
            playerHead.set(DataComponents.PROFILE, profile);
        }
        return playerHead;
    }

    @Override
    public Component getHoverText() {
        if (cachedHoverText == null) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            cachedHoverText = player == null ? Component.literal("Player") : Component.literal(player.getName().getString());
        }
        return cachedHoverText;
    }

    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null || !(player instanceof LocalPlayer localPlayer)) return;

        if (gameMode.getPlayerMode() == GameType.SURVIVAL || gameMode.getPlayerMode() == GameType.ADVENTURE) {
            localPlayer.containerMenu = localPlayer.inventoryMenu;
            Minecraft.getInstance().setScreen(new InventoryScreen(localPlayer));
        }
    }

    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen;
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
