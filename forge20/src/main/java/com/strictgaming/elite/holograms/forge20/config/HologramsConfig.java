package com.strictgaming.elite.holograms.forge20.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.strictgaming.elite.holograms.forge20.util.ConfigUtil;

/**
 * Configuration class for hologram settings
 */
public class HologramsConfig {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private String storageLocation = "config/elite-holograms";
    private int defaultRange = 30;
    private boolean debug = false;

    public HologramsConfig() {
        // Default constructor
    }

    /**
     * Loads the configuration from the config file
     *
     * @throws IOException If there's an error reading the file
     */
    public void load() throws IOException {
        File configDir = new File("config/elite-holograms");
        
        if (!configDir.exists() && !configDir.mkdirs()) {
            LOGGER.error("Failed to create config directory");
            return;
        }
        
        File configFile = new File(configDir, "config.json");
        
        if (!configFile.exists()) {
            // Create config file with default values
            createDefaultConfig(configFile);
            return;
        }
        
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            if (json.has("storage_location")) {
                this.storageLocation = json.get("storage_location").getAsString();
            }
            
            if (json.has("default_range")) {
                this.defaultRange = json.get("default_range").getAsInt();
            }
            
            if (json.has("debug")) {
                this.debug = json.get("debug").getAsBoolean();
            }
            
            LOGGER.info("Loaded configuration: storage_location={}, default_range={}, debug={}",
                    this.storageLocation, this.defaultRange, this.debug);
        } catch (Exception e) {
            LOGGER.error("Error loading configuration", e);
            createDefaultConfig(configFile);
        }
    }
    
    /**
     * Creates the default configuration file
     *
     * @param configFile The file to create
     * @throws IOException If there's an error writing the file
     */
    private void createDefaultConfig(File configFile) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("storage_location", this.storageLocation);
        json.addProperty("default_range", this.defaultRange);
        json.addProperty("debug", this.debug);
        
        String content = GSON.toJson(json);
        Files.write(configFile.toPath(), content.getBytes());
        LOGGER.info("Created default configuration file");
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }

    public int getDefaultRange() {
        return this.defaultRange;
    }
    
    public boolean isDebug() {
        return this.debug;
    }
} 