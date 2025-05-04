package com.strictgaming.elite.holograms.api.hologram;

import java.util.List;

/**
 * Builder interface for creating holograms
 */
public interface HologramBuilder {
    
    /**
     * Sets the unique identifier for this hologram
     *
     * @param id The hologram identifier
     * @return The builder instance
     */
    HologramBuilder id(String id);
    
    /**
     * Sets the lines of text for this hologram
     * 
     * @param lines The lines of text
     * @return The builder instance
     */
    HologramBuilder lines(List<String> lines);
    
    /**
     * Sets the lines of text for this hologram
     * 
     * @param lines The lines of text
     * @return The builder instance
     */
    HologramBuilder lines(String... lines);
    
    /**
     * Sets the world where this hologram will be placed
     * 
     * @param world The world name
     * @return The builder instance
     */
    HologramBuilder world(String world);
    
    /**
     * Sets the position of this hologram
     * 
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return The builder instance
     */
    HologramBuilder position(double x, double y, double z);
    
    /**
     * Builds the hologram object
     * 
     * @return The built hologram
     */
    Hologram build();
    
    /**
     * Builds and spawns the hologram in the world
     * 
     * @return The built and spawned hologram
     */
    Hologram buildAndSpawn();
} 