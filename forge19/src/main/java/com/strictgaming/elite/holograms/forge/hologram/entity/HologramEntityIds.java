package com.strictgaming.elite.holograms.forge.hologram.entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared source of network entity IDs for the fake, packet-only entities holograms send to
 * players.
 *
 * <p>The IDs sit far below anything the server itself allocates so they cannot collide with
 * real entities. A single counter is used across every hologram entity type - text lines and
 * item stands - because separate per-class counters eventually walk into each other: the line
 * counter started at {@code -1000} and the item stand counter at {@code -5000}, so a world with
 * more than four thousand hologram lines would hand the same ID to both, making the client
 * apply one entity's data to the other.
 */
public final class HologramEntityIds {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(-2000000000);

    private HologramEntityIds() {
    }

    /**
     * @return the next unused network entity ID
     */
    public static int next() {
        return NEXT_ID.getAndIncrement();
    }
}
