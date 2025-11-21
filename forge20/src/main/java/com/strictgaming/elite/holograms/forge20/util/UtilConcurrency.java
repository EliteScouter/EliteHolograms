package com.strictgaming.elite.holograms.forge20.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 *
 * Utilities for concurrent tasks
 *
 */
public class UtilConcurrency {

    private UtilConcurrency() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets a player by UUID from the server
     *
     * @param uuid The UUID of the player
     * @return The player, or null if not found
     */
    public static ServerPlayer getPlayer(UUID uuid) {
        if (uuid == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }

        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    /**
     * Runs a task asynchronously
     *
     * @param runnable The task to run
     */
    public static void runAsync(Runnable runnable) {
        // Capture the current classloader (ModClassLoader) to ensure classes can be loaded in the background thread
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        
        CompletableFuture.runAsync(() -> {
            // Set the classloader for this background thread
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            try {
                runnable.run();
            } finally {
                // Restore the old classloader
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        });
    }

    /**
     * Runs a task on the main server thread
     *
     * @param runnable The task to run
     */
    public static void runSync(Runnable runnable) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }

        ServerLifecycleHooks.getCurrentServer().execute(runnable);
    }

    /**
     * Gets a value from a supplier, running it on the main server thread
     *
     * @param supplier The supplier to get the value from
     * @param <T> The type of the value
     * @return The value, or null if the server is not running
     */
    public static <T> T getSync(Supplier<T> supplier) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        
        ServerLifecycleHooks.getCurrentServer().execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            return null;
        }
    }
} 