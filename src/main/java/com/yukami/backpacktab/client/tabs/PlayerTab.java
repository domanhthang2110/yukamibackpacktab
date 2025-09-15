package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import com.mojang.authlib.GameProfile;
import com.yukami.backpacktab.client.util.CarriedItemUtil;

public class PlayerTab implements InventoryTab {
    
    private boolean active = false;
    
    public PlayerTab() {
        
    }
    
    @Override
    public ItemStack getTabIcon() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        GameProfile gameProfile = player.getGameProfile();
        
        // Use the new data component system for player heads
        if (gameProfile.getId() != null) {
            ResolvableProfile profile = new ResolvableProfile(gameProfile);
            playerHead.set(DataComponents.PROFILE, profile);
        }
        return playerHead;
    }
    
    @Override
    public Component getHoverText() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return Component.literal("Player");
        }
        return Component.literal(player.getName().getString());
    }
    
    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (player == null || gameMode == null || !(player instanceof LocalPlayer localPlayer)) return;

        // Stash carried item in inventory slot before opening the screen
        CarriedItemUtil.stashCarriedItem(localPlayer, gameMode, handler);

        // If we are currently in a server-side container (not player inventory), close it first
        if (handler != null && !(handler instanceof InventoryMenu) && localPlayer.connection != null) {
            localPlayer.connection.send(new ServerboundContainerClosePacket(handler.containerId));
        }

        if (gameMode.getPlayerMode() == GameType.SURVIVAL || gameMode.getPlayerMode() == GameType.ADVENTURE) {
            Minecraft.getInstance().setScreen(new InventoryScreen(localPlayer));
        }
    }
    
    @Override
    public void close(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        // Container closing is handled by the server
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

    @Override
    public boolean isInstant() {
        return true;
    }
}