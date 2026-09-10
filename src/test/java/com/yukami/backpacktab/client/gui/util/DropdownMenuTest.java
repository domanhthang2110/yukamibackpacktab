package com.yukami.backpacktab.client.gui.util;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DropdownMenuTest {
    private DropdownMenu<String> menu(AtomicInteger changes, int maxVisibleY) {
        DropdownMenu<String> menu = new DropdownMenu<>(List.of("A", "B", "C", "D"), "A",
                value -> value, value -> changes.incrementAndGet());
        menu.setBounds(100, 100, 100, 14, 12, maxVisibleY);
        menu.handleMousePressed(110, 105);
        return menu;
    }

    @Test
    public void clickingOutsidePopupDismissesWithoutSelecting() {
        AtomicInteger changes = new AtomicInteger();
        DropdownMenu<String> menu = menu(changes, 300);
        assertTrue(menu.handleMousePressed(500, 140));
        assertFalse(menu.isOpen());
        assertEquals("A", menu.getSelected());
        assertEquals(0, changes.get());
    }

    @Test
    public void renderedRowsAndSelectionHaveTheSameBoundaries() {
        AtomicInteger changes = new AtomicInteger();
        DropdownMenu<String> menu = menu(changes, 300);
        menu.handleMousePressed(110, 126); // Last pixel of A, after the 1px top border.
        assertEquals("A", menu.getSelected());
        menu.handleMousePressed(110, 105);
        menu.handleMousePressed(110, 127); // First pixel of B.
        assertEquals("B", menu.getSelected());
        assertEquals(1, changes.get());
    }

    @Test
    public void popupBordersDoNotSelectOptions() {
        AtomicInteger changes = new AtomicInteger();
        for (int[] point : new int[][] {{100, 140}, {199, 140}, {110, 114}, {110, 163}}) {
            DropdownMenu<String> menu = menu(changes, 300);
            menu.handleMousePressed(point[0], point[1]);
            assertEquals("A", menu.getSelected());
            assertFalse(menu.isOpen());
        }
        assertEquals(0, changes.get());
    }

    @Test
    public void upwardPopupUsesTheSameHitTesting() {
        DropdownMenu<String> menu = menu(new AtomicInteger(), 120);
        menu.handleMousePressed(110, 63); // Upward popup starts at 50; B starts at 63.
        assertEquals("B", menu.getSelected());
    }

    @Test
    public void selectingUseGlobalRemovesTheExplicitChoice() {
        DropdownMenu<Optional<String>> menu = new DropdownMenu<>(
                List.of(Optional.empty(), Optional.of("Top left")), Optional.of("Top left"),
                value -> value.orElse("Use global default"), value -> {});
        menu.setBounds(100, 100, 100, 14, 12, 300);
        menu.handleMousePressed(110, 105);
        menu.handleMousePressed(110, 115);
        assertTrue(menu.getSelected().isEmpty());
    }
}
