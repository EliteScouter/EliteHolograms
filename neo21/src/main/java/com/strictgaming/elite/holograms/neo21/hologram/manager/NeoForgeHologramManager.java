package com.strictgaming.elite.holograms.neo21.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * NeoForge implementation of hologram manager
 */
public class NeoForgeHologramManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Creates a new hologram manager
     */
    public NeoForgeHologramManager() {
        LOGGER.info("Initializing NeoForge hologram manager");
    }
    
    /**
     * Adds a hologram to the manager
     * 
     * @param hologram The hologram to add
     */
    public void addHologram(Hologram hologram) {
        HologramManager.addHologram(hologram);
    }
    
    /**
     * Gets a hologram by ID
     * 
     * @param id The hologram ID
     * @return The hologram, if found
     */
    public Optional<Hologram> getHologram(String id) {
        return HologramManager.getHologram(id);
    }
    
    /**
     * Gets all holograms
     * 
     * @return Collection of all holograms
     */
    public Collection<Hologram> getHolograms() {
        return HologramManager.getHolograms().values();
    }
    
    /**
     * Removes a hologram
     * 
     * @param id The hologram ID
     * @return True if removed, false if not found
     */
    public boolean removeHologram(String id) {
        return HologramManager.removeHologram(id);
    }
    
    /**
     * Reloads all holograms
     * 
     * @throws IOException If loading fails
     */
    public void reload() throws IOException {
        HologramManager.load();
    }
    
    /**
     * Saves all holograms
     * 
     * @throws IOException If saving fails
     */
    public void save() throws IOException {
        HologramManager.save();
    }
    
    /**
     * Clears all holograms
     */
    public void clear() {
        for (Hologram hologram : HologramManager.getHolograms().values()) {
            hologram.despawn();
        }
        HologramManager.getHolograms().clear();
    }
} 