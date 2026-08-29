package com.strictgaming.elite.holograms.forge.hologram;

/**
 * How a hologram's lines are rendered.
 *
 * <p>This mirrors the enum of the same name on the Forge 1.20.1, NeoForge 1.21.1 and
 * NeoForge 26.1 editions so the command vocabulary is identical across all of them.
 *
 * <p><strong>Only {@link #FACING} can actually be rendered on Minecraft 1.19.2.</strong>
 * A facing hologram is an invisible armor stand nameplate, which the client always turns
 * towards the viewer. A fixed hologram needs a {@code text_display} entity with a fixed
 * billboard, and {@code text_display} was not added to Minecraft until 1.19.4. Armor stand
 * nameplates are billboarded client side and cannot be pinned to a yaw or pitch, so there
 * is no way to approximate a fixed hologram here.
 *
 * <p>{@link #FIXED} is therefore still declared, but purely so that {@code /eh convert},
 * {@code /eh setrotation} and the {@code fixed} keyword on the create commands can
 * recognise the word and explain the limitation, rather than failing as unknown commands.
 * Check {@link #isSupported()} before acting on a value.
 */
public enum HologramDisplayType {

    FACING("facing"),
    FIXED("fixed");

    /**
     * Shared explanation for why fixed holograms cannot be used on this edition. Kept here so
     * every command reports the limitation with the same wording.
     */
    public static final String UNSUPPORTED_MESSAGE =
            "&c&l(!) &cFixed holograms need Minecraft &f1.19.4&c or newer, because they are built on "
                    + "&ftext_display&c entities. This is the Forge &f1.19.2&c edition, where holograms are "
                    + "armor stand nameplates that the client always turns to face the viewer.";

    /**
     * Follow-up line pointing at the editions that do support fixed holograms.
     */
    public static final String UNSUPPORTED_HINT =
            "&7Use the Forge &f1.20.1&7, NeoForge &f1.21.1&7 or NeoForge &f26.1&7 build for fixed holograms.";

    private final String serializedName;

    HologramDisplayType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * @return the value written to and read from {@code holograms.json}
     */
    public String getSerializedName() {
        return this.serializedName;
    }

    /**
     * @return whether this display type can be rendered on Minecraft 1.19.2
     */
    public boolean isSupported() {
        return this == FACING;
    }

    /**
     * Parses a user or config supplied display type. {@code face} is accepted as an alias for
     * {@code facing} so the wording used by {@code /eh convert <id> fixed|face} works.
     *
     * @param value the raw value, may be null
     * @return the matching type, or null when the value is not recognised
     */
    public static HologramDisplayType fromString(String value) {
        if (value == null) {
            return null;
        }

        switch (value.trim().toLowerCase()) {
            case "fixed":
                return FIXED;
            case "facing":
            case "face":
                return FACING;
            default:
                return null;
        }
    }

    /**
     * Parses a display type, falling back to a default when the value is missing or unknown.
     *
     * @param value        the raw value, may be null
     * @param defaultValue the value to use when parsing fails
     * @return the parsed type or {@code defaultValue}
     */
    public static HologramDisplayType fromStringOrDefault(String value, HologramDisplayType defaultValue) {
        HologramDisplayType parsed = fromString(value);
        return parsed == null ? defaultValue : parsed;
    }
}
