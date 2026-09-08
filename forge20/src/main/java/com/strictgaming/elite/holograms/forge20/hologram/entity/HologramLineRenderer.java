package com.strictgaming.elite.holograms.forge20.hologram.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

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
     * Replaces the text shown on this line.
     *
     * @param text the unresolved text, may contain colour codes and placeholders
     */
    void setText(String text);

    /**
     * @return the unresolved text of this line
     */
    String getText();

    /**
     * Sends the spawn packets for this line, followed by a data update.
     *
     * @param player the viewer
     */
    void spawnForPlayer(ServerPlayer player);

    /**
     * Removes this line from a viewer's world.
     *
     * @param player the viewer
     */
    void despawnForPlayer(ServerPlayer player);

    /**
     * Pushes the current text to a viewer, resolving any per-player placeholders.
     *
     * @param player the viewer
     */
    void updateForPlayer(ServerPlayer player);

    /**
     * Pushes the current position and rotation to a viewer.
     *
     * @param player the viewer
     */
    void sendTeleportPacket(ServerPlayer player);

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
     * Moves this line into a different level, recreating the backing entity if needed.
     *
     * @param world the new level
     */
    void setWorld(Level world);

    /**
     * @return the network entity id used in packets for this line
     */
    int getEntityId();

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
     * Advances this line's animation.
     *
     * @param currentTick the hologram's tick counter
     * @return true when the visible frame changed and viewers need an update
     */
    default boolean tickAnimation(long currentTick) {
        return false;
    }

    /**
     * @return this line's animation frames, or null when it is not animated. Used to carry
     *         content across a display type switch, which has to rebuild every line entity.
     */
    default List<String> getFrames() {
        return null;
    }

    /**
     * @return the tick interval between animation frames, or 0 when not animated
     */
    default int getIntervalTicks() {
        return 0;
    }
    /**
     * Sets the background colour of this line. No-op for backings whose background the server
     * cannot control - an armor stand nameplate is drawn with the viewer's own chat background
     * opacity, which is a client setting.
     *
     * @param argb the packed ARGB background; alpha 0 hides the background entirely
     */
    default void setBackgroundArgb(int argb) {
    }

    /**
     * Pushes a settings change (such as the background) to a viewer. No-op where there are no
     * server-controlled settings to push.
     *
     * @param player the viewer
     */
    default void sendSettingsSnapshot(ServerPlayer player) {
    }
}
