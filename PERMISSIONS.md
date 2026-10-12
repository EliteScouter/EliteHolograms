# EliteHolograms Permission System

EliteHolograms supports modern permission systems like **LuckPerms** and **FTB Ranks**, with automatic fallback to vanilla OP system.

## How It Works

1. **Permission System Detected**: Uses permission nodes (recommended)
2. **No Permission System**: Falls back to OP level 2 (vanilla behavior)

## Permission Nodes

| Permission Node | Description | Commands |
|----------------|-------------|----------|
| `eliteholograms.create` | Create new holograms | `/eh create`, `/eh createat`, `/eh createitem`, `/eh createscoreboard`, `/eh copy`, `/eh settheme` |
| `eliteholograms.delete` | Delete holograms | `/eh delete` |
| `eliteholograms.edit` | Edit hologram content and appearance | `/eh addline`, `/eh setline`, `/eh removeline`, `/eh insertline`, `/eh animateline`, `/eh movehere`, `/eh moveto`, `/eh movevertical`, `/eh convert`, `/eh setrotation`, `/eh background`, `/eh display`, `/eh click set`, `/eh click none` |
| `eliteholograms.list` | List holograms, and use the base `/eh` command at all | `/eh list` |
| `eliteholograms.info` | View hologram info | `/eh info` |
| `eliteholograms.near` | View nearby holograms | `/eh near` |
| `eliteholograms.teleport` | Teleport to holograms | `/eh teleport` |
| `eliteholograms.backlight` | Toggle hologram backlights | `/eh backlight` |
| `eliteholograms.admin` | Administrative commands, and running a click command as console | `/eh reload`, `/eh click <id> console <command>` |

> **Two nodes to be careful with.**
>
> `eliteholograms.list` also gates the base `/eh` literal, so a player needs it *in addition to*
> whichever node their subcommand uses. Granting `eliteholograms.create` on its own does nothing.
>
> `/eh click <id> console <command>` deliberately requires `eliteholograms.admin` rather than
> `eliteholograms.edit`, because it runs its command with server permissions. If it used the edit
> node, anyone trusted to write hologram text could attach `op <themselves>` to a hologram and
> click it. `/eh click <id> set` is the safe form: it runs as the clicking player, with only
> that player's own permissions, and stays on the edit node.

## Permission Groups (Recommended)

### Basic User
```
eliteholograms.list
eliteholograms.info
eliteholograms.near
```

### Hologram Builder
```
eliteholograms.create
eliteholograms.edit
eliteholograms.list
eliteholograms.info
eliteholograms.near
eliteholograms.teleport
```

### Hologram Admin
```
eliteholograms.*
```

## Setup Examples

### LuckPerms
```bash
# Create a hologram builder group
/lp creategroup hologram-builder

# Give permissions to the group
/lp group hologram-builder permission set eliteholograms.create true
/lp group hologram-builder permission set eliteholograms.edit true
/lp group hologram-builder permission set eliteholograms.list true
/lp group hologram-builder permission set eliteholograms.info true
/lp group hologram-builder permission set eliteholograms.near true
/lp group hologram-builder permission set eliteholograms.teleport true

# Add a player to the group
/lp user <player> parent add hologram-builder

# Or give permission directly to a player
/lp user <player> permission set eliteholograms.create true
```

### FTB Ranks
```bash
# Set up permissions in the FTB Ranks config or through commands
# Example: Give Builder rank hologram permissions
/ftbranks permission add Builder eliteholograms.create
/ftbranks permission add Builder eliteholograms.edit
/ftbranks permission add Builder eliteholograms.list
```

## Wildcard Permissions

### Full Access
```
eliteholograms.*
```

### Read-Only Access  
```
eliteholograms.list
eliteholograms.info
eliteholograms.near
```

## Fallback Behavior

If **no permission system** is detected:
- **OP Level 2** required for all commands
- Same as vanilla Minecraft operator permissions
- Use `/op <player>` to grant access

## Console Commands

Console/Command blocks always have **full access** regardless of permission settings.

## Troubleshooting

### Commands Not Working
1. Check if permission system is detected in server logs
2. Verify player has correct permission nodes
3. Test with OP to confirm mod is working
4. Check permission system documentation

### Permission System Not Detected
- Ensure LuckPerms/FTB Ranks is properly installed
- Check server logs for detection messages
- Mod will fall back to OP system automatically

## Supported Permission Systems

- **LuckPerms:** full support
- **FTB Ranks:** full support  
- **Vanilla OP:** fallback support
- **Other systems:** can be added on request

## Migration

### From OP-Only to Permission System
1. Install LuckPerms or FTB Ranks
2. Restart server (mod will auto-detect)
3. Set up permission nodes as shown above
4. Remove OP from players who should only have limited access

### Security Note
Permission nodes provide **granular control**, which is much safer than giving full OP access. 