package com.yukami.backpacktab.config;

import com.yukami.backpacktab.config.TabConfig.ScreenOffsetConfig;
import com.yukami.backpacktab.config.TabConfig.ScreenOffsetConfig.OffsetData;
import com.yukami.backpacktab.config.TabConfig.ScreenOffsetConfig.ScreenConfig;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScreenOffsetConfigTest {
    @Test
    public void legacyOffsetsPreservePlacementAndOnlyNormalizeBottomY() {
        var config = new ScreenOffsetConfig();
        var screen = new ScreenConfig();
        screen.position = "BOTTOM_RIGHT";
        screen.TOP_LEFT = new OffsetData(3, -4);
        screen.TOP_RIGHT = new OffsetData(5, 6);
        screen.BOTTOM_LEFT = new OffsetData(7, -1);
        screen.BOTTOM_RIGHT = new OffsetData(-8, 12);
        config.screens.put("InventoryScreen", screen);

        assertTrue(config.normalizeBottomOffsets());
        assertEquals(2, config.version);
        assertEquals(0, screen.BOTTOM_LEFT.y);
        assertEquals(13, screen.BOTTOM_RIGHT.y);
        assertEquals(7, screen.BOTTOM_LEFT.x);
        assertEquals(-8, screen.BOTTOM_RIGHT.x);
        assertEquals(3, screen.TOP_LEFT.x);
        assertEquals(-4, screen.TOP_LEFT.y);
        assertEquals(5, screen.TOP_RIGHT.x);
        assertEquals(6, screen.TOP_RIGHT.y);
        assertEquals("BOTTOM_RIGHT", screen.position);

        assertFalse(config.normalizeBottomOffsets());
        assertEquals(0, screen.BOTTOM_LEFT.y);
        assertEquals(13, screen.BOTTOM_RIGHT.y);
    }

    @Test
    public void currentAndFutureVersionsAreNotAdjusted() {
        for (int version : new int[]{2, 3}) {
            var config = new ScreenOffsetConfig();
            config.version = version;
            var screen = new ScreenConfig();
            screen.BOTTOM_LEFT = new OffsetData(0, -1);
            config.screens.put("InventoryScreen", screen);

            assertFalse(config.normalizeBottomOffsets());
            assertEquals(version, config.version);
            assertEquals(-1, screen.BOTTOM_LEFT.y);
        }
    }

    @Test
    public void missingOffsetsKeepUsingTheNewDefault() {
        var config = new ScreenOffsetConfig();
        var screen = new ScreenConfig();
        config.screens.put("InventoryScreen", screen);
        config.screens.put("MissingScreen", null);

        assertTrue(config.normalizeBottomOffsets());
        assertNull(screen.BOTTOM_LEFT);
        assertNull(screen.BOTTOM_RIGHT);

        var emptyConfig = new ScreenOffsetConfig();
        emptyConfig.screens = null;
        assertTrue(emptyConfig.normalizeBottomOffsets());
    }
}
