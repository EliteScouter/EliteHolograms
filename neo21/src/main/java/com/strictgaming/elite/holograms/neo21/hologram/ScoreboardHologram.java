package com.strictgaming.elite.holograms.neo21.hologram;

import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
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
    private final String headerFormat;
    private final String playerFormat;
    private final String emptyFormat;
    private final boolean isTimeObjective;
    private final int range;

    private long lastUpdateMs = 0L;
    private List<ScoreEntry> lastScores = new ArrayList<>();

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
            String headerFormat,
            String playerFormat,
            String emptyFormat
    ) {
        super(id, worldName, x, y, z, new ArrayList<>());

        this.objectiveName = objectiveName;
        this.topCount = Math.max(1, Math.min(topCount, 10));
        this.updateIntervalSeconds = Math.max(5, updateIntervalSeconds);
        this.isTimeObjective = isTimeBasedObjective(objectiveName);
        this.headerFormat = headerFormat != null ? headerFormat : "§6§l{objective} - Top {count}";
        this.playerFormat = playerFormat != null ? playerFormat : (this.isTimeObjective
                ? "§e{rank}. §f{player} §7- §a{time}"
                : "§e{rank}. §f{player} §7- §a{score}");
        this.emptyFormat = emptyFormat != null ? emptyFormat : "§7No data available";
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

    @Override
    public boolean isPlayerNearby(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || player.level() == null) return false;
        String playerWorld = player.level().dimension().location().toString();
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

        // Try to enumerate all tracked entries for the objective (includes offline players)
        Map<String, Integer> playerScores = collectScoresForObjective(scoreboard, objective);

        // Fallback: include online players (ensures at least some data if reflection fails)
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

    /**
     * Attempts to list all scoreboard entries for an objective, including offline holders, using
     * reflection for cross-mapping compatibility in 1.21.1.
     */
    private Map<String, Integer> collectScoresForObjective(Scoreboard scoreboard, Objective objective) {
        Map<String, Integer> result = new HashMap<>();
        try {
            // Try Scoreboard#getPlayerScores(Objective) first (older API)
            try {
                java.lang.reflect.Method m = Scoreboard.class.getMethod("getPlayerScores", Objective.class);
                Object collection = m.invoke(scoreboard, objective);
                if (collection instanceof java.util.Collection<?> col) {
                    for (Object scoreObj : col) {
                        if (scoreObj == null) continue;
                        // Try getOwner()/getScore() (older API)
                        String name = null;
                        Integer value = null;
                        try {
                            java.lang.reflect.Method getOwner = scoreObj.getClass().getMethod("getOwner");
                            Object owner = getOwner.invoke(scoreObj);
                            if (owner instanceof String s) name = s;
                        } catch (NoSuchMethodException ignored) {
                            try {
                                java.lang.reflect.Method owner = scoreObj.getClass().getMethod("owner");
                                Object holder = owner.invoke(scoreObj);
                                if (holder != null) {
                                    try {
                                        java.lang.reflect.Method getName = holder.getClass().getMethod("getScoreboardName");
                                        Object n = getName.invoke(holder);
                                        if (n instanceof String s) name = s;
                                    } catch (NoSuchMethodException nsme) {
                                        // attempt getName().getString()
                                        try {
                                            java.lang.reflect.Method getName = holder.getClass().getMethod("getName");
                                            Object comp = getName.invoke(holder);
                                            if (comp != null) {
                                                java.lang.reflect.Method getString = comp.getClass().getMethod("getString");
                                                Object n = getString.invoke(comp);
                                                if (n instanceof String s) name = s;
                                            }
                                        } catch (Throwable ignored2) {}
                                    }
                                }
                            } catch (Throwable ignored2) {}
                        }
                        try {
                            java.lang.reflect.Method getScore = scoreObj.getClass().getMethod("getScore");
                            Object v = getScore.invoke(scoreObj);
                            if (v instanceof Integer i) value = i;
                        } catch (NoSuchMethodException ignored) {
                            try {
                                java.lang.reflect.Method valueM = scoreObj.getClass().getMethod("value");
                                Object v = valueM.invoke(scoreObj);
                                if (v instanceof Integer i) value = i;
                            } catch (Throwable ignored2) {}
                        }
                        if (name != null && value != null) {
                            result.put(name, value);
                        }
                    }
                }
                if (!result.isEmpty()) return result;
            } catch (NoSuchMethodException ignored) {
                // Continue to next strategy
            }

            // Try Scoreboard#getTrackedPlayers() returning a collection of identifiers
            java.lang.reflect.Method getTracked;
            try {
                getTracked = Scoreboard.class.getMethod("getTrackedPlayers");
            } catch (NoSuchMethodException nsme) {
                // Some mappings use getTrackedPlayersForObjective(Objective)
                try {
                    getTracked = Scoreboard.class.getMethod("getTrackedPlayersForObjective", Objective.class);
                    Object collection = getTracked.invoke(scoreboard, objective);
                    fillScoresFromTrackedCollection(scoreboard, objective, collection, result);
                    return result;
                } catch (NoSuchMethodException nsme2) {
                    return result;
                }
            }
            Object collection = getTracked.invoke(scoreboard);
            fillScoresFromTrackedCollection(scoreboard, objective, collection, result);
        } catch (Throwable ignored) {}
        return result;
    }

    private void fillScoresFromTrackedCollection(Scoreboard scoreboard, Objective objective, Object collection, Map<String, Integer> out) {
        if (!(collection instanceof java.util.Collection<?> col)) return;
        for (Object entry : col) {
            if (entry == null) continue;
            String name = null;
            Object holder = null;
            try {
                if (entry instanceof String s) {
                    name = s;
                    // Attempt getOrCreatePlayerScore(String, Objective)
                    try {
                        java.lang.reflect.Method getOrCreate = Scoreboard.class.getMethod("getOrCreatePlayerScore", String.class, Objective.class);
                        Object sc = getOrCreate.invoke(scoreboard, name, objective);
                        Integer val = extractScoreValue(sc);
                        if (val != null) out.put(name, val);
                        continue;
                    } catch (NoSuchMethodException ignored) {}
                }
                // If not String, assume ScoreHolder-like
                holder = entry;
                // Try to get name
                try {
                    java.lang.reflect.Method getName = holder.getClass().getMethod("getScoreboardName");
                    Object n = getName.invoke(holder);
                    if (n instanceof String s2) name = s2;
                } catch (NoSuchMethodException ignored) {}
                // getOrCreatePlayerScore(ScoreHolder, Objective)
                try {
                    java.lang.reflect.Method getOrCreate = Scoreboard.class.getMethod("getOrCreatePlayerScore", holder.getClass(), Objective.class);
                    Object sc = getOrCreate.invoke(scoreboard, holder, objective);
                    Integer val = extractScoreValue(sc);
                    if (name != null && val != null) out.put(name, val);
                } catch (NoSuchMethodException ignored) {}
            } catch (Throwable ignored) {}
        }
    }

    private Integer extractScoreValue(Object scoreAccessLike) {
        if (scoreAccessLike == null) return null;
        try {
            java.lang.reflect.Method get = scoreAccessLike.getClass().getMethod("get");
            Object v = get.invoke(scoreAccessLike);
            if (v instanceof Integer i) return i;
        } catch (NoSuchMethodException ignored) {
            try {
                java.lang.reflect.Method value = scoreAccessLike.getClass().getMethod("value");
                Object v = value.invoke(scoreAccessLike);
                if (v instanceof Integer i) return i;
            } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}
        return null;
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

        // Update all lines in one operation
        setLines(newLines);
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

    private static class ScoreEntry {
        final String playerName;
        final int score;
        ScoreEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
    }
}


