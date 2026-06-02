# Changelog

## All Editions - 1.1.0 - Scoreboard themes, backlights, tab-completion & live reload - 2026-06-01

### Added

* **`/eh backlight <id> <on|off|toggle> [height 1-10]`** - lights up a hologram with a vertical column of invisible `minecraft:light` blocks (the same block WorldEdit's `//set light` uses) that rises from the ground directly below the hologram and goes straight up. `1` places a single light block on the ground behind the hologram; `10` places a ten-block-tall pillar. Every block in the column emits at full brightness (15). Defaults to a height of 3 when none is given. Backlights persist across restarts, follow the hologram when it moves or teleports, and are removed automatically when the hologram is deleted
* **`eliteholograms.backlight` permission** - controls who can toggle backlights (OPs always have it)
* The backlight column will not overwrite existing solid world content - cells that are air or water receive a light block (water stays waterlogged), and any cell already holding a non-air, non-water block is skipped
* **Scoreboard holograms now support named themes for styling.** Instead of the fixed orange/yellow/white/green look, server owners can pick a theme that restyles the header, player rows, and the "no data" line. Usage is now `/eh createscoreboard <id> <objective> [topCount] [interval] [theme]`. When no theme is given, the configured default theme is applied so existing styling still works out of the box
* **A new editable config file `config/eliteholograms/scoreboard_themes.json`** ships with starter themes (`default`, `ocean`, `blood`, `mono`, `gold`, `rainbow`) that owners can freely edit or extend with their own entries to match their server theme. The file is created on first run with inline help, is never overwritten once it exists (edits are preserved), and deleting it restores the starters. A `defaultTheme` key controls which theme new boards use when none is specified
* Theme format strings accept legacy `&` colour codes (e.g. `&a`, `&6`, `&l`) as well as MiniMessage tags like `<gradient:#FF0000:#00FF00>text</gradient>` and `<rainbow>text</rainbow>`. Supported tokens are `{objective}` and `{count}` in headers and `{rank}`, `{player}`, `{score}`, and `{time}` in player rows. Themes can define a separate `timePlayer` row format for time-based objectives (playtime etc.); if omitted, the standard `player` format is reused
* **New `/eh settheme <id> <theme>` command** to restyle an existing scoreboard hologram without recreating it. Both arguments tab-complete: `<id>` suggests existing scoreboard holograms and `<theme>` suggests the available theme names. The board re-renders immediately with the new style
* **The `<objective>` argument now tab-completes** every objective currently registered on the server scoreboard, and the `[theme]` argument tab-completes the available theme names, so owners no longer have to remember exact names

### Changed

* **`/eh reload` now refreshes scoreboard themes from disk without a server restart.** The chosen theme name is saved with each scoreboard hologram, so editing `scoreboard_themes.json` and running reload restyles every board using that theme. Boards created before this update keep working using their stored formats

### Fixed

* **The backlight column did not cover the full height of tall holograms (such as a Top-10 scoreboard), leaving the rows nearest the ground dark.** A tall hologram stacks its lines downward from the anchor, so its lowest rows sit at or below the surface, while the old column rose a fixed number of blocks up from the ground and never lit the cells beside those bottom rows. The column now spans the entire hologram - from the ground (or the lowest line, whichever is lower) up through the top line - placing a light in every open cell along the way, with the configured height acting as a minimum. Solid/underground cells are still skipped so player builds are never overwritten
* **Backlights applied to scoreboard holograms were lost on `/eh reload` and server restart.** Scoreboard holograms are stored in their own config file (`scoreboard_holograms.json`) separate from regular holograms, and that file did not record backlight state, so a light column applied to a scoreboard board vanished as soon as the board was recreated. The scoreboard config now saves the backlight enabled flag and column height, and reapplies the light column when the board is recreated on load
* **Hologram armor stands highlighted by Forbidden & Arcanus' Spectral Eye** (NeoForge 1.21.1 & 26.1 only) - the hologram armor stands were tagged with a scoreboard tag (`spectral_vision_unaffected`), but Forbidden & Arcanus checks the `forbidden_arcanus:spectral_vision_unaffected` *entity-type* tag, which is populated by a data pack rather than a per-entity scoreboard tag. Each NeoForge edition now ships that entity-type tag inside its jar (`data/forbidden_arcanus/tags/entity_type/spectral_vision_unaffected.json` adding `minecraft:armor_stand`), so the Spectral Eye no longer reveals holograms. The Forge editions were unaffected

## NeoForge 26.1 - 1.0.0 - 2025-05-01

### Added

* Initial release for Minecraft 26.1 (NeoForge 26.1.2)
* Full port of all features from the NeoForge 1.21.1 edition
* Updated to Java 25, Gradle 9.2.1, and ModDevGradle 2.0.141
* Adapted to Minecraft 26.1 API changes including the new permission system, deobfuscated mappings, and updated network packets

## NeoForge 1.21.1 - 1.0.9 - 2025-04-29

### Fixed

* **Reload command wiping all holograms** - the `/eh reload` command called an async save then immediately cleared the hologram map. The async save would run after the map was already empty, writing 0 holograms to disk and permanently deleting them. Reload now uses a synchronous save and lets `load()` handle the clear internally
* **Placeholders never updating** - server placeholders like `%players%`, `%tps%`, `%uptime%`, and player-specific placeholders like `%player%` were only resolved once when a hologram was created or a player first saw it. Static hologram lines now refresh placeholders every second for all nearby players
* **Excessive config save spam from scoreboard holograms** - scoreboard holograms (e.g. `TimePlayed`) triggered a full config save to disk every time their display updated. For tick-based objectives where scores change constantly, this caused saves every few seconds. Scoreboard display updates now rebuild lines without triggering a save, since the scoreboard config is persisted separately
