# Yukami's Sophisticated Backpack Tab — Minecraft 1.21.1

A NeoForge client mod that adds tabs for switching between an equipped Sophisticated Backpack and your inventory or the container you opened.

## Requirements

- Minecraft **1.21.1** and Java **21**
- NeoForge **21.1.248** or newer
- Sophisticated Backpacks **3.25.78** or newer for 1.21.1
- Sophisticated Core **1.4.90** or newer for 1.21.1

Use dependency files built for Minecraft 1.21.1.

## Installation and usage

Install NeoForge and the two required Sophisticated mods, then place this mod's JAR in your instance's `mods` folder.

This is a client-only mod and does not need to be installed on the server. When switching with an item on the cursor, the client first tries to move it into a free inventory slot. If no empty slot is available, closing the old menu follows vanilla behavior: Minecraft merges the stack into compatible inventory stacks when possible and drops any remainder that cannot fit.

Equip a backpack and open your inventory or a supported container. Click a tab to switch screens. Clicking the tab for the screen already open does nothing, including while carrying an item.

Press **F9** in an inventory screen to open the offset editor. It supports global and per-screen corners, dragging, arrow-key movement, reset, save, and cancel. Both bottom corners use `(0, 0)` as the normal baseline. Version 1 offset files migrate once without moving explicitly saved positions.

Global settings are in `config/yukamibackpacktab-client.toml`. Screen offsets and corner overrides are in `config/yukamibackpacktab/screen_offsets.json`.

## Development

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

On Linux/macOS use `./gradlew`.

Build output: `build/libs/yukamibackpacktab-1.21.1-2.1.0-neoforge.jar`.

## License

GPL-3.0-only. See `LICENSE.txt`.
