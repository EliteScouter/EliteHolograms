package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A special hologram that displays top players from a scoreboard objective
 * Note: This hologram type is not saved to JSON and must be recreated on server restart
 */
public class ScoreboardHologram extends ForgeHologram {
    
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    
    private final String objectiveName;
    private final int topCount;
    private final int updateInterval; // seconds
    private final String headerFormat;
    private final String playerFormat;
    private final String emptyFormat;
    private final boolean isTimeObjective;
    
    private long lastUpdate = 0;
    private List<ScoreEntry> lastScores = new ArrayList<>();
    
    public ScoreboardHologram(String id, Level world, Vec3 position, int range, 
                             String objectiveName, int topCount, int updateInterval,
                             String headerFormat, String playerFormat, String emptyFormat) {
        super(id, world, position, range, false); // Don't save initially
        
        this.objectiveName = objectiveName;
        this.topCount = Math.max(1, Math.min(topCount, 10)); // Limit between 1-10
        this.updateInterval = Math.max(5, updateInterval); // Minimum 5 seconds
        
        // Detect if this is a time-based objective
        this.isTimeObjective = isTimeBasedObjective(objectiveName);
        
        this.headerFormat = headerFormat != null ? headerFormat : "§6§l{objective} - Top {count}";
        this.playerFormat = playerFormat != null ? playerFormat : (isTimeObjective ? 
            "§e{rank}. §f{player} §7- §a{time}" : "§e{rank}. §f{player} §7- §a{score}");
        this.emptyFormat = emptyFormat != null ? emptyFormat : "§7No data available";
        
        // Clear any default lines and set up initial display
        this.getLines().clear();
        this.updateScoreboardDisplay();
    }
    
    /**
     * Updates the scoreboard display if enough time has passed
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= (updateInterval * 1000L)) {
            updateScoreboardDisplay();
            lastUpdate = currentTime;
        }
    }
    
    /**
     * Forces an immediate update of the scoreboard display
     */
    public void forceUpdate() {
        updateScoreboardDisplay();
        lastUpdate = System.currentTimeMillis();
    }
    
    private void updateScoreboardDisplay() {
        try {
            List<ScoreEntry> currentScores = getTopScores();
            
            // Only update if scores have changed
            if (!scoresEqual(currentScores, lastScores)) {
                updateHologramLines(currentScores);
                lastScores = new ArrayList<>(currentScores);
                LOGGER.debug("Updated scoreboard hologram '{}' with {} entries", getId(), currentScores.size());
            }
        } catch (Exception e) {
            LOGGER.error("Error updating scoreboard hologram '{}': {}", getId(), e.getMessage());
        }
    }
    
    private List<ScoreEntry> getTopScores() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Collections.emptyList();
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        
        if (objective == null) {
            LOGGER.debug("Objective '{}' not found", objectiveName);
            return Collections.emptyList();
        }
        
        // Get all scores for this objective
        Map<String, Integer> playerScores = new HashMap<>();
        
        // Get all player scores for this objective
        Collection<Score> scores = scoreboard.getPlayerScores(objective);
        for (Score score : scores) {
            String playerName = score.getOwner();
            int scoreValue = score.getScore();
            playerScores.put(playerName, scoreValue);
        }
        
        // Sort by score (descending) and take top N
        return playerScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topCount)
                .map(entry -> new ScoreEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    
    private void updateHologramLines(List<ScoreEntry> scores) {
        List<String> newLines = new ArrayList<>();
        
        // Add header
        String header = headerFormat
                .replace("{objective}", objectiveName)
                .replace("{count}", String.valueOf(topCount));
        newLines.add(header);
        
        if (scores.isEmpty()) {
            newLines.add(emptyFormat);
        } else {
            // Add player entries
            for (int i = 0; i < scores.size(); i++) {
                ScoreEntry entry = scores.get(i);
                String line = playerFormat
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{player}", entry.playerName)
                        .replace("{score}", String.valueOf(entry.score))
                        .replace("{time}", isTimeObjective ? formatTime(entry.score) : String.valueOf(entry.score));
                newLines.add(line);
            }
        }
        
        // Update hologram lines
        // Clear existing lines first (despawn for all nearby players)
        for (HologramLine line : getLines()) {
            for (UUID playerUUID : getNearbyPlayers()) {
                ServerPlayer player = UtilPlayer.getOnlinePlayer(playerUUID);
                if (player != null) {
                    line.despawnForPlayer(player);
                }
            }
        }
        getLines().clear();
        
        // Add new lines
        for (String line : newLines) {
            addLine(line);
        }
    }
    
    private boolean scoresEqual(List<ScoreEntry> list1, List<ScoreEntry> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        for (int i = 0; i < list1.size(); i++) {
            ScoreEntry entry1 = list1.get(i);
            ScoreEntry entry2 = list2.get(i);
            
            if (!entry1.playerName.equals(entry2.playerName) || entry1.score != entry2.score) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Detects if an objective name suggests it's tracking time
     */
    private boolean isTimeBasedObjective(String objectiveName) {
        String lowerName = objectiveName.toLowerCase();
        return lowerName.contains("time") || 
               lowerName.contains("played") || 
               lowerName.contains("playtime") ||
               lowerName.contains("online") ||
               lowerName.contains("session") ||
               lowerName.equals("stat.playonetick"); // Vanilla playtime stat
    }
    
    /**
     * Formats ticks into a human-readable time format
     * @param ticks The number of ticks (20 ticks = 1 second)
     * @return Formatted time string
     */
    private String formatTime(int ticks) {
        // Convert ticks to seconds (20 ticks = 1 second)
        int totalSeconds = ticks / 20;
        
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        
        int totalMinutes = totalSeconds / 60;
        if (totalMinutes < 60) {
            int seconds = totalSeconds % 60;
            return totalMinutes + "m" + (seconds > 0 ? " " + seconds + "s" : "");
        }
        
        int totalHours = totalMinutes / 60;
        if (totalHours < 24) {
            int minutes = totalMinutes % 60;
            return totalHours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
        }
        
        int days = totalHours / 24;
        int hours = totalHours % 24;
        return days + "d" + (hours > 0 ? " " + hours + "h" : "");
    }
    
    // Getters
    public String getObjectiveName() { return objectiveName; }
    public int getTopCount() { return topCount; }
    public int getUpdateInterval() { return updateInterval; }
    public boolean isTimeObjective() { return isTimeObjective; }
    
    /**
     * Get the world name for this hologram
     */
    public String getWorldName() {
        if (getWorld() == null) {
            return "unknown";
        }
        return getWorld().dimension().location().toString();
    }
    
    /**
     * Get the location coordinates for this hologram
     */
    public double[] getLocation() {
        Vec3 pos = getPosition();
        return new double[] { pos.x, pos.y, pos.z };
    }
    
    /**
     * Simple data class to hold player name and score
     */
    private static class ScoreEntry {
        final String playerName;
        final int score;
        
        ScoreEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
    }
}