package com.strictgaming.elite.holograms.neo262.hologram;

import com.strictgaming.elite.holograms.neo262.config.ScoreboardTheme;
import com.strictgaming.elite.holograms.neo262.config.ScoreboardThemeManager;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A hologram that renders the top players of a scoreboard objective and periodically updates.
 * This mirrors the Forge 1.20 implementation, adapted for NeoForge 1.21.1.
 */
public class ScoreboardHologram extends NeoForgeHologram {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String objectiveName;
    private final int topCount;
    private final int updateIntervalSeconds;
    private String themeName;
    private String headerFormat;
    private String playerFormat;
    private String emptyFormat;
    private final boolean isTimeObjective;
    private final int range;

    private long lastUpdateMs = 0L;
    private List<ScoreEntry> lastScores = new ArrayList<>();

    /**
     * Theme-based constructor. The named theme is resolved from {@link ScoreboardThemeManager},
     * so editing the theme in {@code scoreboard_themes.json} and reloading restyles the board.
     */
    public ScoreboardHologram(
            String id,
            String worldName,
            double x,
            double y,
            double z,
            int range,
            String objectiveName,
            int topCount,
            int updateIntervalSeconds,
            String themeName
    ) {
        this(id, worldName, x, y, z, range, objectiveName, topCount, updateIntervalSeconds,
                themeName, null, null, null);
    }

    /**
     * Theme-based constructor with an explicit display type and orientation.
     */
    public ScoreboardHologram(
            String id,
            String worldName,
            double x,
            double y,
            double z,
            int range,
            String objectiveName,
            int topCount,
            int updateIntervalSeconds,
            String themeName,
            HologramDisplayType displayType,
            float yaw,
            float pitch
    ) {
        this(id, worldName, x, y, z, range, objectiveName, topCount, updateIntervalSeconds,
                themeName, null, null, null, displayType, yaw, pitch);
    }

    /**
     * Full constructor. When {@code themeName} is set and resolves to a known theme, its format
     * strings win. Otherwise the explicit header/player/empty formats are used, and any that are
     * {@code null} fall back to the built-in defaults. This keeps configs saved before themes
     * existed working unchanged.
     */
    public ScoreboardHologram(
            String id,
            String worldName,
            double x,
            double y,
            double z,
            int range,
            String objectiveName,
            int topCount,
            int updateIntervalSeconds,
            String themeName,
            String headerFormat,
            String playerFormat,
            String emptyFormat
    ) {
        this(id, worldName, x, y, z, range, objectiveName, topCount, updateIntervalSeconds,
                themeName, headerFormat, playerFormat, emptyFormat,
                HologramDisplayType.FACING, 0.0F, 0.0F);
    }

    /**
     * Full constructor including the display type and orientation.
     *
     * <p>The display type has to reach {@code super} here rather than being applied afterwards,
     * because this constructor renders the board's first frame and the line entities differ per
     * display type.
     */
    public ScoreboardHologram(
            String id,
            String worldName,
            double x,
            double y,
            double z,
            int range,
            String objectiveName,
            int topCount,
            int updateIntervalSeconds,
            String themeName,
            String headerFormat,
            String playerFormat,
            String emptyFormat,
            HologramDisplayType displayType,
            float yaw,
            float pitch
    ) {
        super(id, worldName, x, y, z, new ArrayList<>(), displayType, yaw, pitch);

        this.objectiveName = objectiveName;
        this.topCount = Math.max(1, Math.min(topCount, 10));
        this.updateIntervalSeconds = Math.max(5, updateIntervalSeconds);
        this.isTimeObjective = isTimeBasedObjective(objectiveName);
        this.themeName = themeName;

        ScoreboardTheme theme = themeName != null ? ScoreboardThemeManager.getTheme(themeName) : null;
        if (theme != null) {
            this.headerFormat = theme.getHeader();
            this.playerFormat = theme.playerFormatFor(this.isTimeObjective);
            this.emptyFormat = theme.getEmpty();
        } else {
            this.headerFormat = headerFormat != null ? headerFormat : "§6§l{objective} - Top {count}";
            this.playerFormat = playerFormat != null ? playerFormat : (this.isTimeObjective
                    ? "§e{rank}. §f{player} §7- §a{time}"
                    : "§e{rank}. §f{player} §7- §a{score}");
            this.emptyFormat = emptyFormat != null ? emptyFormat : "§7No data available";
        }
        this.range = range > 0 ? range : 32;

        // Initialize display immediately
        updateScoreboardDisplay();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateMs >= (updateIntervalSeconds * 1000L)) {
            updateScoreboardDisplay();
            lastUpdateMs = now;
        }
    }

    public void forceUpdate() {
        updateScoreboardDisplay();
        lastUpdateMs = System.currentTimeMillis();
    }

    /**
     * Switches this board to a different theme at runtime and immediately re-renders.
     *
     * @param newThemeName the theme name to apply (resolved from {@link ScoreboardThemeManager})
     * @return {@code true} if the theme existed and was applied, {@code false} otherwise
     */
    public boolean applyTheme(String newThemeName) {
        ScoreboardTheme theme = newThemeName != null ? ScoreboardThemeManager.getTheme(newThemeName) : null;
        if (theme == null) {
            return false;
        }
        this.themeName = newThemeName;
        this.headerFormat = theme.getHeader();
        this.playerFormat = theme.playerFormatFor(this.isTimeObjective);
        this.emptyFormat = theme.getEmpty();
        // Clear the cached snapshot so the next render is forced even if scores are unchanged.
        this.lastScores = new ArrayList<>();
        forceUpdate();
        return true;
    }

    @Override
    public boolean isPlayerNearby(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || player.level() == null) return false;
        String playerWorld = player.level().dimension().identifier().toString();
        if (!playerWorld.equals(getWorld())) return false;
        double dx = player.getX() - getX();
        double dy = player.getY() - getY();
        double dz = player.getZ() - getZ();
        return (dx * dx + dy * dy + dz * dz) <= (range * range);
    }

    private void updateScoreboardDisplay() {
        try {
            List<ScoreEntry> current = getTopScores();
            if (!scoresEqual(current, lastScores)) {
                applyLines(current);
                lastScores = new ArrayList<>(current);
                LOGGER.debug("Updated scoreboard hologram '{}' with {} entries", getId(), current.size());
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

        // listPlayerScores returns ALL entries for the objective, including offline players
        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        Map<String, Integer> playerScores = new HashMap<>();
        for (PlayerScoreEntry entry : entries) {
            String owner = entry.owner();
            if (owner != null && !owner.isEmpty() && !entry.isHidden()) {
                playerScores.put(owner, entry.value());
            }
        }

        // Fallback: online players only if listPlayerScores is empty (e.g. objective has no data yet)
        if (playerScores.isEmpty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player == null) continue;
                try {
                    ScoreAccess sc = scoreboard.getOrCreatePlayerScore(player, objective);
                    int value = sc.get();
                    String name = player.getScoreboardName();
                    playerScores.put(name, value);
                } catch (Throwable ignored) {}
            }
        }

        return playerScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topCount)
                .map(e -> new ScoreEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private void applyLines(List<ScoreEntry> top) {
        List<String> newLines = new ArrayList<>();

        String header = headerFormat
                .replace("{objective}", objectiveName)
                .replace("{count}", String.valueOf(topCount));
        newLines.add(header);

        if (top.isEmpty()) {
            newLines.add(emptyFormat);
        } else {
            for (int i = 0; i < top.size(); i++) {
                ScoreEntry e = top.get(i);
                String line = playerFormat
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{player}", e.playerName)
                        .replace("{score}", String.valueOf(e.score))
                        .replace("{time}", isTimeObjective ? formatTime(e.score) : String.valueOf(e.score));
                newLines.add(line);
            }
        }

        // Update display without saving — scoreboard lines are computed, not user content.
        // The scoreboard config is saved separately by HologramManager.
        updateHologramContentNoSave(() -> {
            getLinesContentInternal().clear();
            getLinesContentInternal().addAll(newLines);
        });
    }

    private boolean scoresEqual(List<ScoreEntry> a, List<ScoreEntry> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ScoreEntry e1 = a.get(i);
            ScoreEntry e2 = b.get(i);
            if (!e1.playerName.equals(e2.playerName) || e1.score != e2.score) return false;
        }
        return true;
    }

    private boolean isTimeBasedObjective(String name) {
        String n = name.toLowerCase();
        return n.contains("time") ||
               n.contains("played") ||
               n.contains("playtime") ||
               n.contains("online") ||
               n.contains("session") ||
               n.equals("stat.playonetick");
    }

    private String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        if (totalSeconds < 60) return totalSeconds + "s";
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

    public String getObjectiveName() { return objectiveName; }
    public int getTopCount() { return topCount; }
    public int getUpdateInterval() { return updateIntervalSeconds; }
    public boolean isTimeObjective() { return isTimeObjective; }
    public int getRange() { return range; }
    public String getWorldName() { return getWorld(); }
    public double[] getLocation() { return new double[] { getX(), getY(), getZ() }; }

    // Getter methods for format fields
    public String getThemeName() { return themeName; }
    public String getHeaderFormat() { return headerFormat; }
    public String getPlayerFormat() { return playerFormat; }
    public String getEmptyFormat() { return emptyFormat; }

    private static class ScoreEntry {
        final String playerName;
        final int score;
        ScoreEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
    }
}


