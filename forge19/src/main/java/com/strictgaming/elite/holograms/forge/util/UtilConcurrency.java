package com.strictgaming.elite.holograms.forge.util;

import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 * Utility for running tasks on the main thread
 *
 */
public class UtilConcurrency {

    private static final Executor ASYNC_EXECUTOR = Executors.newCachedThreadPool();

    /**
     *
     * Run a task on the main server thread
     *
     * @param runnable The task to run
     */
    public static void runSync(Runnable runnable) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            if (ServerLifecycleHooks.getCurrentServer().isSameThread()) {
                runnable.run();
            } else {
                ServerLifecycleHooks.getCurrentServer().execute(runnable);
            }
        } else {
            runnable.run();
        }
    }
    
    /**
     *
     * Runs a task asynchronously
     *
     * @param runnable The task to run
     */
    public static void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable, ASYNC_EXECUTOR);
    }
} 
