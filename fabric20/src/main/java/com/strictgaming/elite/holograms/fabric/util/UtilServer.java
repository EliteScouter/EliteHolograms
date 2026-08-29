package com.strictgaming.elite.holograms.fabric.util;

import net.minecraft.server.MinecraftServer;

/**
 * Holds the running server instance.
 *
 * <p>Forge and NeoForge expose {@code ServerLifecycleHooks.getCurrentServer()} for this. Fabric
 * has no equivalent, so the server is captured from the lifecycle events in
 * {@link com.strictgaming.elite.holograms.fabric.FabricHolograms} and handed out here. Every
 * caller already tolerates a null server, which is what this returns before the server starts
 * and after it stops.
 */
public final class UtilServer {

    private static volatile MinecraftServer server;

    private UtilServer() {
    }

    /**
     * @return the running server, or null when no server is running
     */
    public static MinecraftServer get() {
        return server;
    }

    /**
     * Records the server as it starts. Called from the mod initialiser only.
     *
     * @param instance the starting server
     */
    public static void set(MinecraftServer instance) {
        server = instance;
    }

    /**
     * Clears the stored server once it has stopped, so nothing keeps a dead server (and the
     * whole world state behind it) alive on the way out of a single-player session.
     */
    public static void clear() {
        server = null;
    }
}
