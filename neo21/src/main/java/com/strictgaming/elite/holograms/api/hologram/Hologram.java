package com.strictgaming.elite.holograms.api.hologram;

import java.util.List;
import java.util.UUID;

/**
 * The Hologram interface represents a hologram entity in the world.
 */
public interface Hologram {
    
    /**
     * Gets the unique identifier for this hologram
     *
     * @return The hologram identifier
     */
    String getId();
    
    /**
     * Gets the lines of text in this hologram
     * 
     * @return The list of text lines
     */
    List<String> getLines();
    
    /**
     * Sets the lines of text for this hologram
     * 
     * @param lines The new lines of text
     */
    void setLines(List<String> lines);
    
    /**
     * Sets the lines of text for this hologram
     * 
     * @param lines The new lines of text
     */
    void setLines(String... lines);
    
    /**
     * Gets a specific line of text
     * 
     * @param index The line index
     * @return The text at the specified index
     */
    String getLine(int index);
    
    /**
     * Sets a specific line of text
     * 
     * @param index The line index
     * @param text The new text
     */
    void setLine(int index, String text);
    
    /**
     * Adds a line of text to the hologram
     * 
     * @param text The text to add
     */
    void addLine(String text);
    
    /**
     * Inserts a line of text at the specified index
     * 
     * @param index The index to insert at
     * @param text The text to insert
     */
    void insertLine(int index, String text);
    
    /**
     * Removes a line of text at the specified index
     * 
     * @param index The index to remove
     */
    void removeLine(int index);
    
    /**
     * Gets the world name where this hologram is located
     * 
     * @return The world name
     */
    String getWorld();
    
    /**
     * Gets the X coordinate of this hologram
     * 
     * @return The X coordinate
     */
    double getX();
    
    /**
     * Gets the Y coordinate of this hologram
     * 
     * @return The Y coordinate
     */
    double getY();
    
    /**
     * Gets the Z coordinate of this hologram
     * 
     * @return The Z coordinate
     */
    double getZ();
    
    /**
     * Sets the position of this hologram
     * 
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     */
    void setPosition(String world, double x, double y, double z);
    
    /**
     * Spawns the hologram in the world
     */
    void spawn();
    
    /**
     * Despawns the hologram from the world
     */
    void despawn();
    
    /**
     * Checks if the hologram is currently spawned
     * 
     * @return True if spawned, false otherwise
     */
    boolean isSpawned();
    
    /**
     * Updates the hologram's visual appearance
     */
    void update();
    
    /**
     * Completely deletes the hologram, removing it from the world and from management
     */
    void delete();
} 