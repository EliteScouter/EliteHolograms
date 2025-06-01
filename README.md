# Elite Holograms

A lightweight and powerful Minecraft Forge mod for creating and managing holographic displays in-game. This was inspired by AdvancedHolograms.

## Features

- Create persistent holograms that stay loaded across server restarts
- Manage multiple lines (add, insert, remove, set)
- Position control (create at your location, teleport to holograms)
- **Built-in placeholder system** with server and player-specific variables
- **Secure command system** - only server operators can manage holograms
- Easy-to-use commands with **tab completion** and intuitive syntax
- Performance optimized for servers with **proper shutdown handling**
- Support for Minecraft 1.19.2, 1.20.1, and NeoForge 1.21.1

## Placeholder System

Elite Holograms includes a comprehensive built-in placeholder system that updates in real-time:

### Server Placeholders
These show the same information for all players:
- `%players%` - Current online player count
- `%maxplayers%` - Server maximum players
- `%tps%` - Server TPS (simplified to 20.0)
- `%uptime%` - Server uptime in HH:MM:SS format
- `%memory%` - Memory usage in "used/max MB" format
- `%server_time%` - Real world time in HH:MM:SS format

### Player-Specific Placeholders
These show different information for each player viewing the hologram:
- `%player%` - Player's display name
- `%player_health%` - Current/max health (e.g., "20.0/20.0")
- `%player_level%` - Experience level
- `%player_world%` - World name (Overworld/Nether/End/custom)
- `%player_coords%` - X, Y, Z coordinates
- `%player_gamemode%` - Game mode (Creative/Survival/Adventure/Spectator)

### Example Usage
```
/eh create welcome &bWelcome &f%player%!
/eh addline welcome &7You are in: &f%player_world%
/eh addline welcome &7Players online: &a%players%&7/&a%maxplayers%
/eh addline welcome &7Your coordinates: &f%player_coords%
```

## Commands

**Note:** All commands require operator (OP) permissions for security.

| Command | Description |
|---------|-------------|
| `/eh create <id> <text>` | Create a new hologram at your location |
| `/eh list` | List all holograms on the server |
| `/eh delete <id>` | Delete a hologram |
| `/eh addline <id> <text>` | Add a line to a hologram |
| `/eh setline <id> <line> <text>` | Change the text on a specific line |
| `/eh removeline <id> <line>` | Remove a line from a hologram |
| `/eh movehere <id>` | Move a hologram to your location |
| `/eh near [page]` | List nearby holograms |
| `/eh reload` | Reload holograms from storage |
| `/eh teleport <id>` | Teleport to a hologram |
| `/eh copy <target> <id>` | Copy a hologram |
| `/eh insertline <id> <line> <text>` | Insert a line at a specific position |
| `/eh info <id>` | Display information about a hologram |

All commands support **tab completion** for hologram IDs and parameters.

## Installation

1. Download the latest version from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/elite-holograms)
2. Place the JAR file in your server's `mods` folder
3. Restart the server
4. Use `/eh create` to start creating holograms!

## Security

- Only server operators (OP level 2) can use hologram commands
- Regular players cannot create, modify, or delete holograms
- Built-in protection against unauthorized access

## Performance

- Optimized background thread management
- Proper server shutdown handling to prevent hanging
- Efficient placeholder processing with error handling
- Memory-conscious hologram rendering
- Improved hologram entity synchronization for smoother multiplayer visibility on NeoForge 1.21.1

## Version Support

- **Forge19**: Minecraft 1.19.2 with Forge
- **Forge20**: Minecraft 1.20.x with Forge  
- **Neo21**: Minecraft 1.21.1 with NeoForge

All versions have feature parity and consistent behavior.

## License

This project is licensed under the [MIT License](LICENSE).

