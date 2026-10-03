package com.strictgaming.elite.holograms.fabric21.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 *
 * A utility class for world operations
 *
 */
public class UtilWorld {

    /**
     *
     * Find a world by name
     *
     * @param worldName The name of the world
     * @return The world, or null if not found
     */
    public static Level findWorld(String worldName) {
        if (worldName == null || UtilServer.get() == null) {
            return null;
        }
        
        try {
            ResourceLocation dimensionKey = ResourceLocation.parse(worldName);
            
            // Try to find the world directly by dimension key
            for (ServerLevel level : UtilServer.get().getAllLevels()) {
                if (level.dimension().location().equals(dimensionKey)) {
                    return level;
                }
            }
            
            // If not found, try to get it by name
            return UtilServer.get().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, 
                            dimensionKey));
        } catch (Exception e) {
            System.out.println("[EliteHolograms] Error finding world: " + worldName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * Get the name of a world
     *
     * @param world The world
     * @return The name of the world
     */
    public static String getName(Level world) {
        if (world == null) {
            return "unknown";
        }
        
        return world.dimension().location().toString();
    }
} 