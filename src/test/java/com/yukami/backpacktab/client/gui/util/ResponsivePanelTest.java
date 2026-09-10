package com.yukami.backpacktab.client.gui.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResponsivePanelTest {
    @Test
    public void wideWindowKeepsPanelBesideInventory() {
        var layout = ResponsivePanel.findLayout(854, 480, 339, 157, 166,
                144, 104, 8, width -> width < 144 ? 188 : 164);
        assertTrue(layout.x() + layout.width() + 8 <= 339);
        assertEquals(144, layout.width());
    }

    @Test
    public void smallWindowKeepsLabelsAndSaveButtonsOnScreen() {
        var layout = ResponsivePanel.findLayout(320, 240, 72, 37, 166,
                144, 104, 8, width -> width < 144 ? 188 : 164);
        assertEquals(104, layout.width());
        assertTrue(layout.x() >= 8 && layout.x() + layout.width() <= 312);
        assertTrue(layout.y() >= 8 && layout.y() + layout.height() <= 232);
    }

    @Test
    public void narrowSideThatWouldOverflowVerticallyIsNotUsed() {
        var layout = ResponsivePanel.findLayout(500, 240, 170, 37, 166,
                184, 140, 8, width -> width < 180 ? 300 : 224);
        assertEquals(184, layout.width());
        assertTrue(layout.y() + layout.height() <= 232);
    }

    @Test
    public void narrowGutterUsesAvailableWidthAndReflows() {
        var layout = ResponsivePanel.findLayout(500, 240, 136, 37, 166,
                144, 104, 8, width -> width < 144 ? 188 : 164);
        assertEquals(8, layout.x());
        assertEquals(120, layout.width());
        assertEquals(188, layout.height());
    }

    @Test
    public void changingRightHandSpaceDoesNotChangeFallbackAnchor() {
        for (int screenWidth : new int[] {320, 321, 500}) {
            var layout = ResponsivePanel.findLayout(screenWidth, 240, 30, 37, 166,
                    144, 104, 8, width -> width < 144 ? 188 : 164);
            assertEquals(8, layout.x());
            assertEquals(104, layout.width());
        }
    }

    @Test
    public void shortWindowUsesWiderLayoutToKeepFooterVisible() {
        var layout = ResponsivePanel.findLayout(320, 190, 72, 12, 166,
                144, 104, 8, width -> width < 144 ? 188 : 164);
        assertEquals(8, layout.x());
        assertEquals(144, layout.width());
        assertTrue(layout.y() + layout.height() <= 182);
    }

    @Test
    public void largerFontMinimumIsRespectedInCompactMode() {
        var layout = ResponsivePanel.findLayout(320, 240, 72, 37, 166,
                190, 142, 8, width -> width < 190 ? 188 : 164);
        assertEquals(142, layout.width());
        assertTrue(layout.x() + layout.width() <= 312);
        assertTrue(layout.y() + layout.height() <= 232);
    }
}
