package com.yukami.backpacktab.client.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;
import java.util.Arrays;

public class TabConfig {
    
    public enum TabPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
    
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    
    static {
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
    
    public static class Client {
        public final ForgeConfigSpec.EnumValue<TabPosition> tabPosition;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> additionalTabBlocks;
        
        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Yukami Backpack Tab Configuration")
                   .push("general");
            
            tabPosition = builder
                .comment("Position of the inventory tabs",
                        "Valid values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineEnum("tabPosition", TabPosition.TOP_LEFT);
            
            additionalTabBlocks = builder
                .comment("Additional blocks that should have tabs enabled",
                        "Format: modID:block_name (e.g., 'minecraft:chest', 'sophisticatedbackpacks:backpack')")
                .defineList("additionalTabBlocks", Arrays.asList(), obj -> obj instanceof String);
            
            builder.pop();
        }
    }
    
    public static TabPosition getTabPosition() {
        return CLIENT.tabPosition.get();
    }
    
    public static List<? extends String> getAdditionalTabBlocks() {
        return CLIENT.additionalTabBlocks.get();
    }
    
    /**
     * Get sprite coordinates based on position and state
     */
    public static SpriteCoords getSpriteCoords(TabPosition position, boolean active, boolean isFirstTab) {
        return switch (position) {
            case TOP_LEFT -> {
                if (active) {
                    yield isFirstTab ? new SpriteCoords(0, 32) : new SpriteCoords(26, 32); // [2,1] or [2,2]
                } else {
                    yield new SpriteCoords(0, 0); // [1,1] universal disabled
                }
            }
            case TOP_RIGHT -> {
                if (active) {
                    yield isFirstTab ? new SpriteCoords(156, 32) : new SpriteCoords(130, 32); // [2,7] or [2,6]
                } else {
                    yield new SpriteCoords(0, 0); // [1,1] universal disabled
                }
            }
            case BOTTOM_LEFT -> {
                if (active) {
                    yield isFirstTab ? new SpriteCoords(0, 96) : new SpriteCoords(26, 96); // [4,1] or [4,2]
                } else {
                    yield new SpriteCoords(0, 64); // [3,1] disabled bottom left
                }
            }
            case BOTTOM_RIGHT -> {
                if (active) {
                    yield isFirstTab ? new SpriteCoords(156, 96) : new SpriteCoords(130, 96); // [4,7] or [4,6]
                } else {
                    yield new SpriteCoords(156, 64); // [3,7] disabled bottom right
                }
            }
        };
    }
    
    public static class SpriteCoords {
        public final int u, v;
        
        public SpriteCoords(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }
}