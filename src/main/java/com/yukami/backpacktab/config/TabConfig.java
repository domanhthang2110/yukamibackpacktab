package com.yukami.backpacktab.config;

import com.yukami.backpacktab.YukamiBackpackTab;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TabConfig {
    private TabConfig() {}
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
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistedScreens;

        public Client(ModConfigSpec.Builder builder) {
            builder.comment("Yukami Backpack Tab Configuration")
                   .push("general");

            tabPosition = builder
                .comment("Position of the inventory tabs",
                        "Valid values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineEnum("tabPosition", TabPosition.TOP_LEFT);

            blacklistedScreens = builder
                .comment("Screen class names to blacklist from tab rendering",
                        "Format: Simple class name (e.g., 'BackpackScreen', 'InventoryScreen')",
                        "When a blacklisted screen is detected, tabs will not be rendered")
                .defineListAllowEmpty("blacklistedScreens", List.of(), () -> "", obj -> obj instanceof String);

            builder.pop();
        }
    }

    public static TabPosition getTabPosition() {
        return CLIENT.tabPosition.get();
    }

    public static void setGlobalTabPosition(TabPosition position) {
        CLIENT.tabPosition.set(position);
        CLIENT_SPEC.save();
    }

    public static List<? extends String> getBlacklistedScreens() {
        return CLIENT.blacklistedScreens.get();
    }

    public static boolean isScreenBlacklisted(String screenClassName) {
        if (screenClassName == null) return false;

        for (String blacklistedScreen : getBlacklistedScreens()) {
            if (blacklistedScreen != null && blacklistedScreen.trim().equals(screenClassName)) {
                return true;
            }
        }
        return false;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OFFSET_FILE_NAME = "screen_offsets.json";
    private static final int OFFSET_CONFIG_VERSION = 2;
    private static Map<String, Map<TabPosition, ScreenOffset>> screenOffsets = null;

    private static Map<String, TabPosition> screenPositions = null;

    public static class ScreenOffset {
        public final int x;
        public final int y;

        public ScreenOffset(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public static final ScreenOffset ZERO = new ScreenOffset(0, 0);
    }

    public static class ScreenOffsetConfig {
        public int version = 1;
        public Map<String, ScreenConfig> screens = new HashMap<>();

        boolean normalizeBottomOffsets() {
            if (version >= OFFSET_CONFIG_VERSION) return false;

            if (screens != null) {
                for (ScreenConfig screen : screens.values()) {
                    if (screen == null) continue;

                    if (screen.BOTTOM_LEFT != null) screen.BOTTOM_LEFT.y++;
                    if (screen.BOTTOM_RIGHT != null) screen.BOTTOM_RIGHT.y++;
                }
            }
            version = OFFSET_CONFIG_VERSION;
            return true;
        }

        public static class ScreenConfig {
            public String position;
            public OffsetData TOP_LEFT;
            public OffsetData TOP_RIGHT;
            public OffsetData BOTTOM_LEFT;
            public OffsetData BOTTOM_RIGHT;
        }

        public static class OffsetData {
            public int x;
            public int y;

            public OffsetData() {}

            public OffsetData(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    public static void initializeOffsets() {
        File offsetFile = getOffsetFile();

        if (!offsetFile.exists()) {
            createDefaultOffsetFile(offsetFile);
        }

        loadOffsetsFromJson(offsetFile);
    }

    public static ScreenOffset getScreenOffset(String screenClassName, TabPosition position) {
        if (screenClassName == null || position == null || screenOffsets == null) {
            return ScreenOffset.ZERO;
        }

        Map<TabPosition, ScreenOffset> screenConfig = screenOffsets.get(screenClassName);
        if (screenConfig == null) return ScreenOffset.ZERO;

        ScreenOffset offset = screenConfig.get(position);
        return offset != null ? offset : ScreenOffset.ZERO;
    }

    public static void setScreenOffset(String screenClassName, TabPosition position, ScreenOffset offset) {
        if (screenClassName == null || position == null || offset == null) {
            return;
        }

        if (screenOffsets == null) {
            screenOffsets = new HashMap<>();
        }

        screenOffsets.computeIfAbsent(screenClassName, ignored -> new HashMap<>()).put(position, offset);
    }

    public static TabPosition getScreenPositionOverride(String screenClassName) {
        if (screenClassName == null || screenPositions == null) {
            return null;
        }
        return screenPositions.get(screenClassName);
    }

    public static void setScreenPositionOverride(String screenClassName, TabPosition position) {
        if (screenClassName == null || position == null) {
            return;
        }
        if (screenPositions == null) {
            screenPositions = new HashMap<>();
        }
        screenPositions.put(screenClassName, position);
    }

    public static void removeScreenPositionOverride(String screenClassName) {
        if (screenClassName == null || screenPositions == null) {
            return;
        }
        screenPositions.remove(screenClassName);
    }

    public static void saveOffsets() {
        File offsetFile = getOffsetFile();
        ScreenOffsetConfig config = new ScreenOffsetConfig();
        config.version = OFFSET_CONFIG_VERSION;

        if (screenOffsets != null) {
            for (Map.Entry<String, Map<TabPosition, ScreenOffset>> entry : screenOffsets.entrySet()) {
                ScreenOffsetConfig.ScreenConfig screenConfig = new ScreenOffsetConfig.ScreenConfig();
                for (Map.Entry<TabPosition, ScreenOffset> offsetEntry : entry.getValue().entrySet()) {
                    setOffsetData(screenConfig, offsetEntry.getKey(), offsetEntry.getValue());
                }

                TabPosition positionOverride = screenPositions != null ? screenPositions.get(entry.getKey()) : null;
                screenConfig.position = positionOverride != null ? positionOverride.name() : null;
                config.screens.put(entry.getKey(), screenConfig);
            }
        }

        if (screenPositions != null) {
            for (Map.Entry<String, TabPosition> entry : screenPositions.entrySet()) {
                ScreenOffsetConfig.ScreenConfig screenConfig = config.screens.get(entry.getKey());
                if (screenConfig == null) {
                    screenConfig = new ScreenOffsetConfig.ScreenConfig();
                    config.screens.put(entry.getKey(), screenConfig);
                }
                screenConfig.position = entry.getValue().name();
            }
        }

        try (FileWriter writer = new FileWriter(offsetFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            YukamiBackpackTab.LOGGER.debug("Failed to save screen offsets to {}", offsetFile, e);
        }
    }

    private static void setOffsetData(ScreenOffsetConfig.ScreenConfig screenConfig, TabPosition position, ScreenOffset offset) {
        ScreenOffsetConfig.OffsetData offsetData = new ScreenOffsetConfig.OffsetData(offset.x, offset.y);
        switch (position) {
            case TOP_LEFT -> screenConfig.TOP_LEFT = offsetData;
            case TOP_RIGHT -> screenConfig.TOP_RIGHT = offsetData;
            case BOTTOM_LEFT -> screenConfig.BOTTOM_LEFT = offsetData;
            case BOTTOM_RIGHT -> screenConfig.BOTTOM_RIGHT = offsetData;
        }
    }

    private static void loadOffsetsFromJson(File offsetFile) {
        screenOffsets = new HashMap<>();
        screenPositions = new HashMap<>();
        boolean migrated;

        try (FileReader reader = new FileReader(offsetFile)) {
            ScreenOffsetConfig config = GSON.fromJson(reader, ScreenOffsetConfig.class);
            migrated = config != null && config.normalizeBottomOffsets();

            if (config != null && config.screens != null) {
                for (Map.Entry<String, ScreenOffsetConfig.ScreenConfig> entry : config.screens.entrySet()) {
                    String screenName = entry.getKey();
                    ScreenOffsetConfig.ScreenConfig screenConfig = entry.getValue();
                    if (screenConfig == null) {
                        continue;
                    }

                    Map<TabPosition, ScreenOffset> positionMap = new HashMap<>();

                    if (screenConfig.TOP_LEFT != null) {
                        positionMap.put(TabPosition.TOP_LEFT, new ScreenOffset(screenConfig.TOP_LEFT.x, screenConfig.TOP_LEFT.y));
                    }
                    if (screenConfig.TOP_RIGHT != null) {
                        positionMap.put(TabPosition.TOP_RIGHT, new ScreenOffset(screenConfig.TOP_RIGHT.x, screenConfig.TOP_RIGHT.y));
                    }
                    if (screenConfig.BOTTOM_LEFT != null) {
                        positionMap.put(TabPosition.BOTTOM_LEFT, new ScreenOffset(screenConfig.BOTTOM_LEFT.x, screenConfig.BOTTOM_LEFT.y));
                    }
                    if (screenConfig.BOTTOM_RIGHT != null) {
                        positionMap.put(TabPosition.BOTTOM_RIGHT, new ScreenOffset(screenConfig.BOTTOM_RIGHT.x, screenConfig.BOTTOM_RIGHT.y));
                    }

                    screenOffsets.put(screenName, positionMap);
                }

                for (Map.Entry<String, ScreenOffsetConfig.ScreenConfig> entry : config.screens.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().position != null) {
                        try {
                            screenPositions.put(entry.getKey(), TabPosition.valueOf(entry.getValue().position));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            YukamiBackpackTab.LOGGER.debug("Failed to load screen offsets from JSON", e);
            return;
        }
        if (migrated) {
            saveOffsets();
        }
    }

    private static File getOffsetFile() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("yukamibackpacktab");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            YukamiBackpackTab.LOGGER.debug("Failed to create config directory {}", configDir, e);
        }
        return configDir.resolve(OFFSET_FILE_NAME).toFile();
    }

    private static void createDefaultOffsetFile(File offsetFile) {
        ScreenOffsetConfig defaultConfig = new ScreenOffsetConfig();
        defaultConfig.version = OFFSET_CONFIG_VERSION;

        try (FileWriter writer = new FileWriter(offsetFile)) {
            GSON.toJson(defaultConfig, writer);
        } catch (IOException e) {
            YukamiBackpackTab.LOGGER.debug("Failed to create default offset file {}", offsetFile, e);
        }
    }
}
