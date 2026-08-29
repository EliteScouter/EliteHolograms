package com.strictgaming.elite.holograms.neo262.hologram.entity;

import net.minecraft.server.level.ServerPlayer;

/**
 * A single rendered line of a hologram.
 *
 * <p>Two backings exist: {@link HologramLine} spawns an invisible armor stand and shows the
 * text as its nameplate, which the client always turns towards the viewer, and
 * {@link TextDisplayHologramLine} spawns a {@code text_display} whose orientation stays fixed
 * at the hologram's yaw/pitch.
 *
 * <p>Every line is a packet-only entity: it is never added to the level, so all state changes
 * have to be pushed to viewers explicitly.
 */
public interface HologramLineRenderer {

    /**
     * Sends the spawn packets for this line, followed by a full data snapshot.
     *
     * @param player the viewer
     */
    void spawnToPlayer(ServerPlayer player);

    /**
     * Pushes the current text to a viewer, resolving any per-player placeholders.
     *
     * @param player         the viewer
     * @param isInitialSpawn when true a full data snapshot is sent rather than just changes
     */
    void updateForPlayer(ServerPlayer player, boolean isInitialSpawn);

    /**
     * Removes this line from a viewer's world.
     *
     * @param player the viewer
     */
    void despawnFromPlayer(ServerPlayer player);

    /**
     * Moves this line. Implementations apply their own vertical offset so that the given
     * coordinate means the same thing for every backing.
     *
     * @param x the X coordinate
     * @param y the Y coordinate of the hologram's line slot
     * @param z the Z coordinate
     */
    void setPosition(double x, double y, double z);

    /**
     * Pushes the current position and rotation to a viewer.
     *
     * @param player the viewer
     */
    void sendTeleportPacket(ServerPlayer player);

    /**
     * @return the unresolved text of this line
     */
    String getText();

    /**
     * Sets the orientation of this line. No-op for backings that always face the viewer.
     *
     * @param yaw   rotation around the Y axis in degrees
     * @param pitch rotation around the X axis in degrees
     */
    default void setRotation(float yaw, float pitch) {
    }

    /**
     * @return whether this line cycles through frames and therefore needs ticking
     */
    default boolean isAnimated() {
        return false;
    }

    /**
     * Advances this line's animation by one tick.
     *
     * @return true when the visible frame changed and viewers need an update
     */
    default boolean tickAnimation() {
        return false;
    }
}
