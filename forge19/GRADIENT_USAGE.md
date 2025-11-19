# MiniMessage Gradient Support - Usage Guide

EliteHolograms now supports **MiniMessage** formatting tags for gradients, hex colors, and more!

## Features Added

### Gradient Text
```
<gradient:#FF6B35:#F7931E>ELITE HOLOGRAMS</gradient>
<gradient:red:blue:green>Multi-color gradient</gradient>
<gradient:#FF0000:#00FF00>Red to Green</gradient>
```

### Rainbow Text
```
<rainbow>Animated Rainbow Text!</rainbow>
<rainbow:!>Reversed rainbow</rainbow>
```

### Hex Colors
```
<color:#FF6B35>Custom hex color</color>
<#FF6B35>Shorthand hex</color>
```

### Named Colors
```
<red>Red text</red>
<blue>Blue text</blue>
<green>Green text</green>
```

## Backward Compatibility

**Legacy `&` codes still work!** You can mix both syntaxes:

```
/eh create welcome &bWelcome <gradient:#FF6B35:#F7931E>%player%</gradient> &bto the server!
```

This creates:
- Line 1: Blue "Welcome" + orange→pink gradient on player name + blue "to the server!"

## Example Commands

### Simple Gradient
```
/eh create gradient1 <gradient:#FF6B35:#F7931E>Welcome Players!</gradient>
```

### Mixed Legacy + Gradient (both in same line)
```
/eh create mixed &bWelcome <gradient:gold:yellow>%player%</gradient> &bto the server!
/eh addline mixed &7Players online: &a%players%
```

### Rainbow Header
```
/eh create rainbow <rainbow>✦ SPAWN AREA ✦</rainbow>
/eh addline rainbow &7Welcome to the server
```

### Multiple Gradients in One Line
```
/eh create multi <gradient:red:yellow>LEFT</gradient> &f| <gradient:blue:cyan>RIGHT</gradient>
```

### Scoreboard with Gradient
```
/scoreboard objectives add Deaths deathCount
/eh createscoreboard deaths Deaths 5 60
```
Then edit the hologram file to add gradients to the format strings.

## Tips

1. **Gradients work best on longer text** - short text won't show smooth transitions
2. **Use hex colors** for precise control: `<gradient:#FF6B35:#F7931E>`
3. **Named colors** are easier: `<gradient:red:blue>`
4. **Mix syntaxes** freely - one line can have `&` codes and `<gradient>` tags
5. **Test on a dummy hologram first** before applying to important ones

## Common Gradient Combinations

### Fire Effect (Orange → Red)
```
<gradient:#FF6B35:#FF0000>Fire Zone</gradient>
```

### Ocean Effect (Blue → Cyan)
```
<gradient:#0066CC:#00FFFF>Ocean Area</gradient>
```

### Sunset Effect (Pink → Orange → Yellow)
```
<gradient:#FF1493:#FF6B35:#FFD700>Sunset View</gradient>
```

### Forest Effect (Dark Green → Light Green)
```
<gradient:#228B22:#90EE90>Forest Path</gradient>
```

### Galaxy Effect (Purple → Pink → Blue)
```
<gradient:#8B00FF:#FF1493:#4169E1>Starlight</gradient>
```

## Troubleshooting

**Q: My gradient isn't showing?**
- Make sure you're using angle brackets `<>` not square brackets `[]`
- Check for typos in color names/hex codes
- Ensure closing tags match: `</gradient>`, `</rainbow>`, `</color>`

**Q: Can I use gradients with placeholders?**
- Yes! `<gradient:red:blue>Hello %player%</gradient>` works fine

**Q: Legacy codes stopped working?**
- No, they still work! Both `&a` and `<green>` are supported

## Full MiniMessage Documentation

For advanced features (hover text, click actions, etc.), see:
https://docs.adventure.kyori.net/minimessage/format.html

