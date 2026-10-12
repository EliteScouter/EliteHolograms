package com.strictgaming.elite.holograms.fabric21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.fabric21.hologram.FabricHologram;
import com.strictgaming.elite.holograms.fabric21.hologram.HologramManager;
import com.strictgaming.elite.holograms.fabric21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Makes a hologram run a command when a player clicks it.
 *
 * <pre>
 *   /eh click &lt;id&gt; set &lt;command...&gt;
 *   /eh click &lt;id&gt; console &lt;command...&gt;
 *   /eh click &lt;id&gt; none
 * </pre>
 *
 * <p>{@code set} runs the command as the clicking player, with their own permissions.
 * {@code console} runs it elevated, which is what you want for something like a warp that
 * players are not otherwise allowed to perform.
 *
 * <p>{@code %player%} in the command is replaced with the clicking player's name.
 *
 * <p>Clicking works by putting a real, invisible {@code minecraft:interaction} hitbox in the
 * world over the hologram's text, so it only applies to holograms that are currently spawned.
 */
public class HologramsClickCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (!UtilPermissions.canEdit(source)) {
            source.sendSystemMessage(Component.literal(
                    "§c§l(!) §cYou don't have permission to use the click command."));
            return 0;
        }

        if (args.length < 2) {
            sendUsage(source);
            return 0;
        }

        String id = args[0];
        String mode = args[1].toLowerCase(Locale.ROOT);

        Hologram hologram = HologramManager.getById(id);

        if (hologram == null) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cHologram '§f" + id + "§c' not found."));
            return 0;
        }

        if (!(hologram instanceof FabricHologram fh)) {
            source.sendSystemMessage(Component.literal(
                    "§c§l(!) §cThis hologram type cannot be made clickable."));
            return 0;
        }

        switch (mode) {
            case "none":
            case "clear": {
                fh.setClickCommand(null, false);
                source.sendSystemMessage(Component.literal(
                        "§a§l(!) §a'§f" + id + "§a' is no longer clickable."));
                return Command.SINGLE_SUCCESS;
            }
            case "set":
            case "console": {
                if (args.length < 3 || args[2].trim().isEmpty()) {
                    sendUsage(source);
                    return 0;
                }

                boolean asConsole = mode.equals("console");

                // Running elevated is a privilege escalation: anyone who can edit holograms could
                // otherwise hand themselves operator commands through a sign. Gate it on admin,
                // not on the edit permission that covers the rest of this command.
                if (asConsole && !UtilPermissions.canAdmin(source)) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cRunning a click command elevated needs the admin permission. "
                                    + "Use §f/eh click " + id + " set ...§c to run it as the player instead."));
                    return 0;
                }

                String command = args[2].trim();

                // Stored without the slash so the dispatcher gets what it expects either way.
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                fh.setClickCommand(command, asConsole);

                final String shown = command;
                source.sendSystemMessage(Component.literal("§a§l(!) §a'§f" + id
                        + "§a' now runs §f/" + shown + "§a when clicked, as §f"
                        + (asConsole ? "console" : "the player") + "§a."));

                if (!fh.hasClickHitbox()) {
                    source.sendSystemMessage(Component.literal(
                            "§7Saved, but '§f" + id + "§7' is not currently spawned, so there is no "
                                    + "hitbox to click yet."));
                }

                return Command.SINGLE_SUCCESS;
            }
            default: {
                sendUsage(source);
                return 0;
            }
        }
    }

    private void sendUsage(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal(
                "§c§l(!) §cUsage: /eh click <id> <set <command...> | console <command...> | none>"));
    }
}
