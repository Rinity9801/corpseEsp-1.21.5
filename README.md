# Corpse ESP for Minecraft 1.21.5

A Fabric mod for Hypixel SkyBlock that highlights corpses in Glacite Mineshafts with colored ESP boxes visible through walls.

## Features

- **Automatic Corpse Detection**: Detects Lapis, Tungsten, Umber, and Vanguard corpses in mineshafts
- **ESP Rendering**: Colored boxes with vertical beacon lines that render through walls
- **Claimed Tracking**: Automatically hides waypoints for corpses you've looted
- **Large Detection Range**: 800 block scan radius to find distant corpses
- **Location-Based Activation**: Only activates when you're in a Glacite Mineshaft

## Corpse Types & Colors

| Corpse Type | Color | Helmet ID |
|-------------|-------|-----------|
| Lapis | Blue | LAPIS_ARMOR_HELMET |
| Tungsten | White | MINERAL_HELMET |
| Umber | Brown | ARMOR_OF_YOG_HELMET, YOG_HELMET |
| Vanguard | Pink | VANGUARD_HELMET |

## Installation

1. Install [Minecraft 1.21.5](https://www.minecraft.net/)
2. Install [Fabric Loader 0.17.3+](https://fabricmc.net/use/)
3. Install [Fabric API 0.128.2+1.21.5](https://modrinth.com/mod/fabric-api)
4. Download the latest release from the [Releases](https://github.com/Rinity9801/corpseEsp-1.21.5/releases) page
5. Place the mod JAR file in your `.minecraft/mods/` folder
6. Launch Minecraft with the Fabric profile

## Requirements

- Minecraft 1.21.5
- Fabric Loader 0.17.3 or higher
- Fabric API 0.128.2+1.21.5

## Usage

The mod works automatically when you enter a Glacite Mineshaft on Hypixel SkyBlock:

1. Join Hypixel SkyBlock
2. Enter the Crystal Hollows and find a Glacite Mineshaft
3. Corpses will be highlighted with colored boxes and vertical beacon lines
4. When you loot a corpse, it will automatically be hidden from the ESP

## Building from Source

```bash
git clone https://github.com/Rinity9801/corpseEsp-1.21.5.git
cd corpseEsp-1.21.5
./gradlew build
```

The built JAR will be located at `build/libs/corpse-1.0-SNAPSHOT.jar`

## License

This project is open source and available under the MIT License.
