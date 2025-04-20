package com.strictgaming.elite.holograms.forge20.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility methods for configuration
 */
public class ConfigUtil {
    
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    
    /**
     * Creates a directory if it doesn't exist
     * 
     * @param path The path to create
     * @return True if successful, false otherwise
     */
    public static boolean createDirectoryIfNotExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                LOGGER.error("Failed to create directory: {}", path);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Ensures a configuration directory exists
     * 
     * @param path The path to check
     * @return The path if it exists, or null if it couldn't be created
     */
    public static Path ensureConfigDir(String path) {
        if (createDirectoryIfNotExists(path)) {
            return Paths.get(path);
        }
        return null;
    }
    
    /**
     * Creates a default file from a resource
     * 
     * @param resourcePath The path to the resource
     * @param targetPath The path to create
     * @return True if successful, false otherwise
     */
    public static boolean createDefaultFromResource(String resourcePath, Path targetPath) {
        try {
            // Check if we need to create parent directories
            Files.createDirectories(targetPath.getParent());
            
            // Copy the resource
            ConfigUtil.class.getResourceAsStream(resourcePath).transferTo(Files.newOutputStream(targetPath));
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to create default configuration from resource", e);
            return false;
        }
    }
} 