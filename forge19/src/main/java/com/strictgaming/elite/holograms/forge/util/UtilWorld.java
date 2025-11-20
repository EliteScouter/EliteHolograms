package com.strictgaming.elite.holograms.forge.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

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
        if (worldName == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }
        
        try {
            // In 1.19, we need to use a different method to get worlds
            ResourceLocation dimensionKey = new ResourceLocation(worldName);
            
            // Try to find the world directly by dimension key
            for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                if (level.dimension().location().equals(dimensionKey)) {
                    return level;
                }
            }
            
            // If not found, try to get it by name
            return ServerLifecycleHooks.getCurrentServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.Registry.DIMENSION_REGISTRY, 
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
