# Elite Holograms

A lightweight and powerful Minecraft mod for creating and managing holographic displays in-game. Inspired by AdvancedHolograms, Elite Holograms runs on Forge, Fabric, and NeoForge across six editions with a shared feature set and command set.

It is server-side only: players join with a vanilla client and still see everything. It also works standalone in single-player worlds and modpacks.

## Features

- Create persistent holograms that stay loaded across server restarts.
- Manage multiple lines (add, insert, remove, set).
- Position control (create at your location or at explicit coordinates, teleport to holograms).
- Move holograms vertically with precise offsets (e.g., up/down 1.5 blocks).
- **Fixed and player-facing display types** - anchor a hologram to a yaw and pitch so it reads like a wall sign, or let it turn to face each viewer (the default).
- **Backlights** - light a hologram with a column of invisible full-bright `minecraft:light` blocks so it stays readable at night, without overwriting player builds.
- **Scoreboard holograms** - display top players from any scoreboard objective, with named themes for styling.
- **Comprehensive built-in placeholder system** with server and player-specific variables, including `%player_rank%`.
- **Advanced permission system** with support for LuckPerms, FTB Ranks, or operator (OP level 2) fallback.
- Easy-to-use commands with **tab completion** and intuitive syntax.
- Performance optimized for servers with **proper shutdown handling** and efficient hologram rendering.
- **Rich text formatting** - RGB gradients, hex colors, and rainbows using MiniMessage, plus legacy `&` codes.
- **Animations** - cycle text lines with custom intervals.
- **Item displays** - show floating items with text.

## Documentation

Full documentation is available in the [Wiki](https://github.com/EliteScouter/EliteHolograms/wiki).

## Supported Versions

| Module | Minecraft | Loader | Minimum loader build | Java | Mod version |
| ------ | --------- | ------ | -------------------- | ---- | ----------- |
| `forge19` | 1.19.2 | Forge | 43 | 17 | `1.19.2-1.2.0` |
| `forge20` | 1.20.1 | Forge | 47 | 17 | `1.20.1-1.2.0` |
| `fabric20` | 1.20.1 | Fabric | Loader 0.15.0 | 17 | `1.20.1-1.2.0` |
| `neo21` | 1.21.1 | NeoForge | 21.1.0 | 21 | `1.21.1-1.2.0` |
| `neo26` | 26.1.2 | NeoForge | 26.1.2.30-beta | 25 | `26.1.2-1.2.0` |
| `neo262` | 26.2 | NeoForge | 26.2.0.57 | 25 | `26.2-1.2.0` |

The **Fabric edition requires [Fabric API](https://modrinth.com/mod/fabric-api)**. The Forge and NeoForge editions have no additional dependencies.

All editions share the same config format and command set, so a world can move between them without converting anything. The one exception is fixed holograms, which need Minecraft 1.20.1 or newer (see the note below).

## Placeholder System

Elite Holograms includes a built-in placeholder system that updates in real-time. No external placeholder API is required.

### Server Placeholders

These show the same information for all players:
- `%players%` - Current online player count.
- `%maxplayers%` - Server maximum players.
- `%tps%` - Server TPS (Ticks Per Second).
- `%uptime%` - Server uptime in HH:MM:SS format.
- `%memory%` - Memory usage in "used/max MB (percentage%)" format.
- `%server_time%` - Real world time in HH:MM:SS format.

### Player-Specific Placeholders

These show different information for each player viewing the hologram:
- `%player%` - Player's display name.
- `%player_rank%` - Player's rank (from LuckPerms/FTB Ranks, or "OP"/"Player").
- `%player_health%` - Current/max health (e.g., "20.0/20.0").
- `%player_level%` - Experience level.
- `%player_world%` - World name (Overworld/Nether/End/custom).
- `%player_coords%` - X, Y, Z coordinates.
- `%player_gamemode%` - Game mode (Creative/Survival/Adventure/Spectator).

### Example Usage

```
/eh create welcome &bWelcome &f%player%&b to the server!
/eh addline welcome &7Your rank: &e%player_rank%
/eh addline welcome &7Players online: &a%players%&7/&a%maxplayers%
/eh addline welcome &7Server TPS: &a%tps%
```

## Commands

**Permissions:** Commands require either specific permission nodes (if using a supported permission mod like LuckPerms or FTB Ranks) or operator (OP level 2) status. See the "Permissions" section below or `PERMISSIONS.md` for details.

| Command                     | Description                                  | Permission Node Suffix |
| --------------------------- | -------------------------------------------- | ---------------------- |
| `/eh create [fixed\|facing] <id> <text...>` | Create a new hologram at your location | `.create`   |
| `/eh createat [fixed\|facing] <id> <x> <y> <z> [text...]` | Create a hologram at explicit coordinates | `.create` |
| `/eh createitem [fixed\|facing] <id> <item> [text...]` | Create a hologram with a floating item above it | `.create` |
| `/eh createscoreboard [fixed\|facing] <id> <objective> [topCount] [updateInterval] [theme]` | Create a scoreboard-based hologram showing the top X players for an objective, refreshing every N seconds | `.create` |
| `/eh copy <source_id> <new_id>`| Copy an existing hologram to a new one    | `.create` (as it creates) |
| `/eh settheme <id> <theme>`  | Restyle an existing scoreboard hologram      | `.create`              |
| `/eh list [page]`           | List all holograms on the server             | `.list`                |
| `/eh near [page]`           | List nearby holograms                        | `.near`                |
| `/eh info <id>`             | Display information about a hologram, including display type and rotation | `.info` |
| `/eh delete <id>`           | Delete a hologram                            | `.delete`              |
| `/eh addline <id> <text...>`| Add a line to a hologram                     | `.edit`                |
| `/eh setline <id> <#> <text...>`| Change the text on a specific line         | `.edit`                |
| `/eh removeline <id> <#>`   | Remove a line from a hologram                | `.edit`                |
| `/eh insertline <id> <#> <text...>`| Insert a line at a specific position  | `.edit`                |
| `/eh animateline <id> <#> <interval> <frame1\|frame2\|...>` | Cycle a line through frames on a timer | `.edit` |
| `/eh movehere <id>`         | Move a hologram to your current location     | `.edit`                |
| `/eh moveto <id> <x> <y> <z>`| Move a hologram to explicit coordinates     | `.edit`                |
| `/eh movevertical <id> <up\|down> <amount>` | Move a hologram up or down by the specified amount (e.g., 1.5) | `.edit` |
| `/eh convert <id> fixed\|face` | Switch a hologram between fixed and player-facing | `.edit`          |
| `/eh setrotation <id> <yaw> [pitch]` | Set the orientation of a fixed hologram | `.edit`             |
| `/eh background <id> <colour\|opacity\|none\|reset>` | Restyle the panel behind a fixed hologram's text | `.edit` |
| `/eh backlight <id> <on\|off\|toggle> [height 1-10]` | Light a hologram with a column of invisible light blocks | `.backlight` |
| `/eh teleport <id>`         | Teleport to a hologram                       | `.teleport`            |
| `/eh reload`                | Reload holograms and scoreboard themes from storage | `.admin`        |

All commands support **tab completion** for hologram IDs and relevant parameters. The base permission is `eliteholograms`. For example, to use `/eh create`, a player would need `eliteholograms.create`.

Note that the base `/eh` literal itself requires `eliteholograms.list`, so a player needs that node in addition to whichever specific node the subcommand uses.

### Fixed vs. Player-Facing Holograms

Holograms come in two display types:

* **`facing`** (the default) - the classic look. The text always turns to face each player, so it
  reads correctly from any angle.
* **`fixed`** - the text is anchored to a yaw and pitch and stays put, like a sign hung on a wall.
  Everyone sees it from the same angle, which is what you want for holograms mounted flat against
  a build, a shop wall, or a portal frame.

Pick the type when you create the hologram:

```
/eh create fixed shop_sign &6Welcome to the Shop!
```

A new fixed hologram is automatically turned to face you. To aim it yourself, or to tilt it:

```
/eh setrotation shop_sign 90
/eh setrotation shop_sign 90 -15
```

Yaw follows the in-game convention - `0` faces south, `90` west, `180` north, `270` east. Pitch
runs from `-90` (tilted up) to `90` (tilted down); leave it out to keep the hologram level.

Already have a hologram you want to pin down? Convert it in place - lines, animations, items and
backlights all carry over:

```
/eh convert shop_sign fixed
/eh convert shop_sign face
```

`/eh info <id>` shows a hologram's current display type and rotation.

> **Forge 1.19.2:** fixed holograms are not available. They are built on `text_display` entities,
> which Minecraft added in 1.19.4, so the 1.19.2 edition supports player-facing holograms only.
> The commands themselves still exist there, so the command set matches the other editions:
> `/eh setrotation` and `/eh create fixed` validate their arguments and then explain the version
> requirement rather than failing as unknown commands, and `/eh convert <id> face` succeeds as a
> no-op because every 1.19.2 hologram is already player-facing. Nothing is created when you ask
> for `fixed`, so a command copied from a newer server never silently produces the wrong thing.

### Scoreboard Holograms

Create a hologram that renders entries from a scoreboard objective (works with online and offline player data):

```
/eh createscoreboard top_time TimePlayed 10 30
/eh createscoreboard top_time TimePlayed 10 30 ocean
```

- **id**: a unique hologram id (e.g., `top_time`)
- **objective**: scoreboard objective name (e.g., `TimePlayed`)
- **topCount** (optional): number of rows to show; default 5; range 1-10
- **updateInterval** (optional): refresh interval in seconds; default 30; range 5-300
- **theme** (optional): a named theme; defaults to the `defaultTheme` set in the theme config

Notes:
- Time-based objectives (e.g., playtime measured in ticks/seconds) are auto-formatted to human-friendly values.
- Entries include players even if they are currently offline.
- Themes ship with `default`, `ocean`, `blood`, `mono`, `gold`, and `rainbow`, and are editable in `scoreboard_themes.json`. Both `<objective>` and `[theme]` tab-complete.
- Use `/eh settheme <id> <theme>` to restyle an existing board, and `/eh reload` to pick up edits to the theme file without restarting.

### Backlights

```
/eh backlight <id> on
/eh backlight <id> on 6
/eh backlight <id> toggle
```

Raises a column of invisible full-bright `minecraft:light` blocks behind the hologram so it stays
readable at night. `height` is the number of blocks in the column, from `1` to `10`, defaulting to
`3`. Only air and water cells are lit, so existing builds are never overwritten. Backlights follow
the hologram when it moves, persist across restarts, and are removed when the hologram is deleted.

### Move Hologram Vertically

```
/eh movevertical <id> up 1.5
/eh movevertical <id> down 0.25
```

Moves the hologram's base Y position by the provided amount and persists to storage.

## Permissions

Elite Holograms features a flexible permission system:

- **Supported Systems:** Automatically detects and integrates with LuckPerms and FTB Ranks.
- **Fallback:** If no supported permission mod is found, commands default to requiring operator (OP level 2) status.
- **Granular Nodes:** Assign specific permissions for different actions. The base node is `eliteholograms`.
  - `eliteholograms.create` - Creating holograms (`/eh create`, `/eh createat`, `/eh createitem`, `/eh createscoreboard`, `/eh copy`, `/eh settheme`).
  - `eliteholograms.delete` - Deleting holograms (`/eh delete`).
  - `eliteholograms.edit` - Modifying existing holograms (`/eh addline`, `/eh setline`, `/eh removeline`, `/eh insertline`, `/eh animateline`, `/eh movehere`, `/eh moveto`, `/eh movevertical`, `/eh convert`, `/eh setrotation`).
  - `eliteholograms.list` - Listing all holograms (`/eh list`). Also required for the base `/eh` command.
  - `eliteholograms.info` - Viewing detailed hologram information (`/eh info`).
  - `eliteholograms.near` - Listing nearby holograms (`/eh near`).
  - `eliteholograms.teleport` - Teleporting to holograms (`/eh teleport`).
  - `eliteholograms.backlight` - Toggling hologram backlights (`/eh backlight`).
  - `eliteholograms.admin` - Administrative actions like `/eh reload`.
- **Console & Command Blocks:** Always have full permission to execute hologram commands.

For a detailed guide on setting up permissions, please see `PERMISSIONS.md`.

## Installation

1. Download the build matching your loader and Minecraft version from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/elite-holograms) or [Modrinth](https://modrinth.com/mod/elite-holograms).
2. Place the JAR file in your `mods` folder. A dedicated server, a single-player instance, and a modpack all work the same way.
3. **Fabric only:** also install [Fabric API](https://modrinth.com/mod/fabric-api). The Fabric edition will not load without it.
4. *(Optional)* Install LuckPerms or FTB Ranks for granular permissions on a server.
5. Restart the server or launch the game.
6. Use `/eh create` to start creating holograms!

## Building

The root Gradle build covers `api`, `forge19`, and `forge20`:

```
./gradlew :forge19:build :forge20:build
```

The Fabric and NeoForge editions are standalone Gradle builds with their own wrappers, because they
need different Gradle and JDK versions (Loom 1.7.x pins `fabric20` to Gradle 8.8 exactly, and the
26.x editions target Java 25). Build each from its own directory, for example:

```
cd neo262 && ./gradlew build
cd fabric20 && ./gradlew build
```

Output jars land in each module's `build/libs`. Use the unclassified jar; the `-slim` variant is the
unshaded one without Adventure bundled.

## Performance

- Optimized background thread management for placeholder updates and hologram visibility.
- Proper server shutdown handling to prevent hanging and ensure data is saved.
- Hologram JSON is written to a temporary file and then replaced, so a crash mid-write will not leave a truncated file.
- Efficient placeholder processing with robust error handling.
- Memory-conscious hologram rendering.

## Storage & Config

Holograms and configuration are stored under the server or instance `config` directory. Alongside the
main hologram store, two files are written for scoreboard support:

- `scoreboard_holograms.json` - the scoreboard boards themselves, including their theme name, backlight state, and column height.
- `scoreboard_themes.json` - the named themes, created pre-populated on first run. Once it exists it is never overwritten, so your edits are preserved; delete it to restore the starters. A `defaultTheme` key controls which theme new boards use when none is specified.

The exact config directory name currently differs between editions, so check the paths the mod logs
on first start rather than assuming. The Forge 1.20.1 and Fabric editions read a `storage_location`
setting from their config file if you want to point them somewhere specific.

## License

This project is licensed under the [MIT License](LICENSE).
