package com.strictgaming.elite.holograms.api.exception;

/**
 *
 * Exception thrown when a hologram operation fails
 *
 */
public class HologramException extends Exception {

    /**
     *
     * Constructor for creating a new hologram exception
     *
     * @param message The error message
     */
    public HologramException(String message) {
        super(message);
    }

    /**
     *
     * Constructor for creating a new hologram exception with a cause
     *
     * @param message The error message
     * @param cause The cause of the error
     */
    public HologramException(String message, Throwable cause) {
        super(message, cause);
    }
} 
