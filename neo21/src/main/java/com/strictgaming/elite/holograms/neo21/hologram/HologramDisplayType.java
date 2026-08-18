package com.strictgaming.elite.holograms.neo21.hologram;

/**
 * How a hologram's lines are rendered.
 *
 * <p>{@link #FACING} uses invisible armor stand nameplates, which the client always turns to
 * face the viewer. {@link #FIXED} uses {@code text_display} entities with a fixed billboard,
 * so the text stays anchored at whatever yaw/pitch the hologram was given.
 */
public enum HologramDisplayType {

    FACING("facing"),
    FIXED("fixed");

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
     * Used when loading holograms saved before fixed holograms existed.
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
