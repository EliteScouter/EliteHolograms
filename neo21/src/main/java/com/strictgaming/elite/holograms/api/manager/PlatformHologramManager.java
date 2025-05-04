package com.strictgaming.elite.holograms.api.manager;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;

import java.io.IOException;

/**
 * Interface for the main hologram manager
 */
public interface PlatformHologramManager {
    
    /**
     * Checks if placeholders are enabled
     * 
     * @return True if placeholders are enabled, false otherwise
     */
    boolean arePlaceholdersEnabled();
    
    /**
     * Gets the hologram factory
     * 
     * @return The hologram factory
     */
    HologramFactory getFactory();
    
    /**
     * Reloads the hologram system
     * 
     * @throws IOException If an error occurs while loading
     */
    void reload() throws IOException;
    
    /**
     * Clears all holograms
     */
    void clear();
    
    /**
     * Creates a new hologram builder
     * 
     * @return The hologram builder
     */
    HologramBuilder builder();
    
    /**
     * Creates a new hologram builder with an ID
     * 
     * @param id The hologram ID
     * @return The hologram builder
     */
    HologramBuilder builder(String id);
    
    /**
     * Creates a new hologram builder with lines
     * 
     * @param lines The text lines
     * @return The hologram builder
     */
    HologramBuilder builder(String... lines);
    
    /**
     * Creates a new hologram builder with position
     * 
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return The hologram builder
     */
    HologramBuilder builder(String world, int x, int y, int z);
} 