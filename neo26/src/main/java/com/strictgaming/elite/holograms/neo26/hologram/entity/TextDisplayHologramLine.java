package com.strictgaming.elite.holograms.neo26.hologram.entity;

import com.strictgaming.elite.holograms.neo26.Neo26Holograms;
import com.strictgaming.elite.holograms.neo26.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo26.util.UtilPlaceholder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * A hologram line rendered as a {@code text_display} with a fixed billboard, so the text stays
 * anchored at the hologram's yaw and pitch instead of turning towards the viewer. This is the
 * {@code fixed} display type.
 */
public class TextDisplayHologramLine implements HologramLineRenderer {

    /**
     * Vertical gap between where an armor stand sits and where a text display has to sit for
     * their text to land at the same height.
     *
     * <p>A nameplate's text is drawn with its top at {@code entityY + bbHeight + 0.5}. A single
     * line text display is laid out {@code height = lines * 10 - 1} font pixels tall and shifted
     * down by that amount, putting its text top {@code 9 * 0.025 = 0.225} above its own origin.
     * The difference is therefore {@code bbHeight + 0.275}. Keeping this exact means switching a
     * hologram between display types leaves it looking like it never moved.
     *
     * <p>This constant is version-specific and is NOT a copy/paste slip against the other
     * editions. Vanilla lays a text display out as {@code lines * 10} font pixels tall up to
     * 1.21.1 and {@code lines * 10 - 1} from 26.1 onwards, so the text top sits 0.225 above the
     * origin here and 0.25 there. Check {@code DisplayRenderer.TextDisplayRenderer.renderInner}
     * before changing it.
     */
    public static final double ARMOR_STAND_TEXT_OFFSET = EntityType.ARMOR_STAND.getHeight() + 0.275D;

    protected final HologramTextDisplay display;
    protected String rawText;

    public TextDisplayHologramLine(ServerLevel level, double x, double y, double z,
                                   float yaw, float pitch, String rawText) {
        this(level, x, y, z, yaw, pitch, rawText, HologramTextDisplay.DEFAULT_BACKGROUND);
    }

    public TextDisplayHologramLine(ServerLevel level, double x, double y, double z,
                                   float yaw, float pitch, String rawText, int backgroundArgb) {
        this.rawText = rawText;
        this.display = new HologramTextDisplay(level);
        this.display.setId(HologramEntityIds.next());
        this.display.setBackgroundArgb(backgroundArgb);
        this.display.applyHologramDefaults();
        this.display.snapTo(x, y + ARMOR_STAND_TEXT_OFFSET, z, yaw, pitch);
        // Seed the entity copy of the text so the snapshot sent on spawn is never blank.
        this.display.setHologramText(renderText(null));
    }

    @Override
    public void spawnToPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        player.connection.send(new ClientboundAddEntityPacket(
                this.display.getId(),
                this.display.getUUID(),
                this.display.getX(),
                this.display.getY(),
                this.display.getZ(),
                this.display.getXRot(),
                this.display.getYRot(),
                this.display.getType(),
                0,
                Vec3.ZERO,
                this.display.getYHeadRot()
        ));

        updateForPlayer(player, true);
    }

    @Override
    public void updateForPlayer(ServerPlayer player, boolean isInitialSpawn) {
        if (player == null || player.connection == null) {
            return;
        }

        if (isInitialSpawn) {
            // The display's own settings (billboard, alignment, wrapping) only reach the client
            // through a full snapshot, so send that on spawn.
            List<SynchedEntityData.DataValue<?>> snapshot = this.display.getEntityData().getNonDefaultValues();

            if (snapshot != null && !snapshot.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(this.display.getId(), snapshot));
            }
        }

        List<SynchedEntityData.DataValue<?>> textData = this.display.buildTextData(renderText(player));

        if (!textData.isEmpty()) {
            player.connection.send(new ClientboundSetEntityDataPacket(this.display.getId(), textData));
        }
    }

    @Override
    public void despawnFromPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        player.connection.send(new ClientboundRemoveEntitiesPacket(this.display.getId()));
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.display.setPos(x, y + ARMOR_STAND_TEXT_OFFSET, z);
        this.display.setOldPosAndRot();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        this.display.setYRot(yaw);
        this.display.setXRot(pitch);
        // Without this the client interpolates from the previous rotation on the next update.
        this.display.setOldPosAndRot();
    }

    @Override
    public void sendTeleportPacket(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        player.connection.send(new ClientboundTeleportEntityPacket(
                this.display.getId(),
                new PositionMoveRotation(
                        this.display.position(),
                        Vec3.ZERO,
                        this.display.getYRot(),
                        this.display.getXRot()
                ),
                Set.of(),
                this.display.onGround()
        ));
    }

    @Override
    public String getText() {
        return this.rawText;
    }

    /**
     * Replaces this line's text and pushes it to the given viewers.
     *
     * @param newRawText          the new unresolved text
     * @param playersToRefreshFor viewers currently seeing this line
     */
    public void updateRawTextAndRefresh(String newRawText, List<ServerPlayer> playersToRefreshFor) {
        this.rawText = newRawText;
        this.display.setHologramText(renderText(null));
        playersToRefreshFor.forEach(player -> updateForPlayer(player, false));
    }

    /**
     * Resolves this line's text for a viewer.
     *
     * @param player the viewer, or null to resolve without per-player placeholders
     * @return the component to display
     */
    protected Component renderText(ServerPlayer player) {
        if ("{empty}".equals(this.rawText)) {
            return Component.empty();
        }

        String processed = this.rawText;

        if (player == null) {
            processed = UtilPlaceholder.replacePlaceholders(this.rawText, null);
        } else if (Neo26Holograms.getInstance().arePlaceholdersEnabled()) {
            processed = UtilPlaceholder.replacePlaceholders(this.rawText, player);
        }

        return UtilChatColour.parse(processed);
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
        updateForPlayer(player, true);
    }

    /**
     * @return the underlying packet-only display entity
     */
    public HologramTextDisplay getDisplay() {
        return this.display;
    }
}
