package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.config.TabConfig.ScreenOffset;
import com.yukami.backpacktab.config.TabConfig.TabPosition;
import org.junit.Test;

import static org.junit.Assert.*;

public class TabBoundsTest {
    @Test
    public void allCornersUseTheirNormalizedActiveAndInactiveGeometry() {
        for (TabPosition position : TabPosition.values()) {
            TabBounds active = switch (position) {
                case TOP_LEFT -> new TabBounds(26, -28, 26, 32);
                case TOP_RIGHT -> new TabBounds(124, -28, 26, 32);
                case BOTTOM_LEFT -> new TabBounds(26, 161, 26, 32);
                case BOTTOM_RIGHT -> new TabBounds(124, 161, 26, 32);
            };
            TabBounds inactive = switch (position) {
                case TOP_LEFT -> new TabBounds(26, -27, 26, 27);
                case TOP_RIGHT -> new TabBounds(124, -27, 26, 27);
                case BOTTOM_LEFT -> new TabBounds(26, 164, 26, 29);
                case BOTTOM_RIGHT -> new TabBounds(124, 164, 26, 29);
            };
            assertEquals(active, TabBounds.at(1, position, 176, 166, ScreenOffset.ZERO, true));
            assertEquals(inactive, TabBounds.at(1, position, 176, 166, ScreenOffset.ZERO, false));
        }
    }

    @Test
    public void customOffsetsMoveHitBoundsWithTheRenderedTab() {
        var bounds = TabBounds.at(0, TabPosition.TOP_LEFT, 176, 166, new ScreenOffset(7, -3), true);
        assertEquals(new TabBounds(7, -31, 26, 32), bounds);
        assertTrue(bounds.contains(7, -31));
        assertTrue(bounds.contains(32.99, 0.99));
        assertFalse(bounds.contains(6.99, -31));
        assertFalse(bounds.contains(7, -31.01));
        assertFalse(bounds.contains(33, 0));
        assertFalse(bounds.contains(7, 1));
    }

    @Test
    public void rightAndBottomAnchorsFollowContainerDimensions() {
        assertEquals(new TabBounds(178, 198, 26, 29),
                TabBounds.at(1, TabPosition.BOTTOM_RIGHT, 230, 200, ScreenOffset.ZERO, false));
    }
}
