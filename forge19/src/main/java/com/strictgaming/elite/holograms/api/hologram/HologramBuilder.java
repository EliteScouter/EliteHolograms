package com.strictgaming.elite.holograms.api.hologram;

/**
 *
 * Builder interface for creating Holograms
 *
 */
public interface HologramBuilder {

    /**
     *
     * Sets the ID of the hologram
     *
     * @param id The hologram ID
     * @return The builder instance
     */
    HologramBuilder id(String id);

    /**
     *
     * Sets the world for the hologram
     *
     * @param worldName The world name
     * @return The builder instance
     */
    HologramBuilder world(String worldName);

    /**
     *
     * Sets the position for the hologram
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return The builder instance
     */
    HologramBuilder position(double x, double y, double z);

    /**
     *
     * Sets the visibility range for the hologram
     *
     * @param range The range in blocks
     * @return The builder instance
     */
    HologramBuilder range(int range);

    /**
     *
     * Adds a line to the hologram
     *
     * @param line The text for the line
     * @return The builder instance
     */
    HologramBuilder line(String line);

    /**
     *
     * Adds multiple lines to the hologram
     *
     * @param lines The text for the lines
     * @return The builder instance
     */
    HologramBuilder lines(String... lines);

    /**
     *
     * Builds and creates the hologram
     *
     * @param save Whether to save the hologram to storage
     * @return The created hologram
     */
    Hologram build(boolean save);
} 
