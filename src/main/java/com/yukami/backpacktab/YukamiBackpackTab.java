package com.yukami.backpacktab;

import com.mojang.logging.LogUtils;
import com.yukami.backpacktab.client.config.TabConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(YukamiBackpackTab.MODID)
public class YukamiBackpackTab {
    public static final String MODID = "yukamibackpacktab";
    public static final Logger LOGGER = LogUtils.getLogger();

    public YukamiBackpackTab(IEventBus modEventBus, ModContainer modContainer) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.CLIENT, TabConfig.CLIENT_SPEC);
        
        LOGGER.info("Yukami Backpack Tab mod initialized");
    }
}