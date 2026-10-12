# Elite Holograms

**Elite Holograms** is a lightweight, server-side or single-player Minecraft mod for creating, managing, and customizing floating text (holograms). Perfect for server info, leaderboards, waypoints, dynamic stats, instructions, and decorative text.

Inspired by Advanced Holograms. Supports Forge (1.19.2, 1.20.1), Fabric (1.20.1), and NeoForge (1.21.1, 26.1, 26.2).

The Fabric build requires **Fabric API**. The Forge and NeoForge builds need nothing extra.

For full details, see the [Wiki](https://github.com/EliteScouter/EliteHolograms/wiki).

## New in 1.3.0

*   **Clickable holograms:** `/eh click <id> set <command...>` makes a hologram run a command when a player right-clicks it, and `console <command...>` runs it elevated for warps and kits players cannot run themselves. `%player%` is replaced with the clicker's name. Anything that used to be a sign and a pressure plate can now be a hologram. Works on fixed *and* player-facing holograms, needs no client mod, and `console` is gated on the admin permission so it cannot be used to hand out operator commands. *Not available on Forge 1.19.2.*

*   **`/eh display <id> scale <0.1-10>`:** resize a fixed hologram's text. Line spacing grows with the scale, so a scaled-up hologram never overlaps its own lines.

*   **`/eh display <id> brightness <0-15|auto>`:** light the text regardless of the surrounding light level. `15` makes a hologram readable at night **without placing a single block in the world**, which for most people is what backlights were for.

*   **`/eh display <id> billboard <fixed|vertical|horizontal|center>`:** control how a fixed hologram turns. `vertical` is the one to know: the text follows the player horizontally but never tilts, so it stays readable from any direction without lying on its back when someone flies overhead.

*   **Fixed: scoreboard holograms lost their styling on restart:** leaderboards are stored in their own file, and the Forge 1.20.1 and Fabric editions never wrote background settings there, so a restyled board reverted to the default on restart. Boards now keep their background, scale, brightness, billboard and click command like any other hologram.

*   **Crash-safe saves:** every save writes a complete file and swaps it in atomically, keeping the previous version as a backup. A crash or power cut can no longer leave a missing or half-written hologram file, and a damaged one is restored from its backup automatically

*   **Nothing is deleted behind your back:** a hologram or scoreboard in a dimension that isn't loaded (say, after removing the mod that adds it) is kept in the file untouched and comes back when the dimension does. It used to be erased by the next save

*   **`/eh reload` loads your hand edits:** it used to save what was in memory first, overwriting the very edit it was meant to load

*   **Deleting your last scoreboard now sticks:** it used to come back on the next restart

*   **Console and command blocks:** `/eh addline`, `setline`, `insertline`, `removeline`, `delete` and `info` now work from the console and command blocks on Forge and Fabric, as documented

## New in 1.2.1

*   **`/eh background <id> ...`:** restyle the panel behind a fixed hologram's text. Vanilla draws it as 25% opaque black; now `colour <name or #RRGGBB>` recolours it, `opacity <0-100>` sets how solid it is, `none` removes it entirely for floating text with no box, and `reset` restores the vanilla look.

*   **Forge 1.19.2 command parity:** `/eh backlight`, `/eh settheme`, `/eh list` and `/eh movevertical` were being registered twice on that edition, and whichever registration ran last won, so several commands reported "Unknown command" despite shipping. All 23 subcommands now resolve against a single registry.

*   **Fixed: `fixed` and `facing` were swallowed into hologram text:** `/eh create shop fixed Welcome` produced a hologram reading "fixed Welcome". Both keywords are now recognised on every create command.

## New in 1.2.0

*   **Fabric 1.20.1 edition:** Elite Holograms now runs on Fabric with the same feature set as the Forge 1.20.1 build: holograms, fixed holograms, item holograms, animated lines, scoreboard leaderboards, themes, backlights, placeholders, and every command. It requires Fabric API, is server-side only, and reads the same hologram config format, so a world can move between the Forge and Fabric builds without converting anything.

*   **NeoForge 26.2 edition:** Full feature parity on Minecraft 26.2, built against NeoForge 26.2.0.57 on Java 25.

*   **Fixed Holograms:** Holograms can be anchored to a rotation instead of always turning to face the viewer. A *fixed* hologram reads like a sign hung on a wall and looks the same to everyone, which is what you want for anything mounted flat against a build. Add `fixed` to any create command, aim it with `/eh setrotation <id> <yaw> [pitch]`, or switch an existing one over with `/eh convert <id> fixed`. Player-facing stays the default and nothing you already built changes. *Requires Minecraft 1.20.1 or newer, so it is not available on the Forge 1.19.2 build.*

*   **Centred backlights:** A hologram sitting near a block edge used to be lit noticeably off to one side, because light blocks can only occupy whole block cells. The light column now widens onto the neighbouring cell when needed, keeping the glow centred on the text.

*   **Fixed: hologram lines could show each other's text:** Different hologram entity types were handing out overlapping network IDs, which made the client apply one line's text to another.

## Also new since 1.0

*   **Backlights:** `/eh backlight <id> <on|off|toggle> [height 1-10]` raises a column of full-bright `minecraft:light` blocks behind a hologram so it stays readable at night. Backlights follow the hologram when it moves, persist across restarts, and never overwrite player builds (only air and water cells are lit).

*   **Scoreboard Themes:** Restyle leaderboards with named themes. Ships with `default`, `ocean`, `blood`, `mono`, `gold`, and `rainbow`, all editable in `config/eliteholograms/scoreboard_themes.json`. Add your own to match your server brand.

*   **`/eh settheme <id> <theme>`:** Re-skin an existing scoreboard instantly, no need to recreate it. Both arguments tab-complete.

*   **Live reload:** `/eh reload` refreshes scoreboard themes from disk without a server restart.

*   **Smarter tab completion:** The `<objective>` argument suggests every objective registered on the server, and `[theme]` suggests the available theme names.

## Key Features

*   **MiniMessage & Gradients:** Full Kyori MiniMessage support for gradients, hex colors, rainbows, and rich formatting on every line. Legacy `&` codes work too.

*   **Persistent Holograms:** Survive restarts with synchronous saves on shutdown.

*   **Easy Commands + Tab Completion:** Intuitive syntax with Brigadier suggestions for IDs, objectives, themes, and display types.

*   **Multi-Line Management:** Add, insert, remove, and set individual text lines.

*   **Item Holograms:** `/eh createitem <id> <item> <text|line|line>` spawns a floating item above the caption, ideal for shops and showcases.

*   **Scoreboard Leaderboards:** Display the top players from any objective with custom row counts, refresh intervals, and themes. Online and offline players are included, and time and number objectives are auto-formatted.

*   **Animated Lines:** `/eh animateline <id> <line#> <interval> <frame1|frame2|...>` rotates text on a timer for countdowns, tips, and announcements.

*   **Backlights:** Light holograms at night without touching the surrounding build.

*   **Fixed Holograms:** Anchor a hologram to a rotation so it reads like a wall sign (1.20.1 and newer).

*   **Clickable Holograms:** Run a command when a player right-clicks a hologram, as the player or elevated as the console (1.20.1 and newer).

*   **Display Control:** Scale a fixed hologram's text, light it to any brightness with no blocks placed, recolour the panel behind it, and choose how it turns towards the viewer.

*   **Position Control:** Create at your location or at explicit coordinates, teleport to any hologram, and nudge vertically with `/eh movevertical`.

*   **Advanced Permissions:** Works with **LuckPerms** and **FTB Ranks**, with OP level 2 fallback and granular nodes.

*   **Built-in Placeholder System:** Real-time server and player stats without external APIs.

*   **Nearby Discovery:** `/eh near` shows holograms around you.

*   **Performance-Optimized:** Separate manager thread, graceful shutdown, minimal overhead.

*   **Cross-Platform & Server-Side Only:** Forge 1.19.2, Forge 1.20.1, Fabric 1.20.1, NeoForge 1.21.1, NeoForge 26.1, and NeoForge 26.2. Players join with a vanilla client, and the mod also works on its own in single-player.

## Quick Command Reference

Every create command takes an optional `fixed` or `facing` keyword immediately after the subcommand. Leave it out and you get a player-facing hologram, which is the default.

**Creating**

*   `/eh create <id> <text...>`: create a hologram at your location

*   `/eh createat <id> <x> <y> <z> [text...]`: create one at explicit coordinates

*   `/eh createitem <id> <item> <text|line|line>`: create one with a floating item on top

*   `/eh createscoreboard <id> <objective> [topCount] [interval] [theme]`: create a leaderboard from a scoreboard objective

**Lines**

*   `/eh addline`, `/eh setline`, `/eh insertline`, `/eh removeline`: manage individual lines

*   `/eh animateline <id> <line#> <interval> <frame1|frame2|...>`: animate a line on a timer

**Appearance**

*   `/eh settheme <id> <theme>`: restyle an existing scoreboard hologram

*   `/eh backlight <id> <on|off|toggle> [height 1-10]`: toggle a light column behind a hologram

*   `/eh convert <id> fixed|face`: switch a hologram between fixed and player-facing

*   `/eh setrotation <id> <yaw> [pitch]`: aim a fixed hologram (yaw 0 south, 90 west, 180 north, 270 east)

*   `/eh background <id> <colour|opacity|none|reset>`: restyle the panel behind a fixed hologram's text

*   `/eh display <id> <scale|brightness|billboard|reset>`: resize, light, or change how a fixed hologram turns

**Interaction**

*   `/eh click <id> set <command...>`: run a command as the clicking player

*   `/eh click <id> console <command...>`: run it elevated (needs the admin permission)

*   `/eh click <id> none`: stop the hologram being clickable

**Position**

*   `/eh movehere <id>`: move a hologram to you

*   `/eh movevertical <id> <up|down> <amount>`: nudge it up or down, e.g. `up 1.5`

*   `/eh teleport <id>`: teleport to a hologram

**Browsing and admin**

*   `/eh list`, `/eh near [page]`, `/eh info <id>`: browse and inspect holograms

*   `/eh delete <id>`: remove a hologram

*   `/eh reload`: reload holograms and themes from storage

## Placeholder Highlights

No external API needed. Placeholders update in real time, including per-viewer player values.

### Server-Wide

*   `%players%`, `%maxplayers%`, `%tps%`, `%uptime%`, `%memory%`, `%server_time%`

### Player-Specific

*   `%player%`, `%player_rank%`, `%player_health%`, `%player_level%`, `%player_world%`, `%player_coords%`, `%player_gamemode%`

*Example:*

```
/eh create welcome <gradient:#00FFA3:#00B7FF>Welcome %player%!</gradient>
/eh addline welcome &7Your rank: &e%player_rank%
/eh addline welcome &7Online: &a%players%&7/&a%maxplayers%  TPS: &a%tps%
```

## Permissions Overview

*   Integrates with **LuckPerms** and **FTB Ranks**, falling back to OP level 2 when neither is present. Console and command blocks always have full access.

*   Key nodes: `eliteholograms.create`, `eliteholograms.delete`, `eliteholograms.edit`, `eliteholograms.list`, `eliteholograms.info`, `eliteholograms.near`, `eliteholograms.teleport`, `eliteholograms.backlight`, `eliteholograms.admin`.

*   See the [Permissions Guide](https://github.com/EliteScouter/EliteHolograms/blob/main/PERMISSIONS.md) for group setups and examples.

## Installation

1.  Download the build matching your loader and Minecraft version.

2.  Drop the JAR into your `mods` folder. Your single-player instance, modpack, or server all work the same way.

3.  **Fabric only:** also install Fabric API. The Fabric build will not load without it.

4.  *(Optional)* Install LuckPerms or FTB Ranks for granular permissions on a server.

5.  Launch the game or restart the server, then run `/eh create` to get started.

*Screenshot:*

![Elite Holograms](https://media.forgecdn.net/attachments/description/1245528/description_2616a837-92f6-4204-891c-600de45fb7b5.png)

![Elite Holograms in action](https://media.forgecdn.net/attachments/description/1245528/description_8c6f023a-5b8a-4470-bb65-8b75075a540a.png)

Designed for Forge 1.19.2 / 1.20.1, Fabric 1.20.1, and NeoForge 1.21.1 / 26.1 / 26.2.
