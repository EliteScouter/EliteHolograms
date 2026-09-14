package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.neo21.hologram.entity.AnimatedTextDisplayHologramLine;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramEntityIds;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramInteraction;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramTextDisplay;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramLineRenderer;
import com.strictgaming.elite.holograms.neo21.hologram.entity.TextDisplayHologramLine;
import com.strictgaming.elite.holograms.neo21.util.UtilBacklight;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPlaceholder;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * NeoForge implementation of hologram with per-player support
 */
public class NeoForgeHologram implements Hologram {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double LINE_SPACING = 0.25;
    
    private final String id;
    private String world;
    private double x;
    private double y;
    private double z;

    // How the lines are rendered, and the orientation used when that is FIXED
    private HologramDisplayType displayType = HologramDisplayType.FACING;
    private float yaw = 0.0F;
    private float pitch = 0.0F;

    /** Vanilla's text display background is black, which is what hologram lines have always had. */
    public static final int DEFAULT_BACKGROUND_COLOUR = 0x000000;

    /** Vanilla's text display background is 25% opaque (alpha 64 of 255). */
    public static final int DEFAULT_BACKGROUND_OPACITY = 25;

    // Background of the text panel behind a fixed hologram's lines. Stored as an RGB
    // colour plus an opacity percentage rather than one packed value so both halves can be
    // set independently, and so the config stays readable.
    private int backgroundColour = DEFAULT_BACKGROUND_COLOUR;
    private int backgroundOpacity = DEFAULT_BACKGROUND_OPACITY;

    // Text display presentation. Only meaningful on a fixed hologram - an armor stand
    // nameplate is drawn by the client at a fixed size, always facing the camera, and lit
    // by the world, none of which the server can override.
    private float scale = HologramTextDisplay.DEFAULT_SCALE;
    private int brightness = HologramTextDisplay.BRIGHTNESS_AUTO;
    private Display.BillboardConstraints billboard = Display.BillboardConstraints.FIXED;

    // Click-to-run. The command is null when the hologram is not clickable.
    private String clickCommand;
    private boolean clickAsConsole = false;
    private transient HologramInteraction interaction;
    
    // Content of lines: String or AnimatedLineData
    private List<Object> linesContent = new ArrayList<>();
    
    // Live entities
    private final List<HologramLineRenderer> hologramLines = new ArrayList<>();
    
    private final List<UUID> nearbyPlayers = Collections.synchronizedList(new ArrayList<>());
    private boolean spawned = false;

    // Backlight state - places invisible minecraft:light blocks at the hologram
    private boolean backlightEnabled = false;
    private int backlightLevel = UtilBacklight.DEFAULT_LEVEL;
    private List<BlockPos> backlightPositions = new ArrayList<>();
    
    // Placeholder refresh: update static lines every 20 ticks (1 second)
    private static final int PLACEHOLDER_REFRESH_INTERVAL = 20;
    private int placeholderTickCounter = 0;
    
    public static class AnimatedLineData {
        public final List<String> frames;
        public final int interval;
        public AnimatedLineData(List<String> frames, int interval) {
            this.frames = frames;
            this.interval = interval;
        }
    }
    
    public NeoForgeHologram(String id, String world, double x, double y, double z, List<String> lines) {
        this(id, world, x, y, z, lines, HologramDisplayType.FACING, 0.0F, 0.0F);
    }

    public NeoForgeHologram(String id, String world, double x, double y, double z, List<String> lines,
                            HologramDisplayType displayType, float yaw, float pitch) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);
        if (lines != null) {
            this.linesContent.addAll(lines);
        }
        synchronized (HologramManager.class) {
            HologramManager.addHologram(this);
        }
        rebuildHologramLines();
    }
    
    // Constructor for loading from config which might have complex data manually handled later, 
    // but for now standard constructor handles Strings. 
    // Complex lines must be added via addAnimatedLine or setLineAnimated after creation if not supported in constructor.
    
    private void rebuildHologramLines() {
        for (UUID playerUUID : new ArrayList<>(nearbyPlayers)) {
            ServerPlayer player = getPlayerByUUID(playerUUID);
            if (player != null) {
                hologramLines.forEach(line -> line.despawnFromPlayer(player));
            }
        }

        hologramLines.clear();
        ServerLevel level = getServerLevel();
        if (level == null) {
            return;
        }

        double currentY = this.y;
        for (Object content : this.linesContent) {
            HologramLineRenderer line = createLine(level, content, currentY);
            
            hologramLines.add(line);
            currentY -= lineGap();
        }

        if (this.spawned) {
            for (UUID playerUUID : new ArrayList<>(nearbyPlayers)) {
                ServerPlayer player = getPlayerByUUID(playerUUID);
                if (player != null) {
                    hologramLines.forEach(line -> line.spawnToPlayer(player));
                }
            }
        }

        // The clickable hitbox is sized and placed from the text block, so it has to be
        // rebuilt whenever the lines change.
        applyInteraction();
    }
    
    /**
     * Builds a single line entity for this hologram's display type.
     *
     * @param level    the level the line lives in
     * @param content  either a plain String or {@link AnimatedLineData}
     * @param lineY    the Y coordinate of this line's slot
     * @return the line renderer
     */
    private HologramLineRenderer createLine(ServerLevel level, Object content, double lineY) {
        boolean fixed = this.displayType == HologramDisplayType.FIXED;

        if (content instanceof AnimatedLineData data) {
            // Stored in seconds, ticked in ticks.
            int intervalTicks = data.interval * 20;

            if (fixed) {
                return new AnimatedTextDisplayHologramLine(level, this.x, lineY, this.z,
                        this.yaw, this.pitch, data.frames, intervalTicks, getBackgroundArgb(),
                        this.scale, this.brightness, this.billboard);
            }

            ArmorStand armorStand = new ArmorStand(level, this.x, lineY, this.z);
            armorStand.setId(HologramEntityIds.next());
            configureArmorStand(armorStand);
            String initialText = data.frames.isEmpty() ? "" : data.frames.get(0);
            armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(initialText, null)));

            return new AnimatedHologramLine(armorStand, data.frames, intervalTicks);
        }

        String text = (content != null) ? content.toString() : "";

        if (fixed) {
            return new TextDisplayHologramLine(level, this.x, lineY, this.z, this.yaw, this.pitch, text,
                    getBackgroundArgb(), this.scale, this.brightness, this.billboard);
        }

        return new HologramLine(level, this.x, lineY, this.z, text);
    }

    private void configureArmorStand(ArmorStand armorStand) {
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setSilent(true);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentData().putBoolean("Marker", true);
        armorStand.addTag("spectral_vision_unaffected");
    }

    private static float normaliseYaw(float yaw) {
        float wrapped = yaw % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        for (Object content : linesContent) {
            if (content instanceof AnimatedLineData) {
                // Return something that indicates it's animated? Or just first frame?
                // Interface expects String.
                // For serialization, we might need to access linesContent directly.
                // For display/listing, first frame is fine.
                lines.add(((AnimatedLineData) content).frames.isEmpty() ? "" : ((AnimatedLineData) content).frames.get(0));
            } else {
                lines.add(content.toString());
            }
        }
        return lines;
    }
    
    public List<Object> getLinesContent() {
        return new ArrayList<>(linesContent);
    }

    /**
     * Returns a direct reference to the internal lines content list.
     * For use by subclasses that need to modify content without copying.
     */
    protected List<Object> getLinesContentInternal() {
        return linesContent;
    }
    
    public void setLinesContent(List<Object> content) {
        updateHologramContent(() -> {
            this.linesContent.clear();
            if (content != null) {
                this.linesContent.addAll(content);
            }
        });
    }
    
    @Override
    public void setLines(List<String> lines) {
        updateHologramContent(() -> {
            this.linesContent.clear();
            if (lines != null) {
                this.linesContent.addAll(lines);
            }
        });
    }
    
    @Override
    public void setLines(String... lines) {
        setLines(Arrays.asList(lines));
    }
    
    @Override
    public String getLine(int index) {
        if (index >= 0 && index < linesContent.size()) {
            Object content = linesContent.get(index);
            if (content instanceof AnimatedLineData) {
                return ((AnimatedLineData) content).frames.isEmpty() ? "" : ((AnimatedLineData) content).frames.get(0);
            }
            return content.toString();
        }
        return null;
    }
    
    @Override
    public void setLine(int index, String text) {
        if (index >= 0 && index < linesContent.size()) {
            updateHologramContent(() -> linesContent.set(index, text));
        }
    }
    
    @Override
    public void addLine(String text) {
        updateHologramContent(() -> linesContent.add(text));
    }
    
    public void addAnimatedLine(List<String> frames, int interval) {
        updateHologramContent(() -> linesContent.add(new AnimatedLineData(frames, interval)));
    }
    
    public void setLineAnimated(int index, List<String> frames, int interval) {
        if (index >= 0 && index < linesContent.size()) {
            updateHologramContent(() -> linesContent.set(index, new AnimatedLineData(frames, interval)));
        }
    }
    
    @Override
    public void insertLine(int index, String text) {
        if (index >= 0 && index <= linesContent.size()) {
            updateHologramContent(() -> linesContent.add(index, text));
        }
    }
    
    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < linesContent.size()) {
            updateHologramContent(() -> linesContent.remove(index));
        }
    }
    
    @Override
    public String getWorld() {
        return world;
    }
    
    @Override
    public double getX() {
        return x;
    }
    
    @Override
    public double getY() {
        return y;
    }
    
    @Override
    public double getZ() {
        return z;
    }
    
    @Override
    public void setPosition(String world, double x, double y, double z) {
        boolean worldChanged = !this.world.equals(world);
        List<UUID> currentPlayers = new ArrayList<>(this.nearbyPlayers);

        if (worldChanged && spawned) {
             currentPlayers.forEach(uuid -> {
                ServerPlayer p = getPlayerByUUID(uuid);
                if (p != null) despawnForPlayer(p);
            });
            nearbyPlayers.clear();
        }

        // Remove backlight from old position before moving
        clearBacklight();

        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        double currentY = this.y;
        for (HologramLineRenderer line : hologramLines) {
            line.setPosition(this.x, currentY, this.z);
            currentY -= lineGap();
        }
        
        if (spawned) {
            if (worldChanged) {
                ServerLevel newLevel = getServerLevel();
                if (newLevel != null) {
                    newLevel.getServer().getPlayerList().getPlayers().forEach(player -> {
                        if (isPlayerNearby(player)) {
                            spawnForPlayer(player);
                        }
                    });
                }
            } else {
                currentPlayers.forEach(uuid -> {
                    ServerPlayer p = getPlayerByUUID(uuid);
                    if (p != null) {
                         hologramLines.forEach(hl -> hl.sendTeleportPacket(p));
                    }
                });
            }
        }
        // Re-apply backlight at the new location
        applyBacklight();
        saveToConfig();
    }
    
    /**
     * @return how this hologram's lines are rendered
     */
    public HologramDisplayType getDisplayType() {
        return this.displayType;
    }

    /**
     * @return the yaw applied to the lines when the display type is fixed
     */
    public float getYaw() {
        return this.yaw;
    }

    /**
     * @return the pitch applied to the lines when the display type is fixed
     */
    public float getPitch() {
        return this.pitch;
    }

    /**
     * @return the RGB background colour used behind a fixed hologram's text
     */
    public int getBackgroundColour() {
        return this.backgroundColour;
    }

    /**
     * @return how opaque the background is, 0 (invisible) to 100 (solid)
     */
    public int getBackgroundOpacity() {
        return this.backgroundOpacity;
    }

    /**
     * @return the colour and opacity packed the way a text display wants them
     */
    public int getBackgroundArgb() {
        int alpha = Math.round(this.backgroundOpacity * 255.0F / 100.0F);
        return (alpha << 24) | (this.backgroundColour & 0xFFFFFF);
    }

    /**
     * Sets the background behind a fixed hologram's text and pushes it to anyone watching.
     *
     * <p>Player-facing holograms are unaffected on screen: their text is an armor stand
     * nameplate, whose background the client draws using the viewer's own chat background
     * opacity. The value is still stored so it applies if the hologram is later converted.
     *
     * @param colour  the RGB colour
     * @param opacity how opaque to draw it, 0 (invisible) to 100 (solid)
     */
    public void setBackground(int colour, int opacity) {
        this.backgroundColour = colour & 0xFFFFFF;
        this.backgroundOpacity = Math.max(0, Math.min(100, opacity));

        int argb = getBackgroundArgb();

        for (HologramLineRenderer line : hologramLines) {
            line.setBackgroundArgb(argb);
        }

        if (spawned) {
            for (UUID uuid : new ArrayList<>(nearbyPlayers)) {
                ServerPlayer player = getPlayerByUUID(uuid);
                if (player != null) {
                    hologramLines.forEach(line -> line.sendSettingsSnapshot(player));
                }
            }
        }

        saveToConfig();
    }

    /**
     * Restores the background from storage without saving. Called during load, before the line
     * entities are built, so they pick the colour up on construction.
     *
     * @param colour  the persisted RGB colour
     * @param opacity the persisted opacity percentage
     */
    public void restoreBackgroundState(int colour, int opacity) {
        this.backgroundColour = colour & 0xFFFFFF;
        this.backgroundOpacity = Math.max(0, Math.min(100, opacity));
    }

    /**
     * @return the vertical distance between lines, widened in step with the scale so a
     *         scaled-up hologram does not overlap its own lines
     */
    private double lineGap() {
        return this.displayType == HologramDisplayType.FIXED
                ? LINE_SPACING * this.scale
                : LINE_SPACING;
    }

    /**
     * @return the uniform scale applied to a fixed hologram text
     */
    public float getScale() {
        return this.scale;
    }

    /**
     * @return the light override, or {@link HologramTextDisplay#BRIGHTNESS_AUTO}
     */
    public int getBrightness() {
        return this.brightness;
    }

    /**
     * @return how a fixed hologram turns towards the viewer
     */
    public Display.BillboardConstraints getBillboard() {
        return this.billboard;
    }

    /**
     * Resizes a fixed hologram. Line spacing widens with the scale, so the lines are
     * rebuilt rather than just re-sent.
     *
     * @param scale the uniform scale; 1.0 is the natural size
     */
    public void setScale(float scale) {
        updateHologramContent(() -> this.scale = scale);
    }

    /**
     * Overrides the light a fixed hologram text is drawn at.
     *
     * @param brightness 0-15, or {@link HologramTextDisplay#BRIGHTNESS_AUTO} for world light
     */
    public void setBrightness(int brightness) {
        this.brightness = brightness;
        pushLineSetting(line -> line.setBrightness(brightness));
    }

    /**
     * Sets how a fixed hologram turns towards the viewer.
     *
     * @param billboard the billboard constraint to apply
     */
    public void setBillboard(Display.BillboardConstraints billboard) {
        this.billboard = billboard;
        pushLineSetting(line -> line.setBillboard(billboard));
    }

    /**
     * Applies a display setting to every line and pushes it to anyone watching.
     */
    private void pushLineSetting(Consumer<HologramLineRenderer> change) {
        hologramLines.forEach(change);

        if (spawned) {
            for (UUID uuid : new ArrayList<>(nearbyPlayers)) {
                ServerPlayer player = getPlayerByUUID(uuid);
                if (player != null) {
                    hologramLines.forEach(line -> line.sendSettingsSnapshot(player));
                }
            }
        }

        saveToConfig();
    }

    /**
     * Restores presentation settings from storage without saving. Called during load,
     * before the line entities are built, so they pick these up on construction.
     *
     * @param scale      the persisted scale
     * @param brightness the persisted light override
     * @param billboard  the persisted billboard constraint
     */
    public void restorePresentationState(float scale, int brightness,
                                         Display.BillboardConstraints billboard) {
        this.scale = scale;
        this.brightness = brightness;
        this.billboard = billboard == null ? Display.BillboardConstraints.FIXED : billboard;
    }

    /**
     * @return whether a clickable hitbox currently exists in the world for this hologram
     */
    public boolean hasClickHitbox() {
        return this.interaction != null && this.interaction.isAlive();
    }

    /**
     * @return the command run when a player clicks this hologram, or null when it is not
     *         clickable
     */
    public String getClickCommand() {
        return this.clickCommand;
    }

    /**
     * @return whether the click command runs elevated rather than as the clicking player
     */
    public boolean isClickAsConsole() {
        return this.clickAsConsole;
    }

    /**
     * Makes this hologram run a command when clicked, or stops it doing so.
     *
     * @param command   the command to run without a leading slash, or null to clear it
     * @param asConsole true to run it elevated rather than as the clicking player
     */
    public void setClickCommand(String command, boolean asConsole) {
        this.clickCommand = (command == null || command.trim().isEmpty()) ? null : command.trim();
        this.clickAsConsole = asConsole;
        applyInteraction();
        saveToConfig();
    }

    /**
     * Restores click settings from storage without saving or touching the world.
     *
     * @param command   the persisted command
     * @param asConsole the persisted execution mode
     */
    public void restoreClickState(String command, boolean asConsole) {
        this.clickCommand = (command == null || command.trim().isEmpty()) ? null : command.trim();
        this.clickAsConsole = asConsole;
    }

    /**
     * Creates the clickable hitbox if it is missing.
     *
     * <p>The hitbox is a real entity, so it can only be added to a loaded chunk. A hologram
     * away from spawn is in an unloaded chunk when the server starts, so the attempt made
     * during load silently does nothing and the hologram comes back unclickable. Calling this
     * as a player comes into range retries at the one moment the chunk is guaranteed loaded.
     */
    public void ensureClickHitbox() {
        if (this.clickCommand != null && !hasClickHitbox()) {
            applyInteraction();
        }
    }

    /**
     * Rebuilds the clickable hitbox to match the current text block.
     *
     * <p>Always clears first: the hitbox is sized from the line count and scale, so an edit
     * that changes either has to replace it rather than move it.
     */
    private void applyInteraction() {
        clearInteraction();

        ServerLevel level = getServerLevel();

        if (this.clickCommand == null || hologramLines.isEmpty() || level == null) {
            return;
        }

        double gap = lineGap();
        // The text hangs below the anchor, one line slot per line, and the bottom line
        // extends a little further down than its own slot.
        double height = ((hologramLines.size() - 1) * gap) + (0.25D * this.scale);

        // The text is not drawn at the hologram position: every line renders a nameplate
        // height above its own slot. The hitbox has to follow the visible text, or it
        // ends up a couple of blocks low and only the bottom of a tall hologram is
        // clickable.
        double textTop = this.y + TextDisplayHologramLine.textTopOffset();
        double bottomY = textTop - height;

        // Width cannot be measured server-side - the font lives on the client - so this is
        // a readable-looking box derived from the longest line, clamped so a very long line
        // does not produce a hitbox players bump into from across the room.
        double width = Math.max(1.0D, Math.min(8.0D, longestLineLength() * 0.11D * this.scale));

        this.interaction = new HologramInteraction(level);
        this.interaction.applySize((float) width, (float) Math.max(gap, height));
        this.interaction.moveTo(this.x, bottomY, this.z, 0.0F, 0.0F);

        level.addFreshEntity(this.interaction);
        HologramManager.registerInteraction(this.interaction.getUUID(), this.id);
    }

    /**
     * Removes the clickable hitbox from the world.
     */
    private void clearInteraction() {
        if (this.interaction == null) {
            return;
        }

        HologramManager.unregisterInteraction(this.interaction.getUUID());
        this.interaction.discard();
        this.interaction = null;
    }

    /**
     * @return the character count of the longest line, used only to size the click hitbox
     */
    private int longestLineLength() {
        int longest = 0;

        for (HologramLineRenderer line : hologramLines) {
            String text = line.getText();

            if (text != null) {
                longest = Math.max(longest, text.length());
            }
        }

        return longest;
    }

    /**
     * Switches how this hologram renders. The line entities are a different entity type per
     * display type, so this respawns them for anyone currently watching.
     *
     * @param displayType the display type to use
     */
    public void setDisplayType(HologramDisplayType displayType) {
        if (displayType == null || displayType == this.displayType) {
            return;
        }

        updateHologramContent(() -> this.displayType = displayType);
    }

    /**
     * Sets the orientation used by fixed holograms. Stored regardless of the current display
     * type so switching to fixed later keeps the rotation.
     *
     * @param yaw   rotation around the Y axis in degrees
     * @param pitch rotation around the X axis in degrees
     */
    public void setRotation(float yaw, float pitch) {
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);

        for (HologramLineRenderer line : hologramLines) {
            line.setRotation(this.yaw, this.pitch);
        }

        if (spawned) {
            // Teleport packets carry rotation, so this is enough to push the change.
            for (UUID uuid : new ArrayList<>(nearbyPlayers)) {
                ServerPlayer player = getPlayerByUUID(uuid);
                if (player != null) {
                    hologramLines.forEach(line -> line.sendTeleportPacket(player));
                }
            }
        }

        saveToConfig();
    }

    /**
     * Restores display settings from config without triggering a save.
     *
     * @param displayType the persisted display type
     * @param yaw         the persisted yaw
     * @param pitch       the persisted pitch
     */
    public void restoreDisplayState(HologramDisplayType displayType, float yaw, float pitch) {
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        this.yaw = normaliseYaw(yaw);
        this.pitch = clampPitch(pitch);
    }

    @Override
    public void spawn() {
        if (this.spawned) return;
        this.spawned = true;
        ServerLevel level = getServerLevel();
        if (level != null) {
            level.getServer().getPlayerList().getPlayers().forEach(player -> {
                if (isPlayerNearby(player)) {
                    spawnForPlayer(player);
                }
            });
        }
        applyBacklight();
        applyInteraction();
        LOGGER.debug("Hologram {} marked as spawned/active.", id);
    }

    @Override
    public void despawn() {
        if (!this.spawned) return;
        this.spawned = false;
        new ArrayList<>(nearbyPlayers).forEach(uuid -> {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                despawnForPlayer(player);
            }
        });
        clearBacklight();
        LOGGER.debug("Hologram {} marked as despawned/inactive.", id);
    }
    
    public void spawnForPlayer(ServerPlayer player) {
        if (player == null || !this.spawned || nearbyPlayers.contains(player.getUUID())) {
            return;
        }
        if (!isPlayerInCorrectWorld(player)) return;

        LOGGER.debug("Spawning hologram {} for player {}", id, player.getName().getString());

        // A player being in range means this chunk is loaded, which is the only point at
        // which a click hitbox can actually be added to the world.
        ensureClickHitbox();

        hologramLines.forEach(line -> line.spawnToPlayer(player));
        nearbyPlayers.add(player.getUUID());
    }

    public void despawnForPlayer(ServerPlayer player) {
        if (player == null || !nearbyPlayers.remove(player.getUUID())) {
            return;
        }
        LOGGER.debug("Despawning hologram {} for player {}", id, player.getName().getString());
        hologramLines.forEach(line -> line.despawnFromPlayer(player));
    }

    public void updateTextForPlayer(ServerPlayer player) {
        if (player == null || !this.spawned || !nearbyPlayers.contains(player.getUUID())) {
            return;
        }
        if (!isPlayerInCorrectWorld(player)) {
             despawnForPlayer(player);
             return;
        }
        // Normal lines don't need constant updates unless we really want dynamic placeholders every tick
        // But Animated lines do.
        // Actually, AnimatedHologramLine.updateForPlayer is called by tick()
        // This method is for periodic full refresh?
        // neo21 HologramManager calls this in handlePlayerMove if nearby&visible
        
        // For compatibility with Animated lines, we should let tick() handle them.
        // For static lines with placeholders, we can update here.
        
        hologramLines.forEach(line -> {
            if (!line.isAnimated()) { // Don't spam animated lines here, tick handles them
                line.updateForPlayer(player, false);
            }
        });
    }
    
    public void tick() {
        // Tick animated lines
        for (HologramLineRenderer line : hologramLines) {
            if (line.isAnimated() && line.tickAnimation()) {
                // Copied because nearbyPlayers is mutated as players walk in and out of range,
                // and iterating a synchronizedList directly is not safe against that.
                for (UUID uuid : new ArrayList<>(nearbyPlayers)) {
                    ServerPlayer p = getPlayerByUUID(uuid);
                    if (p != null) line.updateForPlayer(p, false);
                }
            }
        }
        
        // Periodically refresh static lines so placeholders like %players%, %tps% stay current
        placeholderTickCounter++;
        if (placeholderTickCounter >= PLACEHOLDER_REFRESH_INTERVAL) {
            placeholderTickCounter = 0;
            for (HologramLineRenderer line : hologramLines) {
                if (!line.isAnimated()) {
                    for (UUID uuid : new ArrayList<>(nearbyPlayers)) {
                        ServerPlayer p = getPlayerByUUID(uuid);
                        if (p != null) {
                            line.updateForPlayer(p, false);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void update() {
        if (!spawned) return;
        
        List<ServerPlayer> currentViewers = nearbyPlayers.stream()
            .map(this::getPlayerByUUID)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
            
        hologramLines.forEach(line -> currentViewers.forEach(line::despawnFromPlayer));
        
        rebuildHologramLines();
        
        LOGGER.debug("Hologram {} updated globally for {} viewers.", id, currentViewers.size());
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }
    
    public boolean isVisibleTo(ServerPlayer player) {
        return player != null && nearbyPlayers.contains(player.getUUID());
    }

    @Override
    public void delete() {
        LOGGER.info("Deleting hologram: {}", id);
        clearBacklight();
        clearInteraction();
        despawn();
        HologramManager.removeHologram(this.id);
    }
    
    private boolean isPlayerInCorrectWorld(ServerPlayer player) {
        return player != null && player.level().dimension().location().toString().equals(this.world);
    }

    public boolean isPlayerNearby(ServerPlayer player) {
        if (!isPlayerInCorrectWorld(player)) {
            return false;
        }
        double distSq = player.distanceToSqr(x, y, z);
        return distSq <= (64 * 64);
    }

    private ServerPlayer getPlayerByUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid) : null;
    }
    
    public List<UUID> getNearbyPlayersView() {
        return new ArrayList<>(nearbyPlayers);
    }

    private ServerLevel getServerLevel() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        if (world == null || world.isEmpty()) {
            return ServerLifecycleHooks.getCurrentServer().overworld();
        }
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(world)) {
                return level;
            }
        }
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }

    private void saveToConfig() {
        try {
            HologramManager.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save hologram {} to config", id, e);
        }
    }

    private void updateHologramContent(Runnable contentUpdater) {
        contentUpdater.run();
        rebuildHologramLines();
        saveToConfig();
    }

    /**
     * Updates hologram content and rebuilds lines without triggering a config save.
     * Used by ScoreboardHologram for ephemeral display updates that don't need persistence.
     */
    protected void updateHologramContentNoSave(Runnable contentUpdater) {
        contentUpdater.run();
        rebuildHologramLines();
    }

    // === Backlight API ===

    /**
     * @return whether this hologram has a backlight (light block) applied at its position.
     */
    public boolean isBacklightEnabled() {
        return backlightEnabled;
    }

    /**
     * @return the configured backlight emission level (0-15).
     */
    public int getBacklightLevel() {
        return backlightLevel;
    }

    /**
     * Enable or disable the backlight for this hologram. When enabled, an
     * invisible {@code minecraft:light} block is placed at the hologram's
     * anchor block, lighting up the area without showing any block.
     *
     * @param enabled whether the backlight should be on
     * @param level   light emission level 0-15 (only used when enabling)
     */
    public void setBacklight(boolean enabled, int level) {
        this.backlightLevel = UtilBacklight.clampLevel(level);
        if (this.backlightEnabled && !enabled) {
            clearBacklight();
            this.backlightEnabled = false;
        } else if (enabled) {
            // Re-place even if already enabled so a level change takes effect immediately
            this.backlightEnabled = true;
            clearBacklight();
            applyBacklight();
        }
        saveToConfig();
    }

    /**
     * Restores backlight state from config without saving. Used during load.
     */
    public void restoreBacklightState(boolean enabled, int level) {
        this.backlightEnabled = enabled;
        this.backlightLevel = UtilBacklight.clampLevel(level);
    }

    /**
     * Places the backlight in the world if enabled and the hologram is in a
     * loaded world. The backlight is a vertical column of invisible light
     * blocks that starts on the ground directly below the hologram and rises
     * {@code backlightLevel} blocks straight up, so the light reads as a beam
     * behind the hologram rather than a patch spread across the floor.
     */
    private void applyBacklight() {
        if (!backlightEnabled) {
            return;
        }
        ServerLevel level = getServerLevel();
        if (level == null) {
            return;
        }

        // The column spans from the ground beneath the lowest line up through
        // the top line, so every row of a tall hologram (e.g. a Top-10
        // scoreboard) sits inside the lit column.
        int lineCount = linesContent.isEmpty() ? 1 : linesContent.size();
        double topY = this.y;
        double bottomY = this.y - ((lineCount - 1) * LINE_SPACING);

        this.backlightPositions = UtilBacklight.placeColumn(level, this.x, bottomY, topY, this.z, backlightLevel);
    }

    /**
     * Removes the backlight blocks from the world if any are currently placed.
     */
    private void clearBacklight() {
        if (backlightPositions == null || backlightPositions.isEmpty()) {
            return;
        }
        ServerLevel level = getServerLevel();
        if (level != null) {
            UtilBacklight.removeLights(level, backlightPositions);
        }
        this.backlightPositions = new ArrayList<>();
    }
}