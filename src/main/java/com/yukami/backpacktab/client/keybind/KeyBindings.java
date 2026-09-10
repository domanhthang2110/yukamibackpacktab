package com.yukami.backpacktab.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "yukamibackpacktab", value = Dist.CLIENT)
public final class KeyBindings {
    private KeyBindings() {}

    public static final String KEY_CATEGORY = "key.categories.yukamibackpacktab";
    public static final String KEY_TOGGLE_OFFSET_EDITOR = "key.yukamibackpacktab.toggle_offset_editor";

    public static KeyMapping toggleOffsetEditorKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleOffsetEditorKey = new KeyMapping(
            KEY_TOGGLE_OFFSET_EDITOR,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KEY_CATEGORY
        );

        event.register(toggleOffsetEditorKey);
    }
}
