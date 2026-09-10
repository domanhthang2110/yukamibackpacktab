package com.yukami.backpacktab.client.gui;

final class TabSwitchState {
    private static final long TIMEOUT_NANOS = 5_000_000_000L;
    private Object sourceMenu;
    private long startedAt;

    boolean tryBegin(boolean alreadyActive, Object menu, long now) {
        if (alreadyActive || isPending() || menu == null) {
            return false;
        }
        sourceMenu = menu;
        startedAt = now;
        return true;
    }

    boolean isPending() {
        return sourceMenu != null;
    }

    boolean complete(Object openedMenu) {
        if (!isPending() || openedMenu == null || openedMenu == sourceMenu) {
            return false;
        }
        reset();
        return true;
    }

    boolean hasTimedOut(long now) {
        return isPending() && now - startedAt >= TIMEOUT_NANOS;
    }

    void reset() {
        sourceMenu = null;
    }
}
