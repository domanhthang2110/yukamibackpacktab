package com.yukami.backpacktab.client.keybind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import com.yukami.backpacktab.client.gui.TabOffsetEditor;

@EventBusSubscriber(modid = "yukamibackpacktab", value = Dist.CLIENT)
public final class KeyInputHandler {
    private KeyInputHandler() {}

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen == null) {
            return;
        }

        if (TabOffsetEditor.handleKeyPressed(event.getKeyCode())) {
            event.setCanceled(true);
            return;
        }

        if (KeyBindings.toggleOffsetEditorKey != null &&
            event.getKeyCode() == KeyBindings.toggleOffsetEditorKey.getKey().getValue() &&
            minecraft.screen instanceof AbstractContainerScreen<?>) {
            TabOffsetEditor.toggle(minecraft.screen);
            event.setCanceled(true);
        }
    }
}
