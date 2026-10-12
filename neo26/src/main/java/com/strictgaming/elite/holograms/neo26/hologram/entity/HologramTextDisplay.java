package com.strictgaming.elite.holograms.neo26.hologram.entity;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * A {@code text_display} that is never added to the level. It exists purely so the mod has
 * something to build spawn and data packets from.
 *
 * <p>Vanilla keeps every text display mutator private, so configuration goes through
 * {@link #readAdditionalSaveData}, which is protected and therefore reachable from a subclass.
 * That covers one-off setup; per-player text updates take the faster path in
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

    /**
     * What vanilla gives a text display when nothing sets a background: 25% opaque black
     * ({@code 0x40000000}). This is the look hologram lines have always had, so it stays the
     * default and existing holograms are unaffected.
     */
    public static final int DEFAULT_BACKGROUND = 0x40000000;

    /** Vanilla leaves a text display at its natural size. */
    public static final float DEFAULT_SCALE = 1.0F;

    /**
     * Sentinel for "no brightness override", i.e. the text is lit by the world like any
     * other entity. Vanilla models this as an absent tag rather than a value.
     */
    public static final int BRIGHTNESS_AUTO = -1;

    /** Current ARGB background, applied on every settings write. */
    private int backgroundArgb = DEFAULT_BACKGROUND;

    /** Uniform scale applied through the transformation matrix. */
    private float scale = DEFAULT_SCALE;

    /** 0-15 light override applied to both block and sky light, or {@link #BRIGHTNESS_AUTO}. */
    private int brightness = BRIGHTNESS_AUTO;

    /** How the display turns towards the viewer. */
    private Display.BillboardConstraints billboard = Display.BillboardConstraints.FIXED;

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
     * <p>The remaining vanilla defaults already match how a nameplate looks: 25% black
     * background, no shadow, not see-through, so they are left alone.
     */
    public void applyHologramDefaults() {
        applySettings();

        // Resolve the text field now rather than lazily on the first packet build. Discovery
        // writes a probe value into this entity, and buildTextData is documented not to mutate
        // it, and doing the discovery there would leave the probe text sitting in the entity for
        // the next viewer's spawn snapshot to pick up. Clearing it here also means a line whose
        // text is never set renders empty rather than showing the probe.
        setHologramText(Component.empty());
    }

    /**
     * @return the ARGB background currently applied to this display
     */
    public int getBackgroundArgb() {
        return this.backgroundArgb;
    }

    /**
     * Replaces the background colour and pushes it onto the entity.
     *
     * @param argb the packed ARGB background; alpha 0 hides the background entirely
     */
    public void setBackgroundArgb(int argb) {
        this.backgroundArgb = argb;
        applySettings();
    }

    /**
     * @return the uniform scale applied to this display
     */
    public float getScale() {
        return this.scale;
    }

    /**
     * @param scale the uniform scale; 1.0 is the natural size
     */
    public void setScale(float scale) {
        this.scale = scale;
        applySettings();
    }

    /**
     * @return the light override, or {@link #BRIGHTNESS_AUTO} when the world lights the text
     */
    public int getBrightness() {
        return this.brightness;
    }

    /**
     * @param brightness 0-15 to light the text regardless of its surroundings, or
     *                   {@link #BRIGHTNESS_AUTO} to let the world light it
     */
    public void setBrightness(int brightness) {
        this.brightness = brightness;
        applySettings();
    }

    /**
     * @return how this display turns towards the viewer
     */
    public Display.BillboardConstraints getBillboard() {
        return this.billboard;
    }

    /**
     * @param billboard how the display should turn towards the viewer
     */
    public void setBillboard(Display.BillboardConstraints billboard) {
        this.billboard = billboard;
        applySettings();
    }

    /**
     * Writes the complete display configuration in one go.
     *
     * <p>Every field is written every time on purpose. From 26.1 vanilla reads these with
     * {@code getIntOr}/{@code getByteOr}, which means a tag that omits a key resets that key to
     * its default rather than leaving it alone, so a partial write here would silently wipe a
     * custom background. Writing the full state keeps the behaviour identical on 1.21.1, where
     * the same read is conditional on the key being present.
     */
    private void applySettings() {
        applySaveData(buildSettingsTag());
    }

    /**
     * @return a tag carrying this display's full configuration
     */
    private CompoundTag buildSettingsTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", this.billboard.getSerializedName());
        tag.putString("alignment", Display.TextDisplay.Align.CENTER.getSerializedName());
        tag.putInt("line_width", NO_WRAP_LINE_WIDTH);
        tag.putInt("background", this.backgroundArgb);
        tag.put("transformation", buildTransformationTag());

        // Brightness is the one setting vanilla models as "absent means no override" (it
        // explicitly nulls the override when the key is missing), so auto is written by
        // leaving the key out rather than by writing a value.
        if (this.brightness != BRIGHTNESS_AUTO) {
            CompoundTag light = new CompoundTag();
            light.putInt("block", this.brightness);
            light.putInt("sky", this.brightness);
            tag.put("brightness", light);
        }

        return tag;
    }

    /**
     * Builds the transformation carrying this display scale.
     *
     * <p>Vanilla non-matrix transformation codec requires all four components, so the
     * rotations and translation are written as identity values alongside the scale.
     */
    private CompoundTag buildTransformationTag() {
        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", floats(0.0F, 0.0F, 0.0F));
        transformation.put("left_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
        transformation.put("scale", floats(this.scale, this.scale, this.scale));
        transformation.put("right_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
        return transformation;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();

        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }

        return list;
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

    /**
     * Feeds a tag through the entity's own save-data reader, which is how the otherwise private
     * display settings get applied.
     */
    private void applySaveData(CompoundTag tag) {
        this.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, this.registryAccess(), tag));
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
     * A probe value is written through the save-data path, then the single entry using the
     * component serializer is picked out: on a text display, the text is the only such field.
     */
    private EntityDataAccessor<Component> discoverTextAccessor() {
        // Built from the full settings tag so the probe write cannot reset any other field.
        // This matters from 26.1 on, where vanilla reads these with getIntOr/getByteOr and a
        // missing key resets that setting rather than leaving it alone.
        CompoundTag probe = buildSettingsTag();
        // The component codec accepts a bare string as literal text.
        probe.putString("text", PROBE_TEXT);
        applySaveData(probe);

        List<SynchedEntityData.DataValue<?>> values = this.getEntityData().getNonDefaultValues();

        if (values != null) {
            for (SynchedEntityData.DataValue<?> value : values) {
                if (value.serializer() == EntityDataSerializers.COMPONENT) {
                    LOGGER.debug("Resolved text display text field to synched data id {}", value.id());
                    return new EntityDataAccessor<>(value.id(), EntityDataSerializers.COMPONENT);
                }
            }
        }

        LOGGER.error("Could not resolve the text display text field, so fixed holograms will render blank. "
                + "This build of Elite Holograms may not match the running Minecraft version.");
        return null;
    }
}
