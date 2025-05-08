package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge.ForgeHolograms;
import com.strictgaming.elite.holograms.forge.hologram.database.JsonHologramSaver;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Static factory manager for all {@link Hologram}s on the server
 *
 */
public class HologramManager implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    public static void preInit() {
        new Thread(new HologramManager()).start();

        new PlayerEventListener();
        saver = (HologramSaver) new JsonHologramSaver(ForgeHolograms.getInstance().getConfig().getStorageLocation());
    }

    private static final Map<String, ForgeHologram> HOLOGRAMS = Maps.newConcurrentMap();

    private static HologramSaver saver;

    private HologramManager() {
        // Private constructor for singleton
    }

    public static void clear() {
        for (ForgeHologram value : HOLOGRAMS.values()) {
            value.despawn();
        }
        HOLOGRAMS.clear();
    }

    public static void load() throws IOException {
        LOGGER.info("Loading holograms from storage...");
        
        // Save a snapshot of the currently loaded holograms
        Map<String, ForgeHologram> existingHolograms = new HashMap<>(HOLOGRAMS);
        
        // Load from the saver
        Map<String, Hologram> loadedHolograms = saver.load();
        
        if (loadedHolograms.isEmpty() && !existingHolograms.isEmpty()) {
            LOGGER.info("No holograms loaded from file, but we have " + existingHolograms.size() + " in memory - preserving existing");
            // If we have holograms in memory but none loaded from file, likely an error in loading
            // Just write the existing ones to file again
            save();
            return;
        }
        
        // Add any missing holograms to the HOLOGRAMS map
        for (Map.Entry<String, Hologram> entry : loadedHolograms.entrySet()) {
            if (entry.getValue() instanceof ForgeHologram && 
                !HOLOGRAMS.containsKey(entry.getKey().toLowerCase())) {
                
                HOLOGRAMS.put(entry.getKey().toLowerCase(), (ForgeHologram) entry.getValue());
                LOGGER.info("Added hologram from storage: " + entry.getKey());
            }
        }
        
        LOGGER.info("Successfully loaded " + HOLOGRAMS.size() + " holograms");
    }

    public static void save() {
        UtilConcurrency.runAsync(() -> saver.save(Lists.newArrayList(HOLOGRAMS.values())));
    }

    public static void addHologram(Hologram hologram) {
        if (!(hologram instanceof ForgeHologram)) {
            return;
        }

        addHologram((ForgeHologram) hologram);
    }

    public static void addHologram(ForgeHologram hologram) {
        HOLOGRAMS.put(hologram.getId().toLowerCase(), hologram);
    }

    public static void removeHologram(Hologram hologram) {
        if (!(hologram instanceof ForgeHologram)) {
            return;
        }

        removeHologram((ForgeHologram) hologram);
    }

    public static void removeHologram(ForgeHologram hologram) {
        HOLOGRAMS.remove(hologram.getId().toLowerCase());
    }

    public static Hologram getById(String id) {
        return HOLOGRAMS.get(id.toLowerCase());
    }

    public static List<Hologram> getAllHolograms() {
        return Collections.unmodifiableList(Lists.newArrayList(HOLOGRAMS.values()));
    }

    /**
     * Get the hologram saver used by this manager
     * 
     * @return The hologram saver instance
     */
    public static HologramSaver getSaver() {
        return saver;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(500); // Run every half second
                checkHolograms();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void checkHolograms() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player == null || player.level == null) {
                continue; // Skip if player or their level is null
            }
            
            for (ForgeHologram hologram : HOLOGRAMS.values()) {
                if (hologram == null || hologram.getWorld() == null) {
                    continue; // Skip if hologram or its world is null
                }
                
                if (!hologram.getWorld().equals(player.level)) {
                    if (hologram.getNearbyPlayers().contains(player.getUUID())) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        for (HologramLine line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (player.distanceToSqr(hologram.getPosition()) > (Math.pow(hologram.getRange(), 2))) {
                    if (hologram.getNearbyPlayers().contains(player.getUUID())) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        for (HologramLine line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (!hologram.getNearbyPlayers().contains(player.getUUID())) {
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> line.spawnForPlayer(player));
                        }
                    }

                    hologram.getNearbyPlayers().add(player.getUUID());
                } else {
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> line.updateForPlayer(player));
                        }
                    }
                }
            }
        }
    }

    private static class PlayerEventListener {
        private long nextRun = 0L;
        
        public PlayerEventListener() {
            // Register this instance to the Forge event bus
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        }
        
        @SubscribeEvent
        public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event == null || event.getEntity() == null) {
                return;
            }
            
            UUID playerUUID = event.getEntity().getUUID();
            if (playerUUID == null) {
                return;
            }
            
            for (ForgeHologram value : HologramManager.HOLOGRAMS.values()) {
                if (value != null && value.getNearbyPlayers() != null) {
                    value.getNearbyPlayers().remove(playerUUID);
                }
            }
        }
        
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event == null || event.getEntity() == null || !(event.getEntity() instanceof ServerPlayer)) {
                return;
            }
            
            // When a player logs in, refresh all holograms to make them check visibility
            for (ForgeHologram hologram : HologramManager.HOLOGRAMS.values()) {
                if (hologram != null) {
                    hologram.refreshVisibility();
                }
            }
            
            LOGGER.info("Refreshed hologram visibility for player: {}", event.getEntity().getName().getString());
        }
    }
} 
