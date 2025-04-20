# Elite Holograms

A lightweight and powerful Minecraft Forge mod for creating and managing holographic displays in-game. This was inspired by AdvancedHolograms.


## Features

- Create persistent holograms that stay loaded across server restarts
- Manage multiple lines (add, insert, remove, set)
- Position control (create at your location, teleport to holograms)
- PlaceholderAPI support for dynamic content (when available)
- Easy-to-use commands with intuitive syntax
- Performance optimized for servers
- Support for Minecraft 1.19.2 and 1.20.1

## Commands

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

## Installation

1. Download the latest version from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/elite-holograms)
2. Place the JAR file in your server's `mods` folder
3. Restart the server
4. Use `/eh create` to start creating holograms!

## License

This project is licensed under the [MIT License](LICENSE).

