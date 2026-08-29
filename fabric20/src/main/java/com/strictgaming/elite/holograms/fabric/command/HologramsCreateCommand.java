package com.strictgaming.elite.holograms.fabric.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.fabric.hologram.FabricHologram;
import com.strictgaming.elite.holograms.fabric.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.fabric.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Command to create a new hologram at the player's location
 */
public class HologramsCreateCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = context.getArgument("name", String.class);
        
        if (HologramManager.getById(name) != null) {
            player.sendSystemMessage(Component.literal("§cA hologram with that name already exists!"));
            return 0;
        }
        
        // Get player's position
        double x = player.getX();
        double y = player.getY() - 0.5; // Position below player's eye level
        double z = player.getZ();
        
        FabricHologram hologram = new FabricHologram(name, player.level(), 
                                                  new Vec3(x, y, z), 
                                                  30, true, "§eEdit this hologram with /eh addline " + name + " <text>");
        
        player.sendSystemMessage(Component.literal("§aHologram '" + name + "' created successfully!"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute the command with given arguments, defaulting to a player-facing hologram.
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        return executeCommand(context, args, HologramDisplayType.FACING);
    }

    /**
     * Execute the command with an explicit display type.
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args,
                              HologramDisplayType displayType) throws CommandSyntaxException {
        if (args.length < 1) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /eh create [fixed|facing] <id> <text>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = args[0];
        
        if (HologramManager.getById(name) != null) {
            player.sendSystemMessage(Component.literal("§cA hologram with that name already exists!"));
            return 0;
        }
        
        String text;
        if (args.length > 1) {
            StringBuilder textBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                textBuilder.append(args[i]).append(" ");
            }
            text = textBuilder.toString().trim();
        } else {
            text = "§eEdit this hologram with /eh addline " + name + " <text>";
        }
        
        // Get player's position
        double x = player.getX();
        double y = player.getY() - 0.5; // Position below player's eye level
        double z = player.getZ();
        
        // A fixed hologram keeps whatever rotation it is given, so start it facing the creator.
        float yaw = displayType == HologramDisplayType.FIXED ? player.getYRot() - 180.0F : 0.0F;

        FabricHologram hologram = new FabricHologram(name, player.level(), 
                                                  new Vec3(x, y, z), 
                                                  30, true, displayType, yaw, 0.0F, text);
        
        if (displayType == HologramDisplayType.FIXED) {
            player.sendSystemMessage(Component.literal("§aFixed hologram '" + name
                    + "' created, oriented to face you §7(yaw " + Math.round(hologram.getYaw()) + ")"));
        } else {
            player.sendSystemMessage(Component.literal("§aHologram '" + name + "' created successfully!"));
        }
        
        return Command.SINGLE_SUCCESS;
    }
} 