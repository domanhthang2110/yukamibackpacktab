# Yukami's Sophisticated Backpack Tab — Minecraft 26.1.2

A NeoForge client mod that adds tabs for switching between an equipped Sophisticated Backpack and your inventory or the container you opened.

## Requirements

- Minecraft **26.1.2** and Java **25**
- NeoForge **26.1.2.99** or newer within the 26.1.2 series
- Sophisticated Backpacks **3.25.90** or newer for 26.1.2
- Sophisticated Core **1.4.104** or newer for 26.1.2

Use dependency files built for Minecraft 26.1.2. Files for 1.21.1, 26.1, or 26.2 are not interchangeable.

## Installation and usage

Install NeoForge and the two required Sophisticated mods, then place this mod's JAR in your instance's `mods` folder.

This is a client-only mod and does not need to be installed on the server. When switching with an item on the cursor, the client first tries to move it into a free inventory slot. If no empty slot is available, closing the old menu follows vanilla behavior: Minecraft merges the stack into compatible inventory stacks when possible and drops any remainder that cannot fit.

Equip a backpack and open your inventory or a supported container. Click a tab to switch screens.

Clicking the tab for the screen that is already open is a no-op, including while carrying an item.

Press **F9** in an inventory screen to open the offset editor:

- Set a global corner or override it for the current screen.
- Select and drag a tab preview, or move it with the arrow keys (Shift moves 5 pixels).
- Reset clears the selected corner's offset. Save applies edits; Cancel or Escape discards them.
- Both bottom corners use `(0, 0)` as the normal baseline. Version 1 offset files migrate once without moving explicitly saved positions.

Global settings are in `config/yukamibackpacktab-client.toml`. Screen offsets and corner overrides are in `config/yukamibackpacktab/screen_offsets.json`.

## Development

This folder is an independent port of the 1.21.1 project. Generated caches and development worlds were not copied.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

On Linux/macOS use `./gradlew`. The wrapper uses Gradle 9.1.0, and the Foojay resolver can provision a Java 25 toolchain if needed. The first build downloads Minecraft and its development dependencies.

Build output: `build/libs/yukamibackpacktab-26.1.2-2.1.1-neoforge.jar`.

VS Code tasks are available for building and running the client. The launch configuration starts Gradle with `--debug-jvm` and attaches on localhost port 5005; stop the Gradle debug task when finished.

The development client also includes **Just Dire Things 1.6.11** and **Configured 2.7.5**. These are test conveniences, not required dependencies of the distributed mod. Sophisticated dependencies are not bundled into the output JAR.

## Port notes

The port uses the 26.1.2 GUI extraction API, explicit ARGB text colors, foreground rendering in window coordinates, deferred tooltips, GUI strata for editor layering, and the current keyboard and container-input APIs. The panel layout, tab-switch state machine, and offset migration are retained.

## Verification (2026-09-10)

- Offline build passed with all 24 unit tests and no compiler warnings.
- The development client loaded this mod and all four development dependencies, initialized rendering, and reached the main menu.
- Configured logs a provider-loading error but successfully registers this mod's config screen factory. The test mods also emit model/texture warnings; these did not prevent startup.
- In-world interaction and visual checks remain: inventory/backpack/container switching (including a carried stack), all four corners, F9 editor controls, resizing, Save/Cancel, and closing during a switch. Startup testing alone does not verify these behaviors.

## License

GPL-3.0-only. See `LICENSE.txt`.
