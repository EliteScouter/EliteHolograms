package com.strictgaming.elite.holograms.fabric.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.fabric.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.fabric.hologram.entity.AnimatedTextDisplayHologramLine;
import com.strictgaming.elite.holograms.fabric.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.fabric.hologram.entity.HologramLineRenderer;
import com.strictgaming.elite.holograms.fabric.hologram.entity.TextDisplayHologramLine;
import com.strictgaming.elite.holograms.fabric.util.UtilBacklight;
import com.strictgaming.elite.holograms.fabric.util.UtilConcurrency;
import com.strictgaming.elite.holograms.fabric.util.UtilWorld;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
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
public class FabricHologram implements Hologram {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final double HOLOGRAM_LINE_GAP = 0.25;

    private final String id;
    private transient Level world;
    private transient Vec3 position;
    private int range;
    private transient final List<HologramLineRenderer> lines;
    private transient final List<UUID> nearbyPlayers;
    private transient long tickCount = 0;

    // How the lines are rendered, and the orientation used when that is FIXED
    private HologramDisplayType displayType = HologramDisplayType.FACING;
    private float yaw = 0.0F;
    private float pitch = 0.0F;

    // Backlight state - places invisible minecraft:light blocks at the hologram
    private boolean backlightEnabled = false;
    private int backlightLevel = UtilBacklight.DEFAULT_LEVEL;
    private transient List<BlockPos> backlightPositions = new ArrayList<>();

    public FabricHologram(String id, Level world, Vec3 position, int range, boolean save, String... lines) {
        this(id, world, position, range, save, HologramDisplayType.FACING, 0.0F, 0.0F, lines);
    }

    public FabricHologram(String id, Level world, Vec3 position, int range, boolean save,
                         HologramDisplayType displayType, float yaw, float pitch, String... lines) {
        this.id = id;
        this.world = world;
        this.position = position;
        this.range = range;
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);
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
        
        for (HologramLineRenderer line : lines) {
            if (line.isAnimated() && line.tickAnimation(tickCount)) {
                // Frame changed, update for all nearby players
                for (UUID playerUUID : nearbyPlayers) {
                    ServerPlayer player = UtilConcurrency.getPlayer(playerUUID);
                    if (player != null) {
                        line.updateForPlayer(player);
                    }
                }
            }
        }
    }

    /**
     * Builds a single line entity for this hologram's display type.
     *
     * @param lineY the Y coordinate of this line's slot
     * @param text  the line text
     * @return the line renderer
     */
    private HologramLineRenderer createLine(double lineY, String text) {
        HologramLineRenderer line;

        if (this.displayType == HologramDisplayType.FIXED) {
            line = new TextDisplayHologramLine(this.world, this.position.x, lineY, this.position.z,
                    this.yaw, this.pitch);
        } else {
            ArmorStand armorStand = new ArmorStand(this.world, this.position.x, lineY, this.position.z);
            line = new HologramLine(armorStand);
        }

        line.setText(text);
        return line;
    }

    /**
     * Builds a single animated line entity for this hologram's display type.
     *
     * @param lineY         the Y coordinate of this line's slot
     * @param frames        the text frames to cycle through
     * @param intervalTicks ticks between frame changes
     * @return the line renderer
     */
    private HologramLineRenderer createAnimatedLine(double lineY, List<String> frames, int intervalTicks) {
        if (this.displayType == HologramDisplayType.FIXED) {
            return new AnimatedTextDisplayHologramLine(this.world, this.position.x, lineY, this.position.z,
                    this.yaw, this.pitch, frames, intervalTicks);
        }

        ArmorStand armorStand = new ArmorStand(this.world, this.position.x, lineY, this.position.z);
        return new AnimatedHologramLine(armorStand, frames, intervalTicks);
    }

    /**
     * @return the Y coordinate for the line sitting at {@code index}
     */
    private double lineY(int index) {
        return this.position.y - (index * HOLOGRAM_LINE_GAP);
    }

    /**
     * @return the online players currently seeing this hologram
     */
    private List<ServerPlayer> currentViewers() {
        List<ServerPlayer> viewers = new ArrayList<>();

        for (UUID uuid : this.nearbyPlayers) {
            ServerPlayer player = UtilConcurrency.getPlayer(uuid);
            if (player != null) {
                viewers.add(player);
            }
        }

        return viewers;
    }

    private static float normaliseYaw(float yaw) {
        float wrapped = yaw % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
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

        // Create a hologram line for the current display type and set its text
        HologramLineRenderer line = createLine(lineY(this.lines.size()), text);

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

        // Remove backlight from old position
        clearBacklight();

        // Update position
        this.position = new Vec3(x, y, z);
        this.repositionLines();

        // Re-apply backlight at new position
        applyBacklight();

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

        HologramLineRenderer line = this.lines.get(lineNumber);
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

        // Create a new line
        HologramLineRenderer line = createLine(lineY(lineNumber), text);

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
                for (HologramLineRenderer existingLine : this.lines) {
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
        HologramLineRenderer line = this.lines.remove(lineNumber);
        this.despawnLine(line);

        // Reposition remaining lines
        this.repositionLines();

        // Save changes
        HologramManager.save();
    }

    @Override
    public void delete() {
        // Remove backlight first while we still have a valid world reference
        clearBacklight();

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
        // Apply backlight if enabled (also handles re-apply after reload)
        applyBacklight();
    }

    @Override
    public void despawn() {
        // Despawn visual entities but keep line data
        for (HologramLineRenderer line : this.lines) {
            this.despawnLine(line);
        }
        // Clear nearby players list since no one is seeing the hologram anymore
        this.nearbyPlayers.clear();
        // Remove the world-side light block too
        clearBacklight();
    }

    private void despawnLine(HologramLineRenderer line) {
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
        FabricHologram newHologram = new FabricHologram(
            id, 
            this.world, 
            this.position, 
            this.range, 
            false  // Don't save yet
        );
        
        // Clear any auto-added lines
        newHologram.lines.clear();
        
        // Carry the display type and orientation over to the copy
        newHologram.restoreDisplayState(this.displayType, this.yaw, this.pitch);

        // Get all lines' content and add them in the same order
        for (int i = 0; i < this.lines.size(); i++) {
            HologramLineRenderer line = this.lines.get(i);
            List<String> frames = line.getFrames();

            HologramLineRenderer newLine;
            if (frames != null) {
                LOGGER.debug("Copying animated line {} ({} frames)", i, frames.size());
                newLine = newHologram.createAnimatedLine(newHologram.lineY(i), frames, line.getIntervalTicks());
            } else {
                String lineText = line.getText();
                LOGGER.debug("Copying line {}: '{}'", i, lineText);
                newLine = newHologram.createLine(newHologram.lineY(i), lineText);
            }

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
            HologramLineRenderer line = this.lines.get(i);
            double lineY = lineY(i);
            
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
        // Convert seconds to ticks
        HologramLineRenderer animatedLine = createAnimatedLine(lineY(this.lines.size()), frames, intervalSeconds * 20);

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
        HologramLineRenderer oldLine = this.lines.get(index);
        
        // Despawn old line for all nearby players
        this.despawnLine(oldLine);
        
        // Replace it with an animated line in the same slot. Convert seconds to ticks.
        HologramLineRenderer newLine = createAnimatedLine(lineY(index), frames, intervalSeconds * 20);
        
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

    public List<HologramLineRenderer> getLines() {
        return this.lines;
    }

    /**
     * @return how this hologram's lines are rendered
     */
    public HologramDisplayType getDisplayType() {
        return this.displayType;
    }

    /**
     * @return the yaw applied to the lines when the display type is fixed
     */
    public float getYaw() {
        return this.yaw;
    }

    /**
     * @return the pitch applied to the lines when the display type is fixed
     */
    public float getPitch() {
        return this.pitch;
    }

    /**
     * Restores display settings without saving. Used while loading from storage, and it must be
     * called before any lines are added so they are built as the right entity type.
     *
     * @param displayType the persisted display type
     * @param yaw         the persisted yaw
     * @param pitch       the persisted pitch
     */
    public void restoreDisplayState(HologramDisplayType displayType, float yaw, float pitch) {
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);
    }

    /**
     * Sets the orientation used by fixed holograms. Stored regardless of the current display
     * type so switching to fixed later keeps the rotation.
     *
     * @param yaw   rotation around the Y axis in degrees
     * @param pitch rotation around the X axis in degrees
     */
    public void setRotation(float yaw, float pitch) {
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);

        for (HologramLineRenderer line : this.lines) {
            line.setRotation(this.yaw, this.pitch);
        }

        // Teleport packets carry rotation, so this is enough to push the change.
        for (ServerPlayer player : currentViewers()) {
            for (HologramLineRenderer line : this.lines) {
                line.sendTeleportPacket(player);
            }
        }

        HologramManager.save();
    }

    /**
     * Switches how this hologram renders. The line entities are a different entity type per
     * display type, so every line is rebuilt and respawned for anyone currently watching.
     *
     * @param displayType the display type to use
     */
    public void setDisplayType(HologramDisplayType displayType) {
        if (displayType == null || displayType == this.displayType) {
            return;
        }

        List<ServerPlayer> viewers = currentViewers();

        // Snapshot the content before the old line entities go away.
        List<String> texts = new ArrayList<>();
        List<List<String>> frames = new ArrayList<>();
        List<Integer> intervals = new ArrayList<>();

        for (HologramLineRenderer line : this.lines) {
            texts.add(line.getText());
            frames.add(line.getFrames());
            intervals.add(line.getIntervalTicks());

            for (ServerPlayer player : viewers) {
                line.despawnForPlayer(player);
            }
        }

        this.displayType = displayType;
        this.lines.clear();

        for (int i = 0; i < texts.size(); i++) {
            List<String> lineFrames = frames.get(i);
            this.lines.add(lineFrames != null
                    ? createAnimatedLine(lineY(i), lineFrames, intervals.get(i))
                    : createLine(lineY(i), texts.get(i)));
        }

        for (HologramLineRenderer line : this.lines) {
            for (ServerPlayer player : viewers) {
                line.spawnForPlayer(player);
                line.updateForPlayer(player);
            }
        }

        HologramManager.save();
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
    // === Backlight API ===

    /**
     * @return whether this hologram has a backlight (light block) applied at its position.
     */
    public boolean isBacklightEnabled() {
        return backlightEnabled;
    }

    /**
     * @return the configured backlight emission level (0-15).
     */
    public int getBacklightLevel() {
        return backlightLevel;
    }

    /**
     * Enable or disable the backlight for this hologram. When enabled, an
     * invisible {@code minecraft:light} block is placed at the hologram's
     * anchor block, lighting up the area without showing any block.
     */
    public void setBacklight(boolean enabled, int level) {
        this.backlightLevel = UtilBacklight.clampLevel(level);
        if (this.backlightEnabled && !enabled) {
            clearBacklight();
            this.backlightEnabled = false;
        } else if (enabled) {
            this.backlightEnabled = true;
            clearBacklight();
            applyBacklight();
        }
        HologramManager.save();
    }

    /**
     * Restores backlight state from config without saving. Used during load.
     */
    public void restoreBacklightState(boolean enabled, int level) {
        this.backlightEnabled = enabled;
        this.backlightLevel = UtilBacklight.clampLevel(level);
    }

    private void applyBacklight() {
        if (!backlightEnabled || this.world == null || this.position == null) {
            return;
        }

        // The column spans from the ground beneath the lowest line up through
        // the top line, so every row of a tall hologram (e.g. a Top-10
        // scoreboard) sits inside the lit column.
        int lineCount = this.lines.isEmpty() ? 1 : this.lines.size();
        double topY = this.position.y;
        double bottomY = this.position.y - ((lineCount - 1) * HOLOGRAM_LINE_GAP);

        this.backlightPositions = UtilBacklight.placeColumn(this.world, this.position.x, bottomY,
                topY, this.position.z, backlightLevel);
    }

    private void clearBacklight() {
        if (backlightPositions == null || backlightPositions.isEmpty() || this.world == null) {
            return;
        }
        UtilBacklight.removeLights(this.world, backlightPositions);
        this.backlightPositions = new ArrayList<>();
    }

    public void teleport(Level world, Vec3 position) {
        // Remove backlight from old position/world
        clearBacklight();

        // Update the world and position
        this.world = world;
        this.position = position;

        // Update all lines' world and position
        for (HologramLineRenderer line : this.lines) {
            line.setWorld(world);
        }
        
        // Reposition lines in the new location
        this.repositionLines();

        // Re-apply backlight at the new location
        applyBacklight();

        // Force refresh of visibility
        this.refreshVisibility();

        // Save changes
        HologramManager.save();
    }
}
