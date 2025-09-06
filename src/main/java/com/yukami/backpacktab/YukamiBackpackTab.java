package com.yukami.backpacktab;

import com.mojang.logging.LogUtils;
import com.yukami.backpacktab.client.config.TabConfig;
import com.yukami.backpacktab.client.gui.InventoryTabManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(YukamiBackpackTab.MODID)
public class YukamiBackpackTab {
    public static final String MODID = "yukamibackpacktab";
    public static final Logger LOGGER = LogUtils.getLogger();

    public YukamiBackpackTab(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        // Register config
        TabConfig.register();
        
        // Register client setup
        modEventBus.addListener(this::clientSetup);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("Yukami Backpack Tab mod initialized");
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        // Client-side initialization
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(InventoryTabManager.class);
            LOGGER.info("Client setup for Yukami Backpack Tab - registered inventory tab manager");
        });
    }
}