package com.strictgaming.elite.holograms.api.manager;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;

/**
 * Factory interface for creating holograms
 */
public interface HologramFactory {
    
    /**
     * Creates a new hologram builder
     * 
     * @return The hologram builder
     */
    HologramBuilder builder();
    
    /**
     * Creates a new hologram instance directly
     * 
     * @param id The hologram ID
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param lines The text lines
     * @return The created hologram
     */
    Hologram createHologram(String id, String world, double x, double y, double z, String... lines);
} 