package com.strictgaming.elite.holograms.neo262.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and stores the named scoreboard themes from {@code scoreboard_themes.json}.
 *
 * <p>On first run the file is created pre-populated with a set of starter themes so server
 * owners have working examples to copy from. The file is never overwritten once it exists, so
 * any edits owners make (changing colours, adding new themes) are preserved. Missing starter
 * themes are re-added on load so deleting the whole file - or upgrading to a build with new
 * starters - restores the examples without clobbering customisations.</p>
 *
 * <p>Format strings support legacy {@code &} colour codes as well as MiniMessage tags such as
 * {@code <gradient:#FF0000:#00FF00>...</gradient>} and {@code <rainbow>...</rainbow>}.</p>
 */
public final class ScoreboardThemeManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type CONFIG_TYPE = new TypeToken<ThemeFile>() {}.getType();

    public static final String DEFAULT_THEME_NAME = "default";

    private static File themeFile;
    private static Map<String, ScoreboardTheme> themes = new LinkedHashMap<>();
    private static String defaultThemeName = DEFAULT_THEME_NAME;

    private ScoreboardThemeManager() {
    }

    /**
     * Initialise the theme store, creating the config file with starter themes if absent.
     *
     * @param configDir the mod config directory
     */
    public static synchronized void init(File configDir) {
        try {
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }
            themeFile = new File(configDir, "scoreboard_themes.json");

            ThemeFile loaded = read();
            Map<String, ScoreboardTheme> merged = new LinkedHashMap<>();

            // Start from the built-in starters so new defaults always appear...
            merged.putAll(defaultThemes());

            // ...then overlay whatever the owner has on disk so their edits win.
            boolean addedMissing = false;
            if (loaded != null && loaded.themes != null) {
                merged.putAll(loaded.themes);
            } else {
                addedMissing = true; // file missing or empty -> we'll write starters out
            }

            themes = merged;
            defaultThemeName = (loaded != null && loaded.defaultTheme != null && !loaded.defaultTheme.isEmpty())
                    ? loaded.defaultTheme
                    : DEFAULT_THEME_NAME;

            // Write back if the file did not exist yet, so owners can see and edit the starters.
            if (loaded == null || addedMissing) {
                write();
                LOGGER.info("Created scoreboard themes config with {} starter themes", themes.size());
            } else {
                LOGGER.info("Loaded {} scoreboard themes", themes.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialise scoreboard themes, falling back to built-in defaults", e);
            themes = defaultThemes();
            defaultThemeName = DEFAULT_THEME_NAME;
        }
    }

    /**
     * Re-reads the themes from disk without restarting the server. Owners can edit
     * {@code scoreboard_themes.json} and run {@code /eh reload} to apply changes. Built-in
     * starters are merged back in so deletions of individual starter entries are restored,
     * while owner edits and custom themes on disk take precedence.
     */
    public static synchronized void reload() {
        if (themeFile == null) {
            return;
        }
        try {
            ThemeFile loaded = read();
            Map<String, ScoreboardTheme> merged = new LinkedHashMap<>();
            merged.putAll(defaultThemes());
            if (loaded != null && loaded.themes != null) {
                merged.putAll(loaded.themes);
            }
            themes = merged;
            defaultThemeName = (loaded != null && loaded.defaultTheme != null && !loaded.defaultTheme.isEmpty())
                    ? loaded.defaultTheme
                    : DEFAULT_THEME_NAME;
            LOGGER.info("Reloaded {} scoreboard themes", themes.size());
        } catch (Exception e) {
            LOGGER.error("Failed to reload scoreboard themes", e);
        }
    }

    /**
     * @param name the theme name (case-insensitive)
     * @return the theme, or {@code null} if no theme by that name exists
     */
    public static synchronized ScoreboardTheme getTheme(String name) {        if (name == null) {
            return null;
        }
        ScoreboardTheme direct = themes.get(name);
        if (direct != null) {
            return direct;
        }
        // Case-insensitive lookup so tab-completed and hand-typed names both resolve.
        for (Map.Entry<String, ScoreboardTheme> entry : themes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * @return the configured default theme name (used when a board is created without one)
     */
    public static synchronized String getDefaultThemeName() {
        return defaultThemeName;
    }

    /**
     * @return an immutable view of all known theme names, for tab completion
     */
    public static synchronized Set<String> getThemeNames() {
        return Collections.unmodifiableSet(new LinkedHashMap<>(themes).keySet());
    }

    private static ThemeFile read() {
        if (themeFile == null || !themeFile.exists()) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(themeFile), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, CONFIG_TYPE);
        } catch (Exception e) {
            LOGGER.error("Error reading scoreboard themes config: {}", e.getMessage());
            return null;
        }
    }

    private static void write() {
        if (themeFile == null) {
            return;
        }
        try {
            File parent = themeFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ThemeFile out = new ThemeFile();
            out._comment = helpLines();
            out.defaultTheme = defaultThemeName;
            out.themes = themes;
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(themeFile), StandardCharsets.UTF_8)) {
                GSON.toJson(out, CONFIG_TYPE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Error writing scoreboard themes config: {}", e.getMessage());
        }
    }

    private static List<String> helpLines() {
        List<String> help = new ArrayList<>();
        help.add("EliteHolograms scoreboard themes. Each theme styles a scoreboard hologram.");
        help.add("Use a theme with: /eh createscoreboard <id> <objective> [topCount] [interval] [theme]");
        help.add("Colours: legacy '&' codes (e.g. &a, &6, &l) OR MiniMessage tags like");
        help.add("  <gradient:#FF0000:#00FF00>text</gradient> and <rainbow>text</rainbow>.");
        help.add("Tokens - header: {objective} {count}. player/timePlayer rows: {rank} {player} {score} {time}.");
        help.add("'timePlayer' is used for time-based objectives (playtime etc.); if omitted, 'player' is used.");
        help.add("'defaultTheme' is applied when a board is created without naming a theme.");
        help.add("Edit these or add your own entries under 'themes'. Delete this file to restore the starters.");
        return help;
    }

    /**
     * The built-in starter themes. These are written to disk on first run.
     */
    private static Map<String, ScoreboardTheme> defaultThemes() {
        Map<String, ScoreboardTheme> map = new LinkedHashMap<>();

        // Classic look - matches the original hardcoded formatting.
        map.put("default", new ScoreboardTheme(
                "&6&l{objective} &8- &7Top {count}",
                "&e{rank}. &f{player} &7- &a{score}",
                "&e{rank}. &f{player} &7- &a{time}",
                "&7No data available"));

        // Cool blues.
        map.put("ocean", new ScoreboardTheme(
                "&b&l{objective} &3- Top {count}",
                "&3{rank}. &b{player} &7- &f{score}",
                "&3{rank}. &b{player} &7- &f{time}",
                "&3No data available"));

        // Warm reds.
        map.put("blood", new ScoreboardTheme(
                "&4&l{objective} &c- Top {count}",
                "&c{rank}. &7{player} &8- &6{score}",
                "&c{rank}. &7{player} &8- &6{time}",
                "&8No data yet"));

        // Clean greyscale.
        map.put("mono", new ScoreboardTheme(
                "&f&l{objective} &8- Top {count}",
                "&8{rank}. &f{player} &7- &f{score}",
                "&8{rank}. &f{player} &7- &f{time}",
                "&8No data"));

        // Gold/medal vibe.
        map.put("gold", new ScoreboardTheme(
                "&6&l{objective} &e- Top {count}",
                "&e{rank}. &f{player} &7- &6{score}",
                "&e{rank}. &f{player} &7- &6{time}",
                "&7No data"));

        // MiniMessage gradient/rainbow showcase.
        map.put("rainbow", new ScoreboardTheme(
                "<rainbow><bold>{objective} - Top {count}</bold></rainbow>",
                "<gradient:#FFD700:#FF8C00>{rank}.</gradient> &f{player} &7- &a{score}",
                "<gradient:#FFD700:#FF8C00>{rank}.</gradient> &f{player} &7- &a{time}",
                "&7No data available"));

        return map;
    }

    /**
     * On-disk representation of the themes file.
     */
    private static class ThemeFile {
        // Human-readable help shown at the top of the JSON file.
        public List<String> _comment;
        public String defaultTheme;
        public Map<String, ScoreboardTheme> themes;
    }
}
