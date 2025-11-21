package com.strictgaming.elite.holograms.forge20.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.forge20.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge20.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Forge 1.20 implementation of a {@link Hologram}
 */
public class ForgeHologram implements Hologram {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final double HOLOGRAM_LINE_GAP = 0.25;

    private final String id;
    private transient Level world;
    private transient Vec3 position;
    private int range;
    private transient final List<HologramLine> lines;
    private transient final List<UUID> nearbyPlayers;
    private transient long tickCount = 0;

    public ForgeHologram(String id, Level world, Vec3 position, int range, boolean save, String... lines) {
        this.id = id;
        this.world = world;
        this.position = position;
        this.range = range;
        this.lines = Lists.newArrayList();
        this.nearbyPlayers = Lists.newArrayList();

        // Add to HologramManager
        HologramManager.addHologram(this);

        // Add the lines
        this.addLines(lines);

        // Save
        if (save) {
            HologramManager.save();
        }
    }

    /**
     * Tick animated hologram lines
     */
    public void tick() {
        tickCount++;
        
        for (HologramLine line : lines) {
            if (line instanceof AnimatedHologramLine) {
                AnimatedHologramLine animatedLine = (AnimatedHologramLine) line;
                if (animatedLine.tick(tickCount)) {
                    // Frame changed, update for all nearby players
                    for (UUID playerUUID : nearbyPlayers) {
                        ServerPlayer player = UtilConcurrency.getPlayer(playerUUID);
                        if (player != null) {
                            animatedLine.updateForPlayer(player);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void addLines(String... lines) {
        if (lines == null || lines.length == 0) {
            return;
        }

        // Process each line in the original order
        for (String line : lines) {
            this.addLine(line);
        }
    }

    @Override
    public void addLine(String text) {
        if (text == null) {
            return;
        }

        double y = this.position.y;

        if (!this.lines.isEmpty()) {
            y -= this.lines.size() * HOLOGRAM_LINE_GAP;
        }

        // Create a new ArmorStand for the line
        ArmorStand armorStand = new ArmorStand(this.world,
                this.position.x, y, this.position.z);
        
        // Create a hologram line and set its text
        HologramLine line = new HologramLine(armorStand);
        line.setText(text);

        // Add to our list 
        this.lines.add(line);
        
        // Reposition lines and spawn for nearby players
        this.repositionLines();
        
        // Spawn for all nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                line.spawnForPlayer(player);
            }
        }
        
        // Force immediate update packet to ensure text is visible
        // Sometimes just spawning the entity isn't enough if the metadata isn't fully synced
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                line.updateForPlayer(player);
            }
        }
        
        // Ensure that the hologram is saved immediately after adding a line
        HologramManager.save();
    }

    @Override
    public void move(double x, double y, double z) {
        if (this.position == null) {
            return;
        }

        // Update position
        this.position = new Vec3(x, y, z);
        this.repositionLines();

        // Force refresh of visibility to show updated position
        this.refreshVisibility();

        // Save the updated location
        HologramManager.save();
    }

    @Override
    public void setLine(int lineNumber, String text) {
        if (lineNumber < 0 || lineNumber >= this.lines.size()) {
            return;
        }

        HologramLine line = this.lines.get(lineNumber);
        line.setText(text);

        // Update for all nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                line.updateForPlayer(player);
            }
        }

        // Save changes
        HologramManager.save();
    }

    @Override
    public void insertLine(int lineNumber, String text) {
        if (lineNumber < 0 || lineNumber > this.lines.size()) {
            return;
        }

        if (lineNumber == this.lines.size()) {
            this.addLine(text);
            return;
        }

        double y = this.position.y - (lineNumber * HOLOGRAM_LINE_GAP);

        // Create a new line
        ArmorStand armorStand = new ArmorStand(this.world,
                this.position.x, y, this.position.z);
        HologramLine line = new HologramLine(armorStand);
        line.setText(text);

        // Insert the line at the correct position
        this.lines.add(lineNumber, line);
        
        // Update positions of all lines
        this.repositionLines();
        
        // Spawn for all nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                // Spawn the new line
                line.spawnForPlayer(player);
                
                // Update positions for all lines
                for (HologramLine existingLine : this.lines) {
                    existingLine.sendTeleportPacket(player);
                }
            }
        }

        // Save changes
        HologramManager.save();
    }

    @Override
    public void removeLine(int lineNumber) {
        if (lineNumber < 0 || lineNumber >= this.lines.size()) {
            return;
        }

        // Get the line and despawn it
        HologramLine line = this.lines.remove(lineNumber);
        this.despawnLine(line);

        // Reposition remaining lines
        this.repositionLines();

        // Save changes
        HologramManager.save();
    }

    @Override
    public void delete() {
        // First remove from manager
        HologramManager.removeHologram(this);

        // Then despawn for all players
        this.despawn();

        // Save the updated list
        HologramManager.save();
    }

    /**
     * Spawn the hologram (prepare it for display)
     * This is called after loading from storage to ensure entities are initialized
     */
    public void spawn() {
        // Base implementation does nothing - subclasses can override
        // This is mainly for ItemHologram to initialize its item stand
    }

    @Override
    public void despawn() {
        // Despawn visual entities but keep line data
        for (HologramLine line : this.lines) {
            this.despawnLine(line);
        }
        // Clear nearby players list since no one is seeing the hologram anymore
        this.nearbyPlayers.clear();
    }

    private void despawnLine(HologramLine line) {
        if (line == null) {
            return;
        }

        // Despawn for all nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                line.despawnForPlayer(player);
            }
        }
    }

    @Override
    public void teleport(String worldName, double x, double y, double z) {
        Level world = UtilWorld.findWorld(worldName);
        if (world == null) {
            return;
        }
        
        // Call the implementation method
        this.teleport(world, new Vec3(x, y, z));
    }

    @Override
    public Hologram copy(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        LOGGER.debug("Creating copy of hologram '{}' with ID '{}'", this.id, id);
        
        // Create a new hologram with the same position and properties but no lines
        ForgeHologram newHologram = new ForgeHologram(
            id, 
            this.world, 
            this.position, 
            this.range, 
            false  // Don't save yet
        );
        
        // Clear any auto-added lines
        newHologram.lines.clear();
        
        // Get all lines' text and add them in the same order
        for (int i = 0; i < this.lines.size(); i++) {
            HologramLine line = this.lines.get(i);
            String lineText = line.getText();
            LOGGER.debug("Copying line {}: '{}'", i, lineText);
            
            // Create a new line at the same position
            double lineY = newHologram.position.y - (i * HOLOGRAM_LINE_GAP);
            ArmorStand armorStand = new ArmorStand(newHologram.world,
                    newHologram.position.x, lineY, newHologram.position.z);
            
            HologramLine newLine = new HologramLine(armorStand);
            newLine.setText(lineText);
            
            // Add to the new hologram
            newHologram.lines.add(newLine);
        }
        
        // Now save the hologram
        HologramManager.save();
        
        return newHologram;
    }

    public void refreshVisibility() {
        // Clear the nearby players list
        this.nearbyPlayers.clear();

        // The HologramManager thread will handle updating visibility
    }

    private void repositionLines() {
        // Place lines from top to bottom with consistent spacing
        for (int i = 0; i < this.lines.size(); i++) {
            HologramLine line = this.lines.get(i);
            double lineY = this.position.y - (i * HOLOGRAM_LINE_GAP);
            
            // Set position for the line
            line.setPosition(this.position.x, lineY, this.position.z);
            
            // Debug log
            LOGGER.debug("Repositioned line {} to ({}, {}, {})", 
                i, this.position.x, lineY, this.position.z);
        }
    }
    
    /**
     * Add an animated line to the hologram
     * @param frames List of text frames to cycle through
     * @param intervalSeconds Seconds between frame changes
     */
    public void addAnimatedLine(List<String> frames, int intervalSeconds) {
        double y = this.position.y;
        if (!this.lines.isEmpty()) {
            y -= this.lines.size() * HOLOGRAM_LINE_GAP;
        }

        // Create ArmorStand
        ArmorStand armorStand = new ArmorStand(this.world,
                this.position.x, y, this.position.z);
                
        // Create AnimatedHologramLine
        AnimatedHologramLine animatedLine = new AnimatedHologramLine(
            armorStand,
            frames,
            intervalSeconds * 20 // Convert seconds to ticks
        );

        this.lines.add(animatedLine);
        this.repositionLines();
        
        // Spawn for nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                animatedLine.spawnForPlayer(player);
            }
        }
        
        HologramManager.save();
    }
    
    /**
     * Convert an existing line to an animated line
     * @param lineIndex The line index (1-based)
     * @param frames List of text frames to cycle through
     * @param intervalSeconds Seconds between frame changes
     */
    public void setLineAnimated(int lineIndex, List<String> frames, int intervalSeconds) {
        if (lineIndex < 1 || lineIndex > this.lines.size()) {
            return;
        }
        
        int index = lineIndex - 1;
        HologramLine oldLine = this.lines.get(index);
        
        // Despawn old line for all nearby players
        this.despawnLine(oldLine);
        
        // Create new animated line at same position
        // We reuse the position calculation but need a new ArmorStand instance
        // effectively, or just construct it using current position of old line?
        // The old line has an ArmorStand. We can get its position.
        
        double y = this.position.y - (index * HOLOGRAM_LINE_GAP);
        ArmorStand armorStand = new ArmorStand(this.world,
                this.position.x, y, this.position.z);
        
        AnimatedHologramLine newLine = new AnimatedHologramLine(
            armorStand,
            frames,
            intervalSeconds * 20 // Convert seconds to ticks
        );
        
        this.lines.set(index, newLine);
        
        // Spawn new line for all nearby players
        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                newLine.spawnForPlayer(player);
            }
        }
        
        HologramManager.save();
    }

    public List<HologramLine> getLines() {
        return this.lines;
    }

    public List<UUID> getNearbyPlayers() {
        return this.nearbyPlayers;
    }

    public Level getWorld() {
        return this.world;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public int getRange() {
        return this.range;
    }

    @Override
    public void setRange(int range) {
        this.range = range;
        
        // Force refresh of visibility with new range
        this.refreshVisibility();
        
        // Save changes
        HologramManager.save();
    }
    
    @Override
    public double[] getLocation() {
        return new double[] { this.position.x, this.position.y, this.position.z };
    }

    @Override
    public String getWorldName() {
        return UtilWorld.getName(this.world);
    }

    /**
     * Internal method to teleport the hologram to a new world and position
     * 
     * @param world The new world
     * @param position The new position
     */
    public void teleport(Level world, Vec3 position) {
        // Update the world and position
        this.world = world;
        this.position = position;

        // Update all lines' world and position
        for (HologramLine line : this.lines) {
            line.setWorld(world);
        }
        
        // Reposition lines in the new location
        this.repositionLines();

        // Force refresh of visibility
        this.refreshVisibility();

        // Save changes
        HologramManager.save();
    }
}
