package com.yukami.backpacktab.client.gui;

import org.junit.Test;

import static org.junit.Assert.*;

public class TabSwitchStateTest {
    @Test
    public void activeTabDoesNotStartATransition() {
        TabSwitchState state = new TabSwitchState();
        assertFalse(state.tryBegin(true, new Object(), 0));
        assertFalse(state.isPending());
    }

    @Test
    public void duplicateRequestsCannotReplaceOrExtendThePendingRequest() {
        TabSwitchState state = new TabSwitchState();
        Object source = new Object();
        assertTrue(state.tryBegin(false, source, 0));
        assertFalse(state.tryBegin(false, new Object(), 4_000_000_000L));
        assertFalse(state.complete(source));
        assertTrue(state.hasTimedOut(5_000_000_000L));
    }

    @Test
    public void resizingTheSourceMenuDoesNotCompleteTheSwitch() {
        TabSwitchState state = new TabSwitchState();
        Object source = new Object();
        state.tryBegin(false, source, 0);
        assertFalse(state.complete(source));
        assertTrue(state.isPending());
    }

    @Test
    public void successfulSwitchAllowsAnotherImmediatelyWithoutACooldown() {
        TabSwitchState state = new TabSwitchState();
        Object destination = new Object();
        state.tryBegin(false, new Object(), 0);
        assertTrue(state.complete(destination));
        assertTrue(state.tryBegin(false, destination, 1));
    }

    @Test
    public void timeoutRequiresRecoveryBeforeAnotherRequest() {
        TabSwitchState state = new TabSwitchState();
        state.tryBegin(false, new Object(), 0);
        assertFalse(state.hasTimedOut(4_999_999_999L));
        assertTrue(state.hasTimedOut(5_000_000_000L));
        assertFalse(state.tryBegin(false, new Object(), 5_000_000_001L));
        state.reset();
        assertTrue(state.tryBegin(false, new Object(), 5_000_000_002L));
    }

    @Test
    public void closingOrDisconnectingClearsPendingState() {
        TabSwitchState state = new TabSwitchState();
        state.tryBegin(false, new Object(), 0);
        state.reset();
        assertFalse(state.isPending());
        assertFalse(state.hasTimedOut(10_000_000_000L));
        assertFalse(state.complete(new Object()));
    }
}
