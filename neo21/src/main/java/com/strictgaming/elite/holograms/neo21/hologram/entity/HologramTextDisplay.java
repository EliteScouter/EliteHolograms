package com.strictgaming.elite.holograms.neo21.hologram.entity;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * A {@code text_display} that is never added to the level. It exists purely so the mod has
 * something to build spawn and data packets from.
 *
 * <p>Vanilla keeps every text display mutator private, so configuration goes through
 * {@link #readAdditionalSaveData(CompoundTag)}, which is protected and therefore reachable from
 * a subclass. That covers one-off setup; per-player text updates take the faster path in
 * {@link #buildTextData(Component)}.
 */
public class HologramTextDisplay extends Display.TextDisplay {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Nameplates never wrap, so give the display effectively unlimited width. The vanilla
     * default is 200, which would wrap long hologram lines that the armor stand backing shows
     * on a single line.
     */
    private static final int NO_WRAP_LINE_WIDTH = Integer.MAX_VALUE;

    /** Written once while locating the synched data field that holds the text. */
    private static final String PROBE_TEXT = "EliteHologramsProbe";

    /**
     * The synched data key backing the text field. Vanilla declares it privately, so it is
     * discovered at runtime the first time a display is built and then shared, since the id is
     * identical for every text display.
     */
    private static volatile EntityDataAccessor<Component> textAccessor;

    /** Set when discovery has run, so a failure is not retried (and re-logged) forever. */
    private static volatile boolean textAccessorResolved;

    public HologramTextDisplay(ServerLevel level) {
        super(EntityType.TEXT_DISPLAY, level);
    }

    /**
     * Applies the display settings shared by every hologram line: a fixed billboard so the
     * text keeps the hologram's rotation, centred text, and no wrapping.
     *
     * <p>The remaining vanilla defaults already match how a nameplate looks - 25% black
     * background, no shadow, not see-through - so they are left alone.
     */
    public void applyHologramDefaults() {
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", Display.BillboardConstraints.FIXED.getSerializedName());
        tag.putString("alignment", Display.TextDisplay.Align.CENTER.getSerializedName());
        tag.putInt("line_width", NO_WRAP_LINE_WIDTH);
        this.readAdditionalSaveData(tag);

        // Resolve the text field now rather than lazily on the first packet build. Discovery
        // writes a probe value into this entity, and buildTextData is documented not to mutate
        // it - doing the discovery there would leave the probe text sitting in the entity for
        // the next viewer's spawn snapshot to pick up. Clearing it here also means a line whose
        // text is never set renders empty rather than showing the probe.
        setHologramText(Component.empty());
    }

    /**
     * Stores the text on this entity so the next full snapshot sent to a freshly spawning
     * viewer carries it.
     *
     * @param text the rendered line text
     */
    public void setHologramText(Component text) {
        EntityDataAccessor<Component> accessor = resolveTextAccessor();

        if (accessor != null) {
            this.getEntityData().set(accessor, text);
        }
    }

    /**
     * Builds the payload for a text-only data packet.
     *
     * <p>This deliberately does not mutate the entity. Hologram lines are shared by every
     * viewer but placeholders resolve per player, so writing to the shared entity and then
     * draining its dirty flags would give the first viewer the update and everyone else
     * nothing.
     *
     * @param text the rendered line text for one viewer
     * @return the packet payload, or an empty list when the text field could not be resolved
     */
    public List<SynchedEntityData.DataValue<?>> buildTextData(Component text) {
        EntityDataAccessor<Component> accessor = resolveTextAccessor();

        if (accessor == null) {
            return Collections.emptyList();
        }

        return List.of(new SynchedEntityData.DataValue<>(accessor.id(), EntityDataSerializers.COMPONENT, text));
    }

    private EntityDataAccessor<Component> resolveTextAccessor() {
        if (textAccessorResolved) {
            return textAccessor;
        }

        synchronized (HologramTextDisplay.class) {
            if (!textAccessorResolved) {
                textAccessor = discoverTextAccessor();
                textAccessorResolved = true;
            }
        }

        return textAccessor;
    }

    /**
     * Locates the synched data id of the text field without reflecting on vanilla internals.
     *
     * <p>The field defaults to an empty component, so it only shows up in
     * {@link SynchedEntityData#getNonDefaultValues()} once something has been written to it.
     * A probe value is written through the NBT path, then the single entry using the component
     * serializer is picked out - on a text display, the text is the only such field.
     */
    private EntityDataAccessor<Component> discoverTextAccessor() {
        CompoundTag probe = new CompoundTag();
        // Quoted so it parses as a plain-text component.
        probe.putString("text", "\"" + PROBE_TEXT + "\"");
        // Vanilla logs an error when "alignment" is absent, so always include it.
        probe.putString("alignment", Display.TextDisplay.Align.CENTER.getSerializedName());
        this.readAdditionalSaveData(probe);

        List<SynchedEntityData.DataValue<?>> values = this.getEntityData().getNonDefaultValues();

        if (values != null) {
            for (SynchedEntityData.DataValue<?> value : values) {
                if (value.serializer() == EntityDataSerializers.COMPONENT) {
                    LOGGER.debug("Resolved text display text field to synched data id {}", value.id());
                    return new EntityDataAccessor<>(value.id(), EntityDataSerializers.COMPONENT);
                }
            }
        }

        LOGGER.error("Could not resolve the text display text field - fixed holograms will render blank. "
                + "This build of Elite Holograms may not match the running Minecraft version.");
        return null;
    }
}
