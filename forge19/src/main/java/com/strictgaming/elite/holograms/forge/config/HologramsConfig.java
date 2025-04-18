package com.strictgaming.elite.holograms.forge.config;

import java.io.*;
import java.util.Properties;

/**
 * A simple config class for Elite Holograms
 */
public class HologramsConfig {

    private static final String CONFIG_FILE = "config/eliteholograms.properties";
    
    private final Properties properties = new Properties();
    
    public void load() throws IOException {
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            // Create parent directories if needed
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            
            // Save default values
            saveDefaults();
        }
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }
    }
    
    public void save() throws IOException {
        File configFile = new File(CONFIG_FILE);
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Elite Holograms Configuration");
        }
    }
    
    private void saveDefaults() throws IOException {
        properties.setProperty("storage.type", "JSON");
        properties.setProperty("storage.location", "config/eliteholograms/holograms.json");
        save();
    }
    
    public String getStorageType() {
        return properties.getProperty("storage.type", "JSON");
    }
    
    public String getStorageLocation() {
        return properties.getProperty("storage.location", "config/eliteholograms/holograms.json");
    }
} 
