# Changelog

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
