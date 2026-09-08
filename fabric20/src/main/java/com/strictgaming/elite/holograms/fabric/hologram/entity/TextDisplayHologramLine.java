package com.strictgaming.elite.holograms.fabric.hologram.entity;

import com.strictgaming.elite.holograms.fabric.FabricHolograms;
import com.strictgaming.elite.holograms.fabric.util.UtilChatColour;
import com.strictgaming.elite.holograms.fabric.util.UtilPlaceholder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
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
     * Vertical gap between where an armor stand sits and where a text display has to sit for
     * their text to land at the same height.
     *
     * <p>A nameplate is drawn at {@code getNameTagOffsetY() = bbHeight + 0.5}, while a single
     * line text display is shifted down by {@code lines * 10} font pixels, putting its text top
     * {@code 0.25} above its own origin. The difference is therefore {@code bbHeight + 0.25}.
     * Keeping this exact means switching a hologram between display types leaves it looking like
     * it never moved.
     *
     * <p>This constant is version-specific and is NOT a copy/paste slip against the other
     * editions. Vanilla lays a text display out as {@code lines * 10} font pixels tall up to
     * 1.21.1 and {@code lines * 10 - 1} from 26.1 onwards, so the text top sits 0.25 above the
     * origin here and 0.225 there. Check {@code DisplayRenderer.TextDisplayRenderer.renderInner}
     * before changing it.
     */
    public static final double ARMOR_STAND_TEXT_OFFSET = EntityType.ARMOR_STAND.getHeight() + 0.25D;

    protected HologramTextDisplay display;
    protected String text;

    /** The hologram's line-slot position, before the vertical offset is applied. */
    private double slotX;
    private double slotY;
    private double slotZ;
    private float yaw;
    private float pitch;
    private int backgroundArgb = HologramTextDisplay.DEFAULT_BACKGROUND;

    public TextDisplayHologramLine(Level world, double x, double y, double z, float yaw, float pitch) {
        this(world, x, y, z, yaw, pitch, HologramTextDisplay.DEFAULT_BACKGROUND);
    }

    public TextDisplayHologramLine(Level world, double x, double y, double z, float yaw, float pitch,
                                   int backgroundArgb) {
        this.backgroundArgb = backgroundArgb;
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
        created.applyHologramDefaults();
        created.moveTo(this.slotX, this.slotY + ARMOR_STAND_TEXT_OFFSET, this.slotZ, this.yaw, this.pitch);
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
        this.display.setPos(x, y + ARMOR_STAND_TEXT_OFFSET, z);
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

        if (player != null && FabricHolograms.getInstance().arePlaceholdersEnabled()) {
            try {
                return UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(player, this.text));
            } catch (Exception e) {
                LOGGER.debug("Error processing placeholders, using fallback text", e);
                return UtilChatColour.parse(this.text.replace("%", "%%"));
            }
        }

        if (FabricHolograms.getInstance().arePlaceholdersEnabled()) {
            return UtilChatColour.parse(this.text);
        }

        return UtilChatColour.parse(this.text.replace("%", "%%"));
    }

    /**
     * Changes this line's background colour.
     *
     * <p>The colour lives in the display's synched data rather than in the text payload, so
     * viewers only see it after a settings snapshot - {@link #sendSettingsSnapshot(ServerPlayer)}.
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
