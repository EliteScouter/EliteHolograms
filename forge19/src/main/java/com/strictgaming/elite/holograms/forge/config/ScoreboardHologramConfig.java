package com.strictgaming.elite.holograms.forge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration manager for scoreboard holograms
 * Saves only the metadata needed to recreate them, not the complex objects
 */
public class ScoreboardHologramConfig {
    
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SCOREBOARD_CONFIG_LIST_TYPE = new TypeToken<List<ScoreboardHologramData>>(){}.getType();
    
    private final File configFile;
    
    public ScoreboardHologramConfig(File configDir) {
        this.configFile = new File(configDir, "scoreboard_holograms.json");
    }
    
    /**
     * Save scoreboard hologram configurations
     */
    public void save(List<ScoreboardHologram> scoreboardHolograms) {
        try {
            List<ScoreboardHologramData> configData = new ArrayList<>();
            
            for (ScoreboardHologram hologram : scoreboardHolograms) {
                ScoreboardHologramData data = new ScoreboardHologramData();
                data.id = hologram.getId();
                data.objectiveName = hologram.getObjectiveName();
                data.topCount = hologram.getTopCount();
                data.updateInterval = hologram.getUpdateInterval();
                data.range = hologram.getRange();
                data.worldName = hologram.getWorldName();
                
                double[] location = hologram.getLocation();
                data.x = location[0];
                data.y = location[1];
                data.z = location[2];
                
                configData.add(data);
            }
            
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                GSON.toJson(configData, SCOREBOARD_CONFIG_LIST_TYPE, writer);
                LOGGER.info("Saved {} scoreboard hologram configurations", configData.size());
            }
            
        } catch (IOException e) {
            LOGGER.error("Error saving scoreboard hologram config: {}", e.getMessage());
        }
    }
    
    /**
     * Load scoreboard hologram configurations
     */
    public List<ScoreboardHologramData> load() {
        if (!configFile.exists()) {
            LOGGER.debug("Scoreboard hologram config file does not exist, returning empty list");
            return new ArrayList<>();
        }
        
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            List<ScoreboardHologramData> configData = GSON.fromJson(reader, SCOREBOARD_CONFIG_LIST_TYPE);
            
            if (configData == null) {
                configData = new ArrayList<>();
            }
            
            LOGGER.info("Loaded {} scoreboard hologram configurations", configData.size());
            return configData;
            
        } catch (IOException e) {
            LOGGER.error("Error loading scoreboard hologram config: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Data class for scoreboard hologram configuration
     */
    public static class ScoreboardHologramData {
        public String id;
        public String objectiveName;
        public int topCount;
        public int updateInterval;
        public int range;
        public String worldName;
        public double x;
        public double y;
        public double z;
        
        // Optional custom formats (can be null for defaults)
        public String headerFormat;
        public String playerFormat;
        public String emptyFormat;
    }
}