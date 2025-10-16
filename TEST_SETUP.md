# Corpse ESP Mod - Testing Guide

## Built Mod Location
The mod has been successfully built and is located at:
```
/Users/alexdong/Documents/corpse/build/libs/corpse-1.0-SNAPSHOT.jar
```

## Installation

### Option 1: Test in Development Environment
Run the Fabric client directly from the project:
```bash
./gradlew runClient
```

### Option 2: Test in Regular Minecraft
1. Make sure you have Minecraft 1.21.5 with Fabric Loader 0.17.3+ installed
2. Install Fabric API 0.128.2+1.21.5 in your mods folder
3. Copy the mod file to your Minecraft mods folder:
   ```bash
   cp build/libs/corpse-1.0-SNAPSHOT.jar ~/.minecraft/mods/
   ```
4. Launch Minecraft with the Fabric 1.21.5 profile

## Testing the Corpse ESP in Singleplayer

Since this mod is designed for Hypixel SkyBlock, you'll need to simulate the conditions in singleplayer:

### Method 1: Using Commands (Requires Cheats)

1. **Create a test world** with cheats enabled
2. **Spawn armor stands** with the correct helmets:

```minecraft
# Spawn a Lapis corpse
/summon armor_stand ~ ~ ~ {CustomName:'{"text":"Armor Stand"}',Invisible:0b,ArmorItems:[{},{},{},{id:"diamond_helmet",count:1,components:{"custom_name":'{"text":"Lapis Armor Helmet"}'}}]}

# Spawn a Tungsten corpse
/summon armor_stand ~3 ~ ~ {CustomName:'{"text":"Armor Stand"}',Invisible:0b,ArmorItems:[{},{},{},{id:"iron_helmet",count:1,components:{"custom_name":'{"text":"Mineral Helmet"}'}}]}

# Spawn an Umber corpse
/summon armor_stand ~6 ~ ~ {CustomName:'{"text":"Armor Stand"}',Invisible:0b,ArmorItems:[{},{},{},{id:"leather_helmet",count:1,components:{"custom_name":'{"text":"Yog Helmet"}'}}]}

# Spawn a Vanguard corpse
/summon armor_stand ~9 ~ ~ {CustomName:'{"text":"Armor Stand"}',Invisible:0b,ArmorItems:[{},{},{},{id:"golden_helmet",count:1,components:{"custom_name":'{"text":"Vanguard Helmet"}'}}]}
```

3. **What you should see:**
   - Blue box for Lapis corpses
   - White box for Tungsten corpses
   - Brown box for Umber corpses
   - Pink box for Vanguard corpses
   - Each corpse should have its name and distance displayed above it

4. **Test claimed corpse tracking:**
   - Send a chat message matching the pattern (requires a mod like ClientCommands or use /tellraw):
   ```minecraft
   /tellraw @s " LAPIS CORPSE LOOT! "
   ```
   - The nearest corpse should disappear from the ESP within 5 blocks

### Method 2: Using Structure Blocks

1. Create armor stands manually with the following setup:
   - Name the armor stand "Armor Stand" using a name tag
   - Make sure it's NOT invisible
   - Give it one of these helmets (with the exact name):
     - Lapis Armor Helmet (for blue highlighting)
     - Mineral Helmet (for white highlighting)
     - Yog Helmet (for brown highlighting)
     - Vanguard Helmet (for pink highlighting)

2. Save the structure and load it to test

### Method 3: Test on Hypixel SkyBlock
The mod is primarily designed for Hypixel SkyBlock Crystal Hollows/Dwarven Mines:
1. Install the mod as described above
2. Join Hypixel SkyBlock
3. Go to Crystal Hollows or Dwarven Mines
4. Look for corpses - they should be highlighted with colored boxes

## Expected Behavior

### Detection
- The mod scans for armor stands every tick
- Only detects armor stands named "Armor Stand" that are not invisible
- Checks the helmet slot for corpse type identification

### Rendering
- Colored boxes appear at corpse locations
- Text label shows corpse type and distance
- Boxes render through walls (ESP style)

### Claimed Tracking
- When you loot a corpse (chat message " CORPSE LOOT! " appears), your position is recorded
- Any corpse within 5 blocks of a claimed position is hidden
- Claimed positions are cleared when you leave the world

## Troubleshooting

### Mod doesn't load
- Check Fabric API is installed
- Check Minecraft version is 1.21.5
- Check fabric loader version is 0.17.3+
- Check the logs at `.minecraft/logs/latest.log` for errors

### No waypoints appear
- Make sure the armor stands have the exact helmet names
- Make sure the armor stands are named "Armor Stand"
- Make sure the armor stands are not invisible
- Try moving closer (within 100 blocks)

### Waypoints don't disappear when looting
- The chat message must match the pattern exactly: " CORPSE LOOT! "
- Make sure you're within 5 blocks of the corpse when the message appears

## Development Testing

To run the mod in development mode:
```bash
./gradlew runClient
```

This launches a Minecraft client with the mod loaded for testing.

## Files Modified/Created

- `src/client/java/forfun/corpse/client/CorpseESP.java` - Main ESP logic
- `src/client/java/forfun/corpse/client/CorpseClient.java` - Event registration
- `src/client/java/forfun/corpse/client/utils/waypoint/Waypoint.java` - Box rendering
- `src/client/java/forfun/corpse/client/utils/waypoint/NamedWaypoint.java` - Text rendering
