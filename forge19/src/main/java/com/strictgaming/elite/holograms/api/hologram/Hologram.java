package com.strictgaming.elite.holograms.api.hologram;

import com.strictgaming.elite.holograms.api.exception.HologramException;

/**
 *
 * An interface representing a server hologram
 *
 */
public interface Hologram {

    /**
     *
     * Moves the hologram to the given coordinates in the world with the given name.
     *
     * @param world Name of the new world
     * @param x X pos to move it to
     * @param y Y pos to move it to
     * @param z Z pos to move it to
     */
    void move(String world, double x, double y, double z);

    /**
     *
     * Sets the line at the given index to the given text
     *
     * @param index The line to set
     * @param text The text to set the line to
     */
    void setLine(int index, String text);

    /**
     *
     * Inserts the new line above the line number given
     *
     * @param lineNum The line number to insert above
     * @param line The line to insert
     */
    void insertLine(int lineNum, String line);

    /**
     *
     * Adds a line to the hologram with the given text
     *
     * @param line The new line for the hologram
     */
    void addLine(String line);

    /**
     *
     * Adds the given lines to the hologram
     *
     * @param lines The lines to add
     */
    void addLines(String... lines);

    /**
     *
     * Removes a line from the hologram
     *
     * @param index The index of the line to remove
     * @throws HologramException When index is out of bounds
     */
    void removeLine(int index) throws HologramException;

    /**
     *
     * Removes the lines from the hologram
     *
     * @param indexes The lines to remove
     * @throws HologramException When index is out of bounds
     */
    void removeLines(int... indexes) throws HologramException;

    /**
     *
     * Deletes the hologram from the world
     *
     */
    void delete();

    /**
     *
     * Teleports the hologram to the x, y, z coords in the given world
     *
     * @param worldName The new world
     * @param x The new x
     * @param y The new y
     * @param z The new z
     */
    void teleport(String worldName, double x, double y, double z);
    
    /**
     *
     * Despawns the hologram for all players
     *
     */
    void despawn();
    
    /**
     *
     * Copies the hologram to a new location with a new ID
     *
     * @param newId The new ID for the copy
     * @param world The world to place the copy in
     * @param x The x coordinate for the copy
     * @param y The y coordinate for the copy
     * @param z The z coordinate for the copy
     * @return The new hologram
     */
    Hologram copy(String newId, String world, double x, double y, double z);
    
    /**
     *
     * Gets the ID of the hologram
     *
     * @return The hologram's ID
     */
    String getId();
} 
