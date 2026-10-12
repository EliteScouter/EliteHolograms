# Changelog

## All Editions 1.3.0: Clickable holograms, scale, brightness and billboards (2026-09-29)

### Added

* **Clickable holograms.** `/eh click <id> set <command...>` makes a hologram run a command when a player right-clicks it, `console <command...>` runs it elevated, and `none` turns it off. `%player%` in the command is replaced with the clicking player's name. Use it for warp signs, shop buttons, quest turn-ins, or anything that was previously a sign and a pressure plate. Clicking is backed by a real, invisible `minecraft:interaction` hitbox placed over the text, which is why it works on every loader without a client mod
* **`/eh display <id> scale <0.1-10>`** resizes a fixed hologram's text. Line spacing widens with the scale, so a scaled-up hologram does not overlap its own lines, and the top line stays anchored where the hologram is
* **`/eh display <id> brightness <0-15|auto>`** lights the text regardless of the surrounding light level. `15` makes a hologram readable at night **without placing a single block in the world**, which for most people is what `/eh backlight` was being used for. `auto` restores world lighting
* **`/eh display <id> billboard <fixed|vertical|horizontal|center>`** controls how a fixed hologram turns towards the viewer. `vertical` is worth knowing about: the text turns to follow the player horizontally but never tilts, so it stays readable from any direction without lying on its back when someone flies overhead. `fixed` remains the default
* **`/eh display <id> reset`** restores all three at once

### Changed

* The three display settings are grouped under one `/eh display` command rather than three top-level ones, to keep the `/eh` tab-completion list readable
* **Each save now keeps the previous version** of the file alongside it as `holograms.json.bak` / `scoreboard_holograms.json.bak`, which is restored automatically if the main file is ever found damaged. An unreadable file is kept as `.corrupt-<timestamp>` rather than overwritten, so nothing in it is lost

### Fixed

* **Scoreboard holograms did not keep their background settings.** Boards persist to their own `scoreboard_holograms.json` rather than to `holograms.json`, and the Forge 1.20.1 and Fabric editions never wrote the background colour and opacity there, so a restyled board reverted to 25% black on restart. All editions now persist the full set (background, scale, brightness, billboard and click command) for scoreboard holograms as well as regular ones
* **`/eh movevertical` could be run by any player on Forge 1.20.1 and Fabric.** Every other subcommand declares the permission node it needs when its brigadier literal is built, but `movevertical` was registered and given arguments without ever being added to that chain, and its handler class carried no check of its own. The only gate left was the one on the base `/eh` literal, which is `eliteholograms.list`. Every user of the mod needs that node, so anyone who could run `/eh` at all could move any hologram vertically. It now requires `eliteholograms.edit` like the rest of the editing commands, which is what the documentation already described. The Forge 1.19.2 and all three NeoForge editions build their commands differently and were never affected
* **A hologram or scoreboard in a world that was not loaded was deleted from disk.** On Forge 1.19.2, Forge 1.20.1 and Fabric, a hologram whose world could not be found at startup (a modded dimension whose mod was removed, or one that registers late) was skipped, and the next save, written from memory, erased it from the file for good. It is now kept in the file exactly as it was, written back on every save, and loads again once its world is available. The log names each one and why
* **A hologram in a world that was not loaded placed real blocks and entities in the overworld** (NeoForge). Its world fell back to the overworld, so its backlight's light blocks and its click hitbox were created there at its coordinates. They were invisible but real, and the hitbox was clickable. It is now left unspawned until its world is back, and its world is saved unchanged
* **One hologram that failed to load took every hologram after it with it** (NeoForge). A single bad entry aborted the whole load, so everything later in the file was skipped and then erased by the next save. Each hologram now loads on its own, and one that cannot be read is kept in the file untouched
* **A half-built hologram or board could overwrite the good copy on disk.** Holograms register themselves as they are constructed, so one that failed part way through loading was left in memory with default settings and saved over the original. It is now removed and the original kept
* **Saves were not crash-safe.** `holograms.json` was deleted before its replacement was moved into place, and `scoreboard_holograms.json` (and `holograms.json` on NeoForge) was truncated and rewritten in place, so a crash, kill or power cut at the wrong moment left a missing or half-written file. Every save now writes a complete temporary file, flushes it to disk, and swaps it in with a single atomic move, so an interrupted save leaves either the old version or the new one. The previous version is kept as a backup either way
* **A damaged file was loaded as empty and then overwritten.** If either file could not be parsed, the mod started with no holograms and the next save wrote that emptiness over the recoverable file. A damaged file is now copied aside as `.corrupt-<timestamp>` and the previous good version is restored from `.bak`, which every save now keeps
* **A typo in `scoreboard_themes.json` wiped out every custom theme.** A themes file that existed but could not be parsed was treated the same as a missing one, so the next startup replaced it with the starter themes. It is now left untouched: the built-in themes are used until it is fixed, and `/eh reload` picks up the repaired file
* **Deleting the last scoreboard hologram did not stick.** An empty list of boards was never written, so the last board deleted came back on the next restart
* **A second edit to a scoreboard within five seconds was not saved** (Forge 1.19.2, Forge 1.20.1, Fabric). A cooldown on scoreboard saves dropped it until shutdown, so a crash lost it. The cooldown only existed to absorb the next bug, and is gone
* **Every score change saved every hologram, once per line** (Forge 1.19.2, Forge 1.20.1, Fabric). A board re-rendering its rows queued a full save for each one, so a top-10 board wrote the file eleven times whenever a score moved. Rendering a board no longer saves at all; its settings are saved when they change
* **`/eh reload` overwrote hand edits instead of loading them.** It saved what was in memory before reading the files, so an edit made to either JSON file was replaced by the command meant to load it. It now loads the edited file, and only saves first when nothing was edited outside the game
* **`/eh reload` left the old holograms on screen** (Forge 1.19.2, Forge 1.20.1, Fabric). Reading the file replaced each hologram before the old one was despawned, so the old copies stayed drawn for players, kept their backlight blocks, and kept their click hitboxes in the world. The holograms actually on screen are now the ones despawned
* **Holograms loaded twice on every startup** (NeoForge), the first time without the protection that stops saves running mid-load. Each hologram it built queued a save that could capture a half-loaded registry. They now load once
* **The console and command blocks could not run most editing commands** (Forge 1.19.2, Forge 1.20.1, Fabric). `/eh addline`, `setline`, `insertline`, `removeline`, `delete` and `info` (and `animateline` on 1.19.2) required a player, even though they only used one to reply. They now reply to whoever ran them, so they work from the console, command blocks and RCON as documented
* **Hologram IDs were case-sensitive on NeoForge**, unlike every other edition and unlike the documentation, so `/eh delete Shop` missed a hologram created as `shop` and both could exist side by side. Lookups now ignore case. Two IDs that an older build saved differing only in case both stay reachable
* **Forge 1.19.2: the second world opened in a single-player session showed no holograms.** The background thread that shows holograms to players kept the previous world's shutdown flag and exited immediately
* Hologram line lists and viewer lists were plain lists shared between the server thread, the visibility thread and the saver (Forge 1.20.1, Fabric), so an edit at the wrong moment could abort a save or a visibility pass. They are now safe to read while being edited
* The player listener was registered again every time a single-player world was opened (Forge 1.19.2, Forge 1.20.1), and the click cooldown kept an entry for every player who ever clicked

### Notes

* Scale, brightness and billboard do not apply to a player-facing hologram, although clicking does. Its text is an armor stand nameplate, which the client sizes, turns and lights itself, so the server has no say in any of it. The values are stored either way and take effect the moment the hologram is converted with `/eh convert <id> fixed`
* **`/eh click <id> console ...` requires the admin permission**, not the edit permission that covers the rest of the command. Running a click command elevated would otherwise let anyone who can edit holograms hand themselves operator commands through a sign
* Click hitboxes are real entities, but they are never saved with the chunk. Each one is rebuilt when its hologram comes into range, so they cannot accumulate in the world. A stray one left by an earlier build is removed the first time it is clicked
* Forge 1.19.2 has none of the display or click features because it has no text displays, so there is nothing to scale, light, turn or click. It does receive every storage, reload and console fix above
* For contributors: `runServer` now works in development on Forge 1.19.2, Forge 1.20.1 and all three NeoForge editions. Their dev runs could not see the bundled Adventure and api libraries and failed at startup; release jars were never affected
* The Forge 1.20.1 and Fabric command builders now **fail closed**: a subcommand that reaches the end of the permission chain without naming a node defaults to requiring `eliteholograms.admin` rather than inheriting only the base `/eh` check. That is the bug class `movevertical` fell into, and this makes it impossible to reintroduce by omission

## All Editions 1.2.1: Hologram backgrounds & Forge 1.19.2 command parity (2026-09-06)

### Added

* **`/eh background <id> ...` restyles the panel behind a fixed hologram's text.** Vanilla draws that panel as 25% opaque black, which is the dark box behind the text, and until now there was no way to change it. `colour <name or #RRGGBB>` sets the colour, `opacity <0-100>` sets how solid it is (0 invisible, 100 solid), `none` removes the panel entirely for floating text with no box, and `reset` restores the vanilla look. Colour names and hologram IDs tab-complete
* **`/eh info <id>` now reports the background** colour and opacity alongside the display type and rotation
* Uses the existing **`eliteholograms.edit`** permission

### Fixed

* **Forge 1.19.2: `/eh backlight` reported "Unknown command" even though the feature shipped.** Commands were being registered twice on that edition: `ForgeHolograms` registered the full set, and a leftover `CommandManager` event subscriber registered a second, older set that was missing `backlight`, `settheme` and `list`. Brigadier merges literals that share a name and overwrites the bound handler when it does, so whichever subscriber ran last won, and the older one had no `backlight` entry to look up. The duplicate subscriber has been removed, so all 23 subcommands resolve against a single registry. `/eh settheme` was affected the same way
* **Forge 1.19.2: `/eh movevertical` tab-completed but never ran.** The brigadier node and the command class both existed, but the handler was only ever registered by the duplicate `CommandManager` described above, so the lookup failed at execution time. It is now registered alongside every other subcommand
* **`/eh backlight` with incomplete arguments gave a bare "Unknown or incomplete command".** Neither the `backlight` literal nor its `<id>` argument was executable, so partial input fell off the command tree and Minecraft answered as though the command did not exist. Both are now executable and fall through to the usage message, matching the NeoForge editions
* **`fixed` and `facing` were silently swallowed into hologram text.** `/eh create shop fixed Welcome` produced a hologram whose first line read "fixed Welcome", because the greedy text argument absorbed the keyword. Both keywords are now recognised on `create`, `createat`, `createitem` and `createscoreboard`
* The hardcoded `ForgeHolograms.VERSION` constant had drifted to `1.19.2-1.1.1` while the jar shipped as `1.2.0`. It is now read from the jar manifest

### Added

* **`/eh setrotation` and `/eh convert` now exist on Forge 1.19.2**, so the command set is identical across all five editions. Fixed holograms still cannot be rendered here, so these validate their arguments and then explain that the feature needs Minecraft 1.19.4+, instead of failing as unknown commands. `/eh convert <id> face` succeeds as a no-op, since every 1.19.2 hologram is already player-facing
* Asking for `fixed` on this edition **refuses and explains rather than quietly creating a player-facing hologram**, so a command copied from a 1.20.1 or NeoForge server never silently produces something that looks wrong
* `/eh` help output now lists `setrotation` and `convert`, marked as requiring 1.19.4+

### Notes

* Fixed holograms remain genuinely unavailable on Forge 1.19.2. They are built on `text_display` entities, added in Minecraft 1.19.4, and armor stand nameplates are billboarded by the client so they cannot be pinned to a yaw or pitch. There is no approximation, only clearer reporting

## All Editions 1.2.0: Fixed holograms (2026-08-17)

### Added

* **New Fabric 1.20.1 edition.** Elite Holograms now runs on Fabric, with the same feature set as the Forge 1.20.1 edition: holograms, fixed holograms, item holograms, animated lines, scoreboard leaderboards, themes, backlights, placeholders and every command. It requires Fabric API, is server-side only (players join with a vanilla client), and reads the same `config/elite-holograms/holograms.json` format, so a world can be moved between the Forge and Fabric editions without converting anything. Permissions work through LuckPerms, falling back to operator level 2

* **Fixed holograms** (NeoForge 1.21.1, NeoForge 26.1 & Forge 1.20.1). Holograms can now be anchored to a rotation instead of always turning to face the viewer. A *facing* hologram is the classic armor stand nameplate that spins to follow each player; a *fixed* hologram is a `text_display` entity locked to a yaw and pitch, so it reads like a sign hung on a wall and looks the same to everyone. Facing remains the default, and every hologram created before this update stays facing
* **`fixed|facing` on every create command:** `/eh create fixed <id> <text>`, `/eh createat fixed <id> <x> <y> <z> [text]`, `/eh createitem fixed <id> <item> [text]` and `/eh createscoreboard fixed <id> <objective> [top] [interval] [theme]`. The keyword is optional and goes immediately after the subcommand; leaving it out creates a facing hologram exactly as before. A new fixed hologram is automatically oriented to face whoever created it
* **`/eh convert <id> fixed|face`** switches an existing hologram between the two display types in place, keeping its lines, animations, item, backlight and position. Converting to fixed points the hologram at whoever ran the command, unless a rotation was already set with `/eh setrotation`
* **`/eh setrotation <id> <yaw> [pitch]`** sets the orientation of a fixed hologram. Yaw follows the in-game convention (0 south, 90 west, 180 north, 270 east) and pitch tilts it between -90 and 90. The rotation is stored even on facing holograms, so it applies the moment you convert one to fixed
* **`/eh info <id>` now reports the display type**, and the rotation when the hologram is fixed
* Both new commands use the existing **`eliteholograms.edit`** permission, and tab-complete hologram IDs and display types

### Changed

* **Backlights now stay centred on the hologram.** Light blocks can only sit on whole block cells, so a hologram near a block edge used to be lit noticeably off to one side. The column is now widened onto the neighbouring cell on any axis where the hologram sits within a quarter block of an edge, which caps the offset at a quarter block instead of half. A backlight therefore places up to four columns instead of one, and each column finds its own ground level so widened backlights still sit correctly on uneven terrain
* `/eh create <id> <text>` treats a first argument of `fixed` or `facing` as the display type keyword. If you have a hologram whose ID is literally `fixed` or `facing`, or command blocks that create one, use `/eh create facing fixed <text>` to get the old behaviour

### Fixed

* **Hologram lines could show each other's text.** Armor stand lines, item stands and text display lines each allocated network entity IDs from their own counter, and those counters were seeded with the same starting value, so different entities could be handed the same ID. The client then applied one line's data to another, showing duplicated or wrong text. All hologram entities now draw from a single shared counter
* **Animated holograms could throw a `ConcurrentModificationException`** (NeoForge editions) when a player walked into range during the same tick that an animated line advanced a frame
* **`/eh convert` and `/eh setrotation` ignored `eliteholograms.edit`** on the NeoForge editions, requiring operator level 2 regardless of the permission node

### Notes

* **Forge 1.19.2 does not support fixed holograms.** The feature is built on `text_display` entities, which Minecraft did not add until 1.19.4, so there is no way to provide it on 1.19.2. The 1.19.2 edition still receives the entity ID and backlight fixes above, and its `/eh create` syntax is unchanged

## All Editions 1.1.1: Hologram persistence race fixes (2026-07-09)

### Fixed

* **`/eh reload` and server restarts could wipe most or all holograms from disk** (Forge 1.19.2 & 1.20.1 primarily; NeoForge hardened further). Async saves read the live hologram map when the write actually ran, not when `save()` was called. Reloading cleared that map first, so a pending async save could write `[]` (or only a few holograms) to `holograms.json` and permanently delete the rest. All editions now serialize save/load with a shared lock, expose a synchronous `saveSync()` used by reload and shutdown, and suppress saves while a load is in progress
* **Deserializing holograms during load queued dozens of async saves.** Each `addLine()` / `addAnimatedLine()` call during JSON load triggered `HologramManager.save()`. Those tasks could run mid-load or after a clear and overwrite the config with a partial list. Saves are now no-ops while `loading` is true
* **Forge 1.19.2 `/eh reload` cleared holograms without saving first.** Unlike Forge 1.20.1, the 1.19 reload path despawned and cleared memory then reloaded from disk with no pre-save. If the file was already empty or stale from the race above, everything was lost. Reload now calls `saveSync()` before `load()`
* **Shutdown could still lose holograms to a stale async write.** The server-stop path now saves synchronously under the lock *before* despawning holograms, instead of racing a background save thread against pending async writers
* **Config writes are safer on disk.** Hologram JSON is written to a `.tmp` file and then replaced, so a crash mid-write is less likely to leave an empty or truncated `holograms.json`

## All Editions 1.1.0: Scoreboard themes, backlights, tab-completion & live reload (2026-06-01)

### Added

* **`/eh backlight <id> <on|off|toggle> [height 1-10]`** lights up a hologram with a vertical column of invisible `minecraft:light` blocks (the same block WorldEdit's `//set light` uses) that rises from the ground directly below the hologram and goes straight up. `1` places a single light block on the ground behind the hologram; `10` places a ten-block-tall pillar. Every block in the column emits at full brightness (15). Defaults to a height of 3 when none is given. Backlights persist across restarts, follow the hologram when it moves or teleports, and are removed automatically when the hologram is deleted
* **`eliteholograms.backlight` permission** controls who can toggle backlights (OPs always have it)
* The backlight column will not overwrite existing solid world content. Cells that are air or water receive a light block (water stays waterlogged), and any cell already holding a non-air, non-water block is skipped
* **Scoreboard holograms now support named themes for styling.** Instead of the fixed orange/yellow/white/green look, server owners can pick a theme that restyles the header, player rows, and the "no data" line. Usage is now `/eh createscoreboard <id> <objective> [topCount] [interval] [theme]`. When no theme is given, the configured default theme is applied so existing styling still works out of the box
* **A new editable config file `config/eliteholograms/scoreboard_themes.json`** ships with starter themes (`default`, `ocean`, `blood`, `mono`, `gold`, `rainbow`) that owners can freely edit or extend with their own entries to match their server theme. The file is created on first run with inline help, is never overwritten once it exists (edits are preserved), and deleting it restores the starters. A `defaultTheme` key controls which theme new boards use when none is specified
* Theme format strings accept legacy `&` colour codes (e.g. `&a`, `&6`, `&l`) as well as MiniMessage tags like `<gradient:#FF0000:#00FF00>text</gradient>` and `<rainbow>text</rainbow>`. Supported tokens are `{objective}` and `{count}` in headers and `{rank}`, `{player}`, `{score}`, and `{time}` in player rows. Themes can define a separate `timePlayer` row format for time-based objectives (playtime etc.); if omitted, the standard `player` format is reused
* **New `/eh settheme <id> <theme>` command** to restyle an existing scoreboard hologram without recreating it. Both arguments tab-complete: `<id>` suggests existing scoreboard holograms and `<theme>` suggests the available theme names. The board re-renders immediately with the new style
* **The `<objective>` argument now tab-completes** every objective currently registered on the server scoreboard, and the `[theme]` argument tab-completes the available theme names, so owners no longer have to remember exact names

### Changed

* **`/eh reload` now refreshes scoreboard themes from disk without a server restart.** The chosen theme name is saved with each scoreboard hologram, so editing `scoreboard_themes.json` and running reload restyles every board using that theme. Boards created before this update keep working using their stored formats

### Fixed

* **The backlight column did not cover the full height of tall holograms (such as a Top-10 scoreboard), leaving the rows nearest the ground dark.** A tall hologram stacks its lines downward from the anchor, so its lowest rows sit at or below the surface, while the old column rose a fixed number of blocks up from the ground and never lit the cells beside those bottom rows. The column now spans the entire hologram, from the ground (or the lowest line, whichever is lower) up through the top line, placing a light in every open cell along the way, with the configured height acting as a minimum. Solid/underground cells are still skipped so player builds are never overwritten
* **Backlights applied to scoreboard holograms were lost on `/eh reload` and server restart.** Scoreboard holograms are stored in their own config file (`scoreboard_holograms.json`) separate from regular holograms, and that file did not record backlight state, so a light column applied to a scoreboard board vanished as soon as the board was recreated. The scoreboard config now saves the backlight enabled flag and column height, and reapplies the light column when the board is recreated on load
* **Hologram armor stands highlighted by Forbidden & Arcanus' Spectral Eye** (NeoForge 1.21.1 & 26.1 only). The hologram armor stands were tagged with a scoreboard tag (`spectral_vision_unaffected`), but Forbidden & Arcanus checks the `forbidden_arcanus:spectral_vision_unaffected` *entity-type* tag, which is populated by a data pack rather than a per-entity scoreboard tag. Each NeoForge edition now ships that entity-type tag inside its jar (`data/forbidden_arcanus/tags/entity_type/spectral_vision_unaffected.json` adding `minecraft:armor_stand`), so the Spectral Eye no longer reveals holograms. The Forge editions were unaffected

## NeoForge 26.1: 1.0.0 (2025-05-01)

### Added

* Initial release for Minecraft 26.1 (NeoForge 26.1.2)
* Full port of all features from the NeoForge 1.21.1 edition
* Updated to Java 25, Gradle 9.2.1, and ModDevGradle 2.0.141
* Adapted to Minecraft 26.1 API changes including the new permission system, deobfuscated mappings, and updated network packets

## NeoForge 1.21.1: 1.0.9 (2025-04-29)

### Fixed

* **Reload command wiping all holograms.** The `/eh reload` command called an async save then immediately cleared the hologram map. The async save would run after the map was already empty, writing 0 holograms to disk and permanently deleting them. Reload now uses a synchronous save and lets `load()` handle the clear internally
* **Placeholders never updating.** Server placeholders like `%players%`, `%tps%`, `%uptime%`, and player-specific placeholders like `%player%` were only resolved once when a hologram was created or a player first saw it. Static hologram lines now refresh placeholders every second for all nearby players
* **Excessive config save spam from scoreboard holograms.** Scoreboard holograms (e.g. `TimePlayed`) triggered a full config save to disk every time their display updated. For tick-based objectives where scores change constantly, this caused saves every few seconds. Scoreboard display updates now rebuild lines without triggering a save, since the scoreboard config is persisted separately
