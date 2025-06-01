package com.strictgaming.elite.holograms.neo21.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.config.HologramsConfig;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * Manager for handling hologram operations
 */
public class HologramManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Hologram> HOLOGRAMS = new HashMap<>();
    
    /**
     * Initialize the hologram system
     */
    public static void preInit() {
        LOGGER.info("Pre-initializing hologram manager");
    }
    
    /**
     * Load holograms from config
     * 
     * @throws IOException If loading fails
     */
    public static void load() throws IOException {
        LOGGER.info("Loading holograms from config");
        HologramsConfig config = Neo21Holograms.getInstance().getConfig();
        
        if (config == null) {
            LOGGER.warn("Config is null, cannot load holograms.");
            return;
        }
        
        // Despawn all existing before clearing
        HOLOGRAMS.values().forEach(Hologram::despawn);
        HOLOGRAMS.clear();
        
        // Let config directly add to manager
        config.loadHologramsIntoManager();
    }
    
    /**
     * Add a hologram to the manager
     * 
     * @param hologram The hologram to add
     */
    public static void addHologram(Hologram hologram) {
        if (hologram == null) return;
        HOLOGRAMS.put(hologram.getId(), hologram);
        // No save here, let config do it or explicit calls
    }
    
    /**
     * Get a hologram by ID
     * 
     * @param id The hologram ID
     * @return The hologram, if found
     */
    public static Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(HOLOGRAMS.get(id));
    }
    
    /**
     * Remove a hologram from the manager
     * 
     * @param id The hologram ID
     * @return True if removed, false if not found
     */
    public static boolean removeHologram(String id) {
        Hologram hologram = HOLOGRAMS.remove(id);
        if (hologram != null) {
            // Ensure it's fully despawned / marked inactive
            hologram.despawn();
            saveHolograms();
            return true;
        }
        return false;
    }
    
    /**
     * Get all holograms
     * 
     * @return The map of holograms
     */
    public static Map<String, Hologram> getHolograms() {
        return new HashMap<>(HOLOGRAMS);
    }
    
    /**
     * Save holograms to config
     * 
     * @throws IOException If saving fails
     */
    public static void save() throws IOException {
        HologramsConfig config = Neo21Holograms.getInstance().getConfig();
        if (config != null) {
            config.saveHologramsFromManager(HOLOGRAMS);
            LOGGER.info("Saved {} holograms to config.", HOLOGRAMS.size());
        } else {
            LOGGER.warn("Config is null, cannot save holograms.");
        }
    }
    
    /**
     * Helper method to save holograms and handle exceptions
     */
    private static void saveHolograms() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms", e);
        }
    }
    
    // Player event handling
    public static void handlePlayerJoin(ServerPlayer player) {
        if (player == null) return;
        LOGGER.debug("Player {} joined, checking nearby holograms.", player.getName().getString());
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isSpawned() && nfHologram.isPlayerNearby(player)) {
                nfHologram.spawnForPlayer(player);
            }
        });
    }
    
    public static void handlePlayerLeave(ServerPlayer player) {
        if (player == null) return;
        LOGGER.debug("Player {} left, despawning their holograms.", player.getName().getString());
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isVisibleTo(player)) {
                nfHologram.despawnForPlayer(player);
            }
        });
    }
    
    public static void handlePlayerMove(ServerPlayer player) {
        if (player == null) return;
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isSpawned()) {
                boolean isNearby = nfHologram.isPlayerNearby(player);
                boolean isVisible = nfHologram.isVisibleTo(player);
                
                if (isNearby && !isVisible) {
                    nfHologram.spawnForPlayer(player);
                } else if (!isNearby && isVisible) {
                    nfHologram.despawnForPlayer(player);
                } else if (isNearby && isVisible) {
                    // Player is nearby and hologram is visible, check if text needs updating for this player
                    // This is a good place for periodic updates if hologram lines can change dynamically 
                    // without a global /eh update command (e.g. placeholders that update frequently)
                    // For now, just ensure text is correct based on last global update.
                    nfHologram.updateTextForPlayer(player); 
                }
            }
        });
    }
} 