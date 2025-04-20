package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A simple factory class for command registration
 */
public class CommandFactory {

    private final Map<Class<?>, BiFunction<CommandSourceStack, String[], ?>> injectors = new HashMap<>();

    /**
     * Register an injector for a specific type
     *
     * @param clazz The class to inject
     * @param injector The function to create the object from command arguments
     * @param <T> The type of object to inject
     */
    public <T> void registerInjector(Class<T> clazz, BiFunction<CommandSourceStack, String[], T> injector) {
        this.injectors.put(clazz, injector);
    }

    /**
     * Register a command with the dispatcher
     *
     * @param dispatcher The command dispatcher
     * @param command The main command to register
     */
    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, HologramsCommand command) {
        command.register(dispatcher);
    }

    /**
     * Register a child command
     *
     * @param parent The parent command
     * @param child The child command to register
     */
    public void registerChildCommand(HologramsCommand parent, Object child) {
        String className = child.getClass().getSimpleName();
        
        // Extract command name without the "Holograms" prefix
        String name;
        if (className.startsWith("Holograms")) {
            name = className.substring("Holograms".length()).toLowerCase().replace("command", "");
        } else {
            name = className.toLowerCase().replace("command", "");
        }
        
        System.out.println("Registering command: " + name + " from class " + className);
        parent.registerSubCommand(name, child);
        
        // Don't register with the original name format - this causes confusion
        // Just use the clean command name (create, delete, etc.)
    }

    /**
     * Get an injector for a specific type
     *
     * @param clazz The class to inject
     * @param <T> The type of object to inject
     * @return The injector function
     */
    @SuppressWarnings("unchecked")
    public <T> BiFunction<CommandSourceStack, String[], T> getInjector(Class<T> clazz) {
        return (BiFunction<CommandSourceStack, String[], T>) this.injectors.get(clazz);
    }
} 