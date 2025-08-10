# Commands

All commands are available via `/eh`, `/hologram`, or `/eliteholograms`. Tab completion is supported for IDs and arguments.

## Create & Edit
- `/eh create <id> <text...>`
- `/eh addline <id> <text...>`
- `/eh setline <id> <line> <text...>`
- `/eh insertline <id> <line> <text...>`
- `/eh removeline <id> <line>`

## Movement & Teleport
- `/eh movehere <id>` – move a hologram to your current position
- `/eh teleport <id>` – teleport to a hologram
- `/eh movevertical <id> <up|down> <amount>`
  - Example: `up 1.5`, `down 0.25`
  - Persists by changing the base Y position

## Scoreboards
- `/eh createscoreboard <id> <objective> [topCount] [updateInterval]`
  - `topCount`: 1–10 (default 5)
  - `updateInterval`: 5–300 seconds (default 30)
  - Works with offline and online entries; time values are auto-formatted

## Info & List
- `/eh list [page]`
- `/eh near [page]`
- `/eh info <id>`

## Admin
- `/eh delete <id>`
- `/eh copy <source_id> <new_id>`
- `/eh reload`

## Permissions
See [Permissions](./permissions.md). If no permission system is present, commands require OP level 2.


