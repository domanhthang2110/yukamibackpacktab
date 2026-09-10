package com.yukami.backpacktab;

import com.mojang.logging.LogUtils;
import com.yukami.backpacktab.config.TabConfig;
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
        modContainer.registerConfig(ModConfig.Type.CLIENT, TabConfig.CLIENT_SPEC);

        TabConfig.initializeOffsets();

        LOGGER.info("Yukami Backpack Tab mod initialized");
    }
}
