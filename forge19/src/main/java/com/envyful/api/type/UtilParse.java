package com.envyful.api.type;

import java.util.Optional;

/**
 *
 * Utility for parsing types
 *
 */
public class UtilParse {

    /**
     *
     * Parse a string to an integer
     *
     * @param s The string to parse
     * @return An Optional containing the parsed integer, or empty if parsing failed
     */
    public static Optional<Integer> parseInteger(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     *
     * Parse a string to a double
     *
     * @param s The string to parse
     * @return An Optional containing the parsed double, or empty if parsing failed
     */
    public static Optional<Double> parseDouble(String s) {
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     *
     * Parse a string to a float
     *
     * @param s The string to parse
     * @return An Optional containing the parsed float, or empty if parsing failed
     */
    public static Optional<Float> parseFloat(String s) {
        try {
            return Optional.of(Float.parseFloat(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     *
     * Parse a string to a long
     *
     * @param s The string to parse
     * @return An Optional containing the parsed long, or empty if parsing failed
     */
    public static Optional<Long> parseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     *
     * Parse a string to a boolean
     *
     * @param s The string to parse
     * @return The parsed boolean
     */
    public static boolean parseBoolean(String s) {
        return Boolean.parseBoolean(s);
    }
} 