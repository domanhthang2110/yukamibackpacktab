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
import static com.yukami.backpacktab.YukamiBackpackTab.LOGGER;

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

        LOGGER.info("PlayerTab.open() START - Current handler: {}, containerId: {}", 
                   handler != null ? handler.getClass().getSimpleName() : "null", 
                   handler != null ? handler.containerId : "null");
        LOGGER.info("PlayerTab.open() - Player containerMenu: {}, containerId: {}", 
                   localPlayer.containerMenu.getClass().getSimpleName(), 
                   localPlayer.containerMenu.containerId);

        if (gameMode.getPlayerMode() == GameType.SURVIVAL || gameMode.getPlayerMode() == GameType.ADVENTURE) {
            LOGGER.info("PlayerTab.open() - Opening InventoryScreen");
            // Force the player to use their inventory menu like vanilla does
            localPlayer.containerMenu = localPlayer.inventoryMenu;
            Minecraft.getInstance().setScreen(new InventoryScreen(localPlayer));
            LOGGER.info("PlayerTab.open() END - Player containerMenu after screen open: {}, containerId: {}", 
                       localPlayer.containerMenu.getClass().getSimpleName(), 
                       localPlayer.containerMenu.containerId);
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