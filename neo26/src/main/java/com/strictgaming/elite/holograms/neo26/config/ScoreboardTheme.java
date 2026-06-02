package com.strictgaming.elite.holograms.neo26.config;

/**
 * A reusable visual style for scoreboard holograms.
 *
 * <p>A theme is just a bundle of format strings with colour codes (using the {@code &} or
 * {@code §} prefix) and placeholder tokens. When a scoreboard hologram is created with a theme,
 * the theme's format strings are applied to the header, each player row, and the "no data" line.</p>
 *
 * <p>Supported tokens:</p>
 * <ul>
 *     <li>{@code {objective}} - the objective name (header)</li>
 *     <li>{@code {count}} - the configured top count (header)</li>
 *     <li>{@code {rank}} - the player's position, 1-based (player rows)</li>
 *     <li>{@code {player}} - the player's name (player rows)</li>
 *     <li>{@code {score}} - the raw score value (player rows)</li>
 *     <li>{@code {time}} - the score formatted as a duration, for time-based objectives (player rows)</li>
 * </ul>
 */
public class ScoreboardTheme {

    private String header;
    private String player;
    private String timePlayer;
    private String empty;

    public ScoreboardTheme() {
    }

    public ScoreboardTheme(String header, String player, String timePlayer, String empty) {
        this.header = header;
        this.player = player;
        this.timePlayer = timePlayer;
        this.empty = empty;
    }

    public String getHeader() {
        return header;
    }

    public String getPlayer() {
        return player;
    }

    /**
     * @return the row format used for time-based objectives, falling back to the standard
     *         player format when not defined.
     */
    public String getTimePlayer() {
        return timePlayer != null ? timePlayer : player;
    }

    public String getEmpty() {
        return empty;
    }

    /**
     * Picks the correct player-row format for the objective type.
     *
     * @param timeBased whether the objective tracks time/ticks
     * @return the time row format when {@code timeBased}, otherwise the standard row format
     */
    public String playerFormatFor(boolean timeBased) {
        return timeBased ? getTimePlayer() : getPlayer();
    }
}
