package com.yukami.backpacktab.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.Arrays;

public class TabConfig {
    
    public enum TabPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public boolean isBottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }

        public boolean isRight() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }
    }
    
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    
    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }
    
    public static class Client {
        public final ModConfigSpec.EnumValue<TabPosition> tabPosition;
        public final ModConfigSpec.ConfigValue<List<? extends String>> additionalTabBlocks;
        
        public Client(ModConfigSpec.Builder builder) {
            builder.comment("Yukami Backpack Tab Configuration")
                   .push("general");
            
            tabPosition = builder
                .comment("Position of the inventory tabs",
                        "Valid values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineEnum("tabPosition", TabPosition.TOP_LEFT);
            
            additionalTabBlocks = builder
                .comment("Additional blocks that should have tabs enabled",
                        "Format: modID:block_name (e.g., 'minecraft:chest', 'sophisticatedbackpacks:backpack')")
                .defineList("additionalTabBlocks", Arrays.asList(), mapping -> ((String) mapping).matches("^[a-z0-9_.-]+:[a-z0-9/_.-]+$"));
            
            builder.pop();
        }
    }
    
    public static TabPosition getTabPosition() {
        return CLIENT.tabPosition.get();
    }


    public static List<? extends String> getAdditionalTabBlocks() {
        return CLIENT.additionalTabBlocks.get();
    }
    
}