package com.strictgaming.elite.holograms.forge20.hologram.entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared source of network entity IDs for the fake, packet-only entities holograms send to
 * players.
 *
 * <p>The IDs sit far below anything the server itself allocates so they cannot collide with
 * real entities. A single counter is used across every hologram entity type - armor stand
 * lines, text display lines and item stands - because separate per-class counters seeded with
 * the same start value hand out identical IDs, which makes the client apply one line's data
 * to another.
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
