# Yukami Sophisticated Backpack Tab

A Minecraft Forge mod that enhances inventory management by providing dedicated tabs for Sophisticated Backpacks and other container blocks. This mod streamlines access to your carried storage and placed containers, making your adventuring and building experience more efficient.

## Features

- **Backpack Tabs**: Easily access your equipped Sophisticated Backpacks directly from your inventory screen via dedicated tabs
- **Container Block Tabs**: Quick access to placed backpack blocks and other containers
- **Configurable Tab Position**: Choose where tabs appear on your inventory screen
- **Custom Block Support**: Configure additional blocks to have tab functionality as a backup for blocks that can't pass through GUI checks
- **Smart Tab Management**: Intelligent tab switching with proper item handling to prevent duplication or loss

## Requirements

- **Minecraft**: 1.20.1
- **Minecraft Forge**: 47.4.0 or later
- **Dependencies**:
  - [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks)
  - [Sophisticated Core](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core)

## Installation

1. **Install Minecraft Forge**: Ensure you have Minecraft Forge 47.4.0 or later installed
2. **Install Dependencies**: Download and install Sophisticated Backpacks and Sophisticated Core
3. **Download the Mod**: Get the latest version of Yukami Sophisticated Backpack Tab
4. **Install**: Place the downloaded `.jar` file into your Minecraft `mods` folder
5. **Launch**: Start Minecraft with the Forge profile

## Usage

### Basic Functionality

Once installed, tabs will automatically appear when you open your inventory if you have:
- **Equipped backpacks** in your inventory slots
- **Placed backpack blocks** that you've recently interacted with
- **Other supported container blocks** (chests, furnaces, etc.)

Click on any tab to quickly switch between different containers without closing and reopening GUIs.

## Building from Source

If you want to build the mod from source:

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd yukamibackpacktab
   ```

2. **Set up Development Environment**:
   ```bash
   ./gradlew genEclipseRuns  # For Eclipse
   ./gradlew genIdeaRuns     # For IntelliJ IDEA
   ```

3. **Build the Mod**:
   ```bash
   ./gradlew build
   ```

The compiled `.jar` file will be in the `build/libs/` directory.

## Troubleshooting

### Tabs Not Appearing
- Ensure you have Sophisticated Backpacks and Sophisticated Core installed
- Check that you're carrying a backpack or have interacted with a supported container
- Verify your configuration file syntax if using custom blocks

### Items Disappearing
- The mod includes safeguards to prevent item loss during tab switching, if this problem persists, please issue a bug report

### Custom Blocks Not Working
- Ensure the block ID format is correct: `modid:blockname`
- Check that the mod containing the block is loaded
- Verify the block name matches exactly

## License

This project is licensed under the LGPL-3.0-or-later License.

## Credits

- **Author**: Yukami
- **Dependencies**: Sophisticated Backpacks, Sophisticated Core
- **Minecraft Version**: 1.20.1
- **Forge Version**: 47.4.0+

## Contributing

Contributions are welcome! Please ensure any pull requests maintain compatibility with the existing codebase and follow the established code style.