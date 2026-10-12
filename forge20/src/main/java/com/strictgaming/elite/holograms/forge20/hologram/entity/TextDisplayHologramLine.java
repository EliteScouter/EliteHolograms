package com.strictgaming.elite.holograms.forge20.hologram.entity;

import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge20.util.UtilPlaceholder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * A hologram line rendered as a {@code text_display} with a fixed billboard, so the text stays
 * anchored at the hologram's yaw and pitch instead of turning towards the viewer. This is the
 * {@code fixed} display type.
 */
public class TextDisplayHologramLine implements HologramLineRenderer {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    /**
     * How far above its own origin a single-line text display draws the top of its text, at
     * scale 1.
     *
     * <p>This constant is version-specific and is NOT a copy/paste slip against the other
     * editions. Vanilla lays a text display out as {@code lines * 10} font pixels tall up to
     * 1.21.1 and {@code lines * 10 - 1} from 26.1 onwards, so the text top sits 0.25 above the
     * origin here and 0.225 there. Check {@code DisplayRenderer.TextDisplayRenderer.renderInner}
     * before changing it.
     */
    private static final double TEXT_TOP_ABOVE_ORIGIN = 0.25D;

    /** A nameplate's text is drawn with its top at {@code entityY + bbHeight + 0.5}. */
    private static final double NAMEPLATE_TEXT_TOP = 0.5D;

    /**
     * Vertical gap between where an armor stand sits and where a text display has to sit for
     * their text to land at the same height.
     *
     * <p>The text top scales with the display, so the offset has to shrink as the scale grows
     * to keep the top line anchored where the hologram is. At scale 1 this is the historic
     * {@code bbHeight + 0.25}, so an unscaled hologram sits exactly where it always did.
     *
     * @param scale the display's uniform scale
     * @return the offset to add to a line slot's Y coordinate
     */
    /**
     * How far above a line slot that line's text is actually drawn.
     *
     * <p>The backing entity sits at the slot, but the text renders a nameplate height
     * above it, which is the whole point of {@link #textOffset(float)}. Anything that
     * needs to line something up with the visible text, rather than with the entity, has
     * to add this. It does not vary with scale: the offset above shrinks as the scale
     * grows precisely so the text top stays put.
     *
     * @return the vertical distance from a line slot to the top of its text
     */
    public static double textTopOffset() {
        return EntityType.ARMOR_STAND.getHeight() + NAMEPLATE_TEXT_TOP;
    }

    public static double textOffset(float scale) {
        return EntityType.ARMOR_STAND.getHeight() + NAMEPLATE_TEXT_TOP - (TEXT_TOP_ABOVE_ORIGIN * scale);
    }

    protected HologramTextDisplay display;
    protected String text;

    /** The hologram's line-slot position, before the vertical offset is applied. */
    private double slotX;
    private double slotY;
    private double slotZ;
    private float yaw;
    private float pitch;
    private int backgroundArgb = HologramTextDisplay.DEFAULT_BACKGROUND;
    private float scale = HologramTextDisplay.DEFAULT_SCALE;
    private int brightness = HologramTextDisplay.BRIGHTNESS_AUTO;
    private Display.BillboardConstraints billboard = Display.BillboardConstraints.FIXED;

    public TextDisplayHologramLine(Level world, double x, double y, double z, float yaw, float pitch) {
        this(world, x, y, z, yaw, pitch, HologramTextDisplay.DEFAULT_BACKGROUND);
    }

    public TextDisplayHologramLine(Level world, double x, double y, double z, float yaw, float pitch,
                                   int backgroundArgb) {
        this(world, x, y, z, yaw, pitch, backgroundArgb, HologramTextDisplay.DEFAULT_SCALE,
                HologramTextDisplay.BRIGHTNESS_AUTO, Display.BillboardConstraints.FIXED);
    }

    public TextDisplayHologramLine(Level world, double x, double y, double z, float yaw, float pitch,
                                   int backgroundArgb, float scale, int brightness,
                                   Display.BillboardConstraints billboard) {
        this.backgroundArgb = backgroundArgb;
        this.scale = scale;
        this.brightness = brightness;
        this.billboard = billboard;
        this.slotX = x;
        this.slotY = y;
        this.slotZ = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.display = createDisplay(world);
    }

    private HologramTextDisplay createDisplay(Level world) {
        HologramTextDisplay created = new HologramTextDisplay(world);
        created.setId(HologramEntityIds.next());
        created.setBackgroundArgb(this.backgroundArgb);
        created.setScale(this.scale);
        created.setBrightness(this.brightness);
        created.setBillboard(this.billboard);
        created.applyHologramDefaults();
        created.moveTo(this.slotX, this.slotY + textOffset(this.scale), this.slotZ, this.yaw, this.pitch);
        return created;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        this.display.setHologramText(renderText(null));
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public void spawnForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            player.connection.send(new ClientboundAddEntityPacket(this.display));

            // The display's own settings (billboard, alignment, wrapping) only reach the client
            // through a full snapshot, so send that on spawn.
            List<SynchedEntityData.DataValue<?>> snapshot = this.display.getEntityData().getNonDefaultValues();

            if (snapshot != null && !snapshot.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(this.display.getId(), snapshot));
            }

            updateForPlayer(player);
        } catch (Exception e) {
            LOGGER.warn("Error spawning fixed hologram line for player", e);
        }
    }

    @Override
    public void despawnForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            player.connection.send(new ClientboundRemoveEntitiesPacket(this.display.getId()));
        } catch (Exception e) {
            LOGGER.debug("Error despawning fixed hologram line for player", e);
        }
    }

    @Override
    public void updateForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            List<SynchedEntityData.DataValue<?>> textData = this.display.buildTextData(renderText(player));

            if (!textData.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(this.display.getId(), textData));
            }
        } catch (Exception e) {
            LOGGER.debug("Error updating fixed hologram line for player", e);
        }
    }

    @Override
    public void sendTeleportPacket(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            player.connection.send(new ClientboundTeleportEntityPacket(this.display));
        } catch (Exception e) {
            LOGGER.debug("Error sending teleport packet for fixed hologram line", e);
        }
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.slotX = x;
        this.slotY = y;
        this.slotZ = z;
        this.display.setPos(x, y + textOffset(this.scale), z);
        this.display.setOldPosAndRot();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.display.setYRot(yaw);
        this.display.setXRot(pitch);
        // Without this the client interpolates from the previous rotation on the next update.
        this.display.setOldPosAndRot();
    }

    @Override
    public void setWorld(Level world) {
        if (this.display.level() == world) {
            return;
        }

        // A display entity is bound to its level, so moving worlds means building a new one.
        // The network id is carried over so viewers that still track the old id get replaced
        // rather than duplicated.
        int previousId = this.display.getId();
        this.display = createDisplay(world);
        this.display.setId(previousId);
        this.display.setHologramText(renderText(null));
    }

    @Override
    public int getEntityId() {
        return this.display.getId();
    }

    /**
     * Resolves this line's text for a viewer.
     *
     * @param player the viewer, or null to resolve without per-player placeholders
     * @return the component to display
     */
    protected Component renderText(ServerPlayer player) {
        if (this.text == null || this.text.isEmpty() || "{empty}".equals(this.text)) {
            return Component.empty();
        }

        if (player != null && Forge20Holograms.getInstance().arePlaceholdersEnabled()) {
            try {
                return UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(player, this.text));
            } catch (Exception e) {
                LOGGER.debug("Error processing placeholders, using fallback text", e);
                return UtilChatColour.parse(this.text.replace("%", "%%"));
            }
        }

        if (Forge20Holograms.getInstance().arePlaceholdersEnabled()) {
            return UtilChatColour.parse(this.text);
        }

        return UtilChatColour.parse(this.text.replace("%", "%%"));
    }

    /**
     * Resizes this line. The vertical offset is recalculated because a scaled display draws
     * its text further from its own origin.
     *
     * @param scale the uniform scale; 1.0 is the natural size
     */
    @Override
    public void setScale(float scale) {
        this.scale = scale;
        this.display.setScale(scale);
        this.display.setPos(this.slotX, this.slotY + textOffset(scale), this.slotZ);
        this.display.setOldPosAndRot();
    }

    /**
     * @param brightness 0-15 to light the text regardless of its surroundings, or
     *                   {@link HologramTextDisplay#BRIGHTNESS_AUTO} to let the world light it
     */
    @Override
    public void setBrightness(int brightness) {
        this.brightness = brightness;
        this.display.setBrightness(brightness);
    }

    /**
     * @param billboard how this line should turn towards the viewer
     */
    @Override
    public void setBillboard(Display.BillboardConstraints billboard) {
        this.billboard = billboard;
        this.display.setBillboard(billboard);
    }

    /**
     * Changes this line's background colour.
     *
     * <p>The colour lives in the display's synched data rather than in the text payload, so
     * viewers only see it after a settings snapshot (see {@link #sendSettingsSnapshot(ServerPlayer)}).
     *
     * @param argb the packed ARGB background; alpha 0 hides the background entirely
     */
    @Override
    public void setBackgroundArgb(int argb) {
        this.backgroundArgb = argb;
        this.display.setBackgroundArgb(argb);
    }

    /**
     * Re-sends this line's full data snapshot, then its text.
     *
     * <p>Used after a settings change such as the background colour. The snapshot carries the
     * shared, placeholder-free text, so the per-player text is pushed straight after it to stop
     * a viewer briefly seeing another player's resolved line.
     *
     * @param player the viewer
     */
    @Override
    public void sendSettingsSnapshot(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            List<SynchedEntityData.DataValue<?>> snapshot = this.display.getEntityData().getNonDefaultValues();

            if (snapshot != null && !snapshot.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(this.display.getId(), snapshot));
            }

            updateForPlayer(player);
        } catch (Exception e) {
            LOGGER.debug("Error refreshing fixed hologram line settings for player", e);
        }
    }

    /**
     * @return the underlying packet-only display entity
     */
    public HologramTextDisplay getDisplay() {
        return this.display;
    }
}
