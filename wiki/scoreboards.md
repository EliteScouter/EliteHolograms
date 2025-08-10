# Scoreboard Holograms

Create a leaderboard hologram from a scoreboard objective.

## Command

`/eh createscoreboard <id> <objective> [topCount] [updateInterval]`

- `id`: unique hologram ID
- `objective`: scoreboard objective name (e.g., `TimePlayed`)
- `topCount` (optional): 1–10 (default 5)
- `updateInterval` (optional): 5–300 seconds (default 30)

## Behavior

- Includes offline and online player entries.
- Time/number values are rendered in a human-friendly format where applicable.
- Refreshes automatically based on `updateInterval`.

## Storage

- File: `config/eliteholograms/scoreboard_holograms.json`
- Each entry contains: id, world, x/y/z, range, objective, topCount, updateInterval, and optional formats.

