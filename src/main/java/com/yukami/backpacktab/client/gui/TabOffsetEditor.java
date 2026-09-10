package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.gui.util.DropdownMenu;
import com.yukami.backpacktab.client.gui.util.EditorGui;
import com.yukami.backpacktab.client.gui.util.ResponsivePanel;
import com.yukami.backpacktab.config.TabConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class TabOffsetEditor {
    private TabOffsetEditor() {}
    private static boolean enabled;
    private static String screenClassName = "";
    private static TabConfig.TabPosition selectedPosition = TabConfig.TabPosition.TOP_LEFT;

    private static TabConfig.TabPosition pendingScreenPosition = TabConfig.TabPosition.TOP_LEFT;

    private static TabConfig.TabPosition pendingGlobalPosition = TabConfig.TabPosition.TOP_LEFT;
    private static boolean dragging;
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static final EditorGui editorGui = new EditorGui();
    private static final List<TabConfig.TabPosition> POSITION_OPTIONS = List.of(TabConfig.TabPosition.values());

    private static final DropdownMenu<Optional<TabConfig.TabPosition>> screenPositionDropdown =
            new DropdownMenu<>(
                    Stream.concat(Stream.of(Optional.<TabConfig.TabPosition>empty()),
                            POSITION_OPTIONS.stream().map(Optional::of)).toList(),
                    Optional.empty(),
                    position -> position.map(TabOffsetEditor::positionLabel).orElse("Use global"),
                    position -> {
                        pendingScreenPosition = position.orElse(null);
                        selectedPosition = effectivePosition();
                    });
    private static final DropdownMenu<TabConfig.TabPosition> globalPositionDropdown =
            new DropdownMenu<>(
                    POSITION_OPTIONS,
                    TabConfig.TabPosition.TOP_LEFT,
                    TabOffsetEditor::positionLabel,
                    position -> {
                        pendingGlobalPosition = position;
                        if (pendingScreenPosition == null) selectedPosition = position;
                    });
    private static final int DROPDOWN_HEADER_HEIGHT = 14;
    private static final int DROPDOWN_OPTION_HEIGHT = 12;
    private static final int PANEL_HEIGHT = 164;
    private static final int STACKED_PANEL_HEIGHT = 188;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_VALUE = 0xFFFFFF55;
    private static final int COLOR_CAPTION = 0xFF55FFFF;
    private static final Map<TabConfig.TabPosition, TabConfig.ScreenOffset> pendingOffsets =
            new EnumMap<>(TabConfig.TabPosition.class);

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isEnabledFor(AbstractContainerScreen<?> screen) {
        return enabled && screen != null && screen.getClass().getSimpleName().equals(screenClassName);
    }

    public static void toggle(Screen screen) {
        if (enabled) {
            cancel();
            return;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            begin(containerScreen);
        }
    }

    public static void begin(AbstractContainerScreen<?> screen) {
        screenClassName = screen.getClass().getSimpleName();

        TabConfig.TabPosition override = TabConfig.getScreenPositionOverride(screenClassName);
        pendingScreenPosition = override;
        pendingGlobalPosition = TabConfig.getTabPosition();
        selectedPosition = effectivePosition();
        dragging = false;
        editorGui.clearButtons();
        screenPositionDropdown.setSelected(Optional.ofNullable(pendingScreenPosition));
        screenPositionDropdown.close();
        globalPositionDropdown.setSelected(pendingGlobalPosition);
        globalPositionDropdown.close();
        pendingOffsets.clear();

        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            TabConfig.ScreenOffset offset = TabConfig.getScreenOffset(screenClassName, position);
            pendingOffsets.put(position, new TabConfig.ScreenOffset(offset.x, offset.y));
        }

        enabled = true;
    }

    public static void cancel() {
        enabled = false;
        dragging = false;
        screenPositionDropdown.close();
        globalPositionDropdown.close();
        pendingOffsets.clear();
    }

    public static boolean handleKeyPressed(KeyEvent event) {
        if (!enabled) {
            return false;
        }

        int step = event.hasShiftDown() ? 5 : 1;
        switch (event.key()) {
            case GLFW.GLFW_KEY_TAB -> {
                cyclePosition();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                save();
                return true;
            }
            case GLFW.GLFW_KEY_R -> {
                resetScreenOffsets();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                cancel();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveSelected(0, -step);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveSelected(0, step);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveSelected(-step, 0);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveSelected(step, 0);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static TabConfig.ScreenOffset getOffset(TabConfig.TabPosition position) {
        return pendingOffsets.getOrDefault(position, TabConfig.ScreenOffset.ZERO);
    }

    public static boolean handleMousePressed(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        double localMouseX = mouseX - screen.getLeftPos();
        double localMouseY = mouseY - screen.getTopPos();

        if (screenPositionDropdown.isOpen()) {
            screenPositionDropdown.handleMousePressed(localMouseX, localMouseY);
            return true;
        }
        if (globalPositionDropdown.isOpen()) {
            globalPositionDropdown.handleMousePressed(localMouseX, localMouseY);
            return true;
        }
        if (screenPositionDropdown.handleMousePressed(localMouseX, localMouseY)) {
            return true;
        }
        if (globalPositionDropdown.handleMousePressed(localMouseX, localMouseY)) {
            return true;
        }
        if (editorGui.click(localMouseX, localMouseY)) {
            return true;
        }

        TabConfig.TabPosition hoveredPosition = getHoveredPosition(screen, mouseX, mouseY);
        if (hoveredPosition == null) {
            return true;
        }

        selectedPosition = hoveredPosition;
        dragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public static boolean handleMouseDragged(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (!dragging) {
            return true;
        }

        int dx = (int) Math.round(mouseX - lastMouseX);
        int dy = (int) Math.round(mouseY - lastMouseY);
        if (dx != 0 || dy != 0) {
            moveSelected(dx, dy);
            lastMouseX += dx;
            lastMouseY += dy;
        }
        return true;
    }

    public static boolean handleMouseReleased(AbstractContainerScreen<?> screen, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        dragging = false;
        return true;
    }

    public static void render(GuiGraphicsExtractor guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            TabRenderer.renderTabsAtPosition(
                    guiGraphics,
                    screen,
                    mouseX,
                    mouseY,
                    position,
                    getOffset(position),
                    position == selectedPosition
            );
        }

        Minecraft minecraft = Minecraft.getInstance();
        TabConfig.ScreenOffset offset = getOffset(selectedPosition);
        int minimumWidth = minimumPanelWidth();
        int labelWidth = Math.max(minecraft.font.width("Current screen"), minecraft.font.width("Global")) + 8;
        int preferredWidth = minimumWidth + labelWidth;
        ResponsivePanel.Layout layout = ResponsivePanel.findLayout(screen, preferredWidth, minimumWidth, 8,
                width -> width < preferredWidth ? STACKED_PANEL_HEIGHT : PANEL_HEIGHT);
        boolean stacked = layout.width() < preferredWidth;
        int extraRow = stacked ? 12 : 0;
        int screenFieldY = 35 + extraRow;
        int globalFieldY = 55 + extraRow * 2;
        int offsetSectionY = 80 + extraRow * 2;
        int selectedY = 92 + extraRow * 2;
        int valuesY = 106 + extraRow * 2;
        int resetY = 122 + extraRow * 2;
        int actionsY = resetY + 14 + 4;
        int panelX = layout.x() - screen.getLeftPos();
        int panelY = layout.y() - screen.getTopPos();
        int innerX = panelX + 6;
        int innerWidth = layout.width() - 12;
        int fieldX = innerX + (stacked ? 0 : labelWidth);
        int fieldWidth = innerWidth - (stacked ? 0 : labelWidth);
        double localMouseX = mouseX - screen.getLeftPos();
        double localMouseY = mouseY - screen.getTopPos();

        editorGui.clearButtons();

        guiGraphics.nextStratum();
        editorGui.panel(guiGraphics, panelX, panelY, layout.width(), layout.height());
        editorGui.label(guiGraphics, fitLabel(screenClassName, innerWidth), innerX, panelY + 6, COLOR_TITLE);
        editorGui.sectionHeader(guiGraphics, "Position", innerX, panelY + 24, innerWidth, COLOR_CAPTION);

        editorGui.label(guiGraphics, "Current screen", innerX, panelY + screenFieldY + (stacked ? -12 : 3), COLOR_CAPTION);
        editorGui.label(guiGraphics, "Global", innerX, panelY + globalFieldY + (stacked ? -12 : 3), COLOR_CAPTION);
        editorGui.sectionHeader(guiGraphics, "Offset", innerX, panelY + offsetSectionY, innerWidth, COLOR_CAPTION);

        editorGui.label(guiGraphics, "Selected: " + positionLabel(selectedPosition),
                innerX, panelY + selectedY, COLOR_VALUE);
        editorGui.label(guiGraphics, fitLabel("X: " + offset.x + "  Y: " + offset.y, innerWidth),
                innerX, panelY + valuesY, COLOR_VALUE);
        editorGui.button(guiGraphics, "Reset", innerX, panelY + resetY, innerWidth, 14,
                localMouseX, localMouseY, TabOffsetEditor::resetScreenOffsets);

        int halfWidth = (innerWidth - 4) / 2;
        editorGui.primaryButton(guiGraphics, "Save", innerX, panelY + actionsY, halfWidth, 16,
                localMouseX, localMouseY, TabOffsetEditor::save);
        editorGui.button(guiGraphics, "Cancel", innerX + halfWidth + 4, panelY + actionsY,
                innerWidth - halfWidth - 4, 16, localMouseX, localMouseY, TabOffsetEditor::cancel);

        int maxVisibleY = screen.height - screen.getTopPos();
        screenPositionDropdown.renderHeader(guiGraphics, minecraft.font, fieldX, panelY + screenFieldY, fieldWidth,
                DROPDOWN_HEADER_HEIGHT, DROPDOWN_OPTION_HEIGHT, localMouseX, localMouseY, maxVisibleY);
        globalPositionDropdown.renderHeader(guiGraphics, minecraft.font, fieldX, panelY + globalFieldY, fieldWidth,
                DROPDOWN_HEADER_HEIGHT, DROPDOWN_OPTION_HEIGHT, localMouseX, localMouseY, maxVisibleY);

        guiGraphics.nextStratum();
        screenPositionDropdown.renderPopup(guiGraphics, minecraft.font, localMouseX, localMouseY);
        globalPositionDropdown.renderPopup(guiGraphics, minecraft.font, localMouseX, localMouseY);
    }

    private static int minimumPanelWidth() {
        var font = Minecraft.getInstance().font;
        int choiceWidth = font.width("Use global");
        int selectedWidth = 0;
        for (TabConfig.TabPosition position : POSITION_OPTIONS) {
            choiceWidth = Math.max(choiceWidth, font.width(positionLabel(position)));
            selectedWidth = Math.max(selectedWidth, font.width("Selected: " + positionLabel(position)));
        }

        int footerWidth = 2 * Math.max(font.width("Save"), font.width("Cancel")) + 20;
        return Math.max(104, 12 + Math.max(choiceWidth + 18, Math.max(footerWidth, selectedWidth)));
    }

    private static String fitLabel(String text, int width) {
        var font = Minecraft.getInstance().font;
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    private static TabConfig.TabPosition effectivePosition() {
        return pendingScreenPosition != null ? pendingScreenPosition : pendingGlobalPosition;
    }

    private static String positionLabel(TabConfig.TabPosition position) {
        return switch (position) {
            case TOP_LEFT -> "Top left";
            case TOP_RIGHT -> "Top right";
            case BOTTOM_LEFT -> "Bottom left";
            case BOTTOM_RIGHT -> "Bottom right";
        };
    }

    private static void cyclePosition() {
        TabConfig.TabPosition[] positions = TabConfig.TabPosition.values();
        selectedPosition = positions[(selectedPosition.ordinal() + 1) % positions.length];
    }

    private static void moveSelected(int dx, int dy) {
        TabConfig.ScreenOffset current = getOffset(selectedPosition);
        pendingOffsets.put(selectedPosition, new TabConfig.ScreenOffset(current.x + dx, current.y + dy));
    }

    private static void resetScreenOffsets() {
        pendingOffsets.put(selectedPosition, TabConfig.ScreenOffset.ZERO);
    }

    private static TabConfig.TabPosition getHoveredPosition(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double localMouseX = mouseX - screen.getLeftPos();
        double localMouseY = mouseY - screen.getTopPos();

        var tabs = TabManager.getActiveTabsView();
        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            for (int i = 0; i < tabs.size(); i++) {
                boolean active = tabs.get(i).isActive();
                TabBounds bounds = TabRenderer.getTabBounds(i, position, screen, getOffset(position), active);
                if (bounds.contains(localMouseX, localMouseY)) {
                    return position;
                }
            }
        }
        return null;
    }

    private static void save() {
        for (Map.Entry<TabConfig.TabPosition, TabConfig.ScreenOffset> entry : pendingOffsets.entrySet()) {
            TabConfig.setScreenOffset(screenClassName, entry.getKey(), entry.getValue());
        }

        if (pendingScreenPosition == null) {
            TabConfig.removeScreenPositionOverride(screenClassName);
        } else {
            TabConfig.setScreenPositionOverride(screenClassName, pendingScreenPosition);
        }

        if (pendingGlobalPosition != TabConfig.getTabPosition()) {
            TabConfig.setGlobalTabPosition(pendingGlobalPosition);
        }

        TabConfig.saveOffsets();
        TabRenderer.invalidateCache();
        cancel();
        sendMessage("Saved offsets & position for " + screenClassName);
    }

    private static void sendMessage(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
        }
    }
}
