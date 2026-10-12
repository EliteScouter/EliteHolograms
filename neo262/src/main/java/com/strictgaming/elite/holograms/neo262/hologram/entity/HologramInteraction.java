package com.strictgaming.elite.holograms.neo262.hologram.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The invisible hitbox that makes a hologram clickable.
 *
 * <p>Unlike every other entity this mod creates, this one is added to the world for real rather
 * than being sent as packets. A packet-only entity does not exist server-side, so when a player
 * right-clicks it the interact packet is looked up against the level, finds nothing, and is
 * dropped. Catching it would mean mixing into the packet handler on every loader. A real
 * {@code minecraft:interaction} is inert (no AI, no ticking, no collision, invisible) and makes
 * the normal interaction events fire on their own, which keeps the feature working identically
 * on Forge, NeoForge and Fabric with no mixins at all.
 *
 * <p>The trade-off is that it lives in the world and would be saved with the chunk, so every
 * instance is tagged with {@link #HOLOGRAM_TAG} and swept on startup (see
 * {@code HologramManager.sweepOrphanedInteractions}). That covers the case where the server dies
 * without running its shutdown path.
 */
public class HologramInteraction extends Interaction {

    /**
     * Scoreboard tag marking an interaction as one of ours, so orphans left by a crash can be
     * identified and removed without touching anything a player or another mod placed.
     */
    public static final String HOLOGRAM_TAG = "eliteholograms_click";

    public HologramInteraction(Level level) {
        super(EntityTypes.INTERACTION, level);
        this.addTag(HOLOGRAM_TAG);
    }

    /**
     * Never written to the chunk.
     *
     * <p>These hitboxes are rebuilt from the hologram every time it spawns, so persisting
     * one only creates a stale copy. That copy used to survive a restart: the shutdown path
     * discards the entity through a Java reference, which does nothing if its chunk already
     * unloaded and serialised it, and the startup sweep only sees chunks that are loaded -
     * which, for a hologram away from spawn, it is not. The result was two overlapping
     * hitboxes, one of them unregistered, so clicking it swung the arm and ran nothing.
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /**
     * Sizes the hitbox.
     *
     * <p>Vanilla keeps the width and height setters private, so this goes through the save-data
     * reader, the same way the text display is configured.
     *
     * @param width  the hitbox width in blocks
     * @param height the hitbox height in blocks
     */
    public void applySize(float width, float height) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("width", width);
        tag.putFloat("height", height);
        // Makes the player's arm swing on click, so the hologram feels like it responded.
        tag.putBoolean("response", true);
        // 26.x reads save data through a ValueInput rather than a raw tag.
        this.readAdditionalSaveData(
                TagValueInput.create(ProblemReporter.DISCARDING, this.registryAccess(), tag));
    }
}
