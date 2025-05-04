package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

/**
 * NeoForge implementation of hologram
 */
public class NeoForgeHologram implements Hologram {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double LINE_SPACING = 0.25;
    
    private final String id;
    private String world;
    private double x;
    private double y;
    private double z;
    private final List<String> lines = new ArrayList<>();
    private final List<ArmorStand> armorStands = new ArrayList<>();
    private boolean spawned = false;
    
    /**
     * Creates a new hologram
     * 
     * @param id The hologram ID
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param lines The text lines
     */
    public NeoForgeHologram(String id, String world, double x, double y, double z, List<String> lines) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        if (lines != null) {
            this.lines.addAll(lines);
        }
        
        // Register with manager
        HologramManager.addHologram(this);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
    
    @Override
    public void setLines(List<String> lines) {
        this.lines.clear();
        if (lines != null) {
            this.lines.addAll(lines);
        }
        update();
        saveToConfig();
    }
    
    @Override
    public void setLines(String... lines) {
        this.lines.clear();
        if (lines != null) {
            this.lines.addAll(Arrays.asList(lines));
        }
        update();
        saveToConfig();
    }
    
    @Override
    public String getLine(int index) {
        if (index >= 0 && index < lines.size()) {
            return lines.get(index);
        }
        return null;
    }
    
    @Override
    public void setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, text);
            update();
            saveToConfig();
        }
    }
    
    @Override
    public void addLine(String text) {
        lines.add(text);
        update();
        saveToConfig();
    }
    
    @Override
    public void insertLine(int index, String text) {
        if (index >= 0 && index <= lines.size()) {
            lines.add(index, text);
            update();
            saveToConfig();
        }
    }
    
    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            update();
            saveToConfig();
        }
    }
    
    @Override
    public String getWorld() {
        return world;
    }
    
    @Override
    public double getX() {
        return x;
    }
    
    @Override
    public double getY() {
        return y;
    }
    
    @Override
    public double getZ() {
        return z;
    }
    
    @Override
    public void setPosition(String world, double x, double y, double z) {
        boolean needsRespawn = !world.equals(this.world) && spawned;
        
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        
        if (spawned) {
            if (needsRespawn) {
                despawn();
                spawn();
            } else {
                updatePositions();
            }
        }
        
        saveToConfig();
    }
    
    @Override
    public void spawn() {
        if (spawned) {
            return;
        }
        
        ServerLevel level = getServerLevel();
        if (level == null) {
            LOGGER.warn("Failed to spawn hologram: World not found - " + world);
            return;
        }
        
        despawn();
        
        double currentY = y;
        for (int i = 0; i < lines.size(); i++) {
            String line = processLine(lines.get(i));
            ArmorStand stand = createArmorStand(level, x, currentY, z, line);
            if (stand != null) {
                armorStands.add(stand);
                level.addFreshEntity(stand);
            }
            currentY -= LINE_SPACING;
        }
        
        spawned = true;
    }
    
    @Override
    public void despawn() {
        // Make a copy of the list to avoid concurrent modification
        List<ArmorStand> standsToRemove = new ArrayList<>(armorStands);
        
        // Clear our internal list first
        armorStands.clear();
        
        // Then remove each armor stand from the world
        for (ArmorStand stand : standsToRemove) {
            try {
                if (stand != null && stand.isAlive()) {
                    LOGGER.debug("Removing armor stand entity for hologram: {}", id);
                    stand.remove(ArmorStand.RemovalReason.DISCARDED);
                    
                    // We don't need to manually remove from entity lookup
                    // Just removing with DISCARDED reason is sufficient
                }
            } catch (Exception e) {
                LOGGER.error("Error removing armor stand for hologram {}: {}", id, e.getMessage());
            }
        }
        
        spawned = false;
        LOGGER.debug("Hologram {} despawned, removed {} armor stands", id, standsToRemove.size());
    }
    
    @Override
    public boolean isSpawned() {
        return spawned;
    }
    
    @Override
    public void update() {
        if (spawned) {
            despawn();
            spawn();
        }
    }
    
    /**
     * Completely deletes this hologram from the world and manager
     */
    @Override
    public void delete() {
        LOGGER.info("Deleting hologram: {}", id);
        
        // First despawn from the world to clean up entities
        despawn();
        
        // Then remove from the manager
        HologramManager.removeHologram(id);
        
        LOGGER.info("Hologram deleted: {}", id);
    }
    
    /**
     * Updates the positions of all armor stands
     */
    private void updatePositions() {
        double currentY = y;
        for (ArmorStand stand : armorStands) {
            stand.setPos(x, currentY, z);
            currentY -= LINE_SPACING;
        }
    }
    
    /**
     * Creates an armor stand for a text line
     * 
     * @param level The server level
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param text The text to display
     * @return The created armor stand
     */
    private ArmorStand createArmorStand(ServerLevel level, double x, double y, double z, String text) {
        // Create the armor stand directly instead of using the complex factory method
        ArmorStand stand = new ArmorStand(level, x, y, z);
        
        // Configure the armor stand for hologram display
        stand.setInvisible(true);
        stand.setNoGravity(true);
        
        // We need to directly set the flags instead of using setters
        // Use NBT data to configure the armor stand
        stand.getPersistentData().putBoolean("Invisible", true);
        stand.getPersistentData().putBoolean("NoGravity", true);
        stand.getPersistentData().putBoolean("Marker", true);
        stand.getPersistentData().putBoolean("NoBasePlate", true);
        stand.getPersistentData().putBoolean("Small", true);
        stand.getPersistentData().putBoolean("ShowArms", false);
        
        // Mark as a hologram (for identification)
        stand.getPersistentData().putBoolean("EliteHologram", true);
        
        stand.setSilent(true);
        stand.setInvulnerable(true);
        stand.setCustomNameVisible(true);
        
        // Use UtilChatColour to parse color codes before setting the name
        if (text.equals("{empty}")) {
            stand.setCustomNameVisible(false);
            stand.setCustomName(Component.literal(" "));
        } else {
            stand.setCustomName(UtilChatColour.parse(text));
        }
        
        return stand;
    }
    
    /**
     * Processes a line with placeholders if enabled
     * 
     * @param line The line to process
     * @return The processed line
     */
    private String processLine(String line) {
        if (Neo21Holograms.getInstance().arePlaceholdersEnabled()) {
            // Placeholder processing would go here
            // For example: return PlaceholderAPI.setPlaceholders(null, line);
        }
        return line;
    }
    
    /**
     * Gets the server level for this hologram
     * 
     * @return The server level
     */
    private ServerLevel getServerLevel() {
        if (world == null || world.isEmpty()) {
            LOGGER.warn("World name is null or empty, using overworld");
            return ServerLifecycleHooks.getCurrentServer().overworld();
        }
        
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            String levelName = level.dimension().location().toString();
            if (levelName.equals(world) || level.dimension().location().getPath().equals(world)) {
                return level;
            }
        }
        
        LOGGER.warn("World '{}' not found, using overworld instead", world);
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }
    
    /**
     * Saves this hologram to the config
     */
    private void saveToConfig() {
        try {
            HologramManager.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save hologram to config", e);
        }
    }
} 