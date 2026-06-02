package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.api.exception.HologramException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilBacklight;
import com.strictgaming.elite.holograms.forge.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
import com.strictgaming.elite.holograms.forge.util.UtilWorld;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * Forge implementation of the {@link Hologram} interface
 *
 */
public class ForgeHologram implements Hologram {

    private static final double HOLOGRAM_LINE_GAP = 0.25;

    private final String id;

    private Level world;
    private Vec3 position;
    private int range;

    private final List<HologramLine> lines = Lists.newArrayList();
    private final List<UUID> nearbyPlayers = new CopyOnWriteArrayList<>();
    private long tickCount = 0;

    // Backlight state - places invisible minecraft:light blocks at the hologram
    private boolean backlightEnabled = false;
    private int backlightLevel = UtilBacklight.DEFAULT_LEVEL;
    private transient List<BlockPos> backlightPositions = new ArrayList<>();

    public ForgeHologram(String id, Level world, Vec3 position, int range, boolean save, String... lines) {
        this.id = id;
        this.world = world;
        this.position = position;
        this.range = range;

        this.addLines(save, lines);
        HologramManager.addHologram(this);

        if (save) {
            HologramManager.save();
        }
    }
    
    /**
     * Tick animated hologram lines
     */
    public void tick() {
        tickCount++;
        
        for (HologramLine line : lines) {
            if (line instanceof AnimatedHologramLine) {
                AnimatedHologramLine animatedLine = (AnimatedHologramLine) line;
                if (animatedLine.tick(tickCount)) {
                    // Frame changed, update for all nearby players
                    for (UUID playerUUID : nearbyPlayers) {
                        ServerPlayer player = UtilPlayer.getOnlinePlayer(playerUUID);
                        if (player != null) {
                            animatedLine.updateForPlayer(player);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addLines(String... lines) {
        this.addLines(true, lines);
    }

    private void addLines(boolean save, String... lines) {
        if (!ServerLifecycleHooks.getCurrentServer().isSameThread()) {
            UtilConcurrency.runSync(() -> this.addLines(lines));
            return;
        }

        for (String line : lines) {
            this.addLine(line, save);
        }
    }

    @Override
    public void addLine(String line) {
        this.addLine(line, true);
    }

    private void addLine(String line, boolean save) {
        if (!ServerLifecycleHooks.getCurrentServer().isSameThread()) {
            UtilConcurrency.runSync(() -> this.addLine(line));
            return;
        }

        HologramLine armorStand = new HologramLine(new ArmorStand(this.world, this.position.x,
                this.position.y - (HOLOGRAM_LINE_GAP * this.lines.size()), this.position.z));

        this.lines.add(armorStand);
        armorStand.setText(line);
        this.spawnLine(armorStand);

        if (save) {
            HologramManager.save();
        }
    }

    @Override
    public void move(String world, double x, double y, double z) {
        Level foundWorld = UtilWorld.findWorld(world);

        if (foundWorld == null) {
            return;
        }

        // Remove the backlight from old position before moving
        clearBacklight();

        PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

        for (HologramLine line : this.lines) {
            for (ServerPlayer player : playerList.getPlayers()) {
                line.despawnForPlayer(player);
            }
        }

        for (HologramLine line : this.lines) {
            line.setWorld(foundWorld);
            line.setPosition(x, y, z);
        }

        // Re-apply backlight at new position
        applyBacklight();

        HologramManager.save();
    }

    @Override
    public void setLine(int index, String text) {
        if (index > this.lines.size()) {
            this.addLine(text);
        } else {
            if (!ServerLifecycleHooks.getCurrentServer().isSameThread()) {
                UtilConcurrency.runSync(() -> {
                    this.lines.get(index - 1).setText(text);
                    HologramManager.save();
                });
            } else {
                this.lines.get(index - 1).setText(text);
                HologramManager.save();
            }
        }
    }
    
    @Override
    public void insertLine(int index, String line) {
        if (index > this.lines.size()) {
            this.addLine(line);
            return;
        }

        for (int i = (index - 1); i < this.lines.size(); ++i) {
            HologramLine armorStand = this.lines.get(i);
            armorStand.setPosition(this.position.x, this.position.y - (HOLOGRAM_LINE_GAP * (i + 1)), this.position.z);
        }

        UtilConcurrency.runSync(() -> {
            HologramLine newLine = new HologramLine(new ArmorStand(this.world, this.position.x,
                    this.position.y - (HOLOGRAM_LINE_GAP * (index - 1)), this.position.z));
            newLine.setText(line);
            this.lines.add(index - 1, newLine);

            for (UUID nearbyPlayer : this.nearbyPlayers) {
                ServerPlayer player = UtilPlayer.getOnlinePlayer(nearbyPlayer);

                if (player == null) {
                    continue;
                }

                newLine.spawnForPlayer(player);

                for (int i = (index - 1); i < this.lines.size(); ++i) {
                    HologramLine armorStand = this.lines.get(i);
                    armorStand.sendTeleportPacket(player);
                }
            }

            HologramManager.save();
        });
    }

    @Override
    public void removeLines(int... indexes) throws HologramException {
        for (int index : indexes) {
            this.removeLine(index);
        }
    }

    @Override
    public void removeLine(int index) throws HologramException {
        if (lines.size() == 1) {
            throw new HologramException("§4Cannot remove anymore lines as there's only one left! To delete use §7/hd delete " + this.id);
        }

        if (index > this.lines.size()) {
            throw new HologramException("§4Cannot remove that line as it's out of the bounds of this hologram.");
        }

        HologramLine remove = this.lines.remove(index - 1);

        for (int i = (index - 1); i < this.lines.size(); ++i) {
            HologramLine armorStand = this.lines.get(i);
            armorStand.setPosition(this.position.x, this.position.y - (HOLOGRAM_LINE_GAP * i), this.position.z);
        }

        UtilConcurrency.runSync(() -> {
            for (UUID nearbyPlayer : this.nearbyPlayers) {
                ServerPlayer player = UtilPlayer.getOnlinePlayer(nearbyPlayer);

                if (player == null) {
                    continue;
                }

                remove.despawnForPlayer(player);

                for (int i = (index - 1); i < this.lines.size(); ++i) {
                    HologramLine armorStand = this.lines.get(i);
                    armorStand.sendTeleportPacket(player);
                }
            }

            HologramManager.save();
        });
    }

    @Override
    public void delete() {
        clearBacklight();
        this.despawn();
        HologramManager.removeHologram(this);
        HologramManager.save();
    }

    @Override
    public void despawn() {
        PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

        for (HologramLine line : this.lines) {
            for (ServerPlayer player : playerList.getPlayers()) {
                line.despawnForPlayer(player);
            }
        }
        clearBacklight();
    }
    
    @Override
    public void teleport(String worldName, double x, double y, double z) {
        Level foundWorld = UtilWorld.findWorld(worldName);

        if (foundWorld == null) {
            return;
        }

        // Remove backlight from old location
        clearBacklight();

        this.world = foundWorld;
        this.position = new Vec3(x, y, z);

        // Despawn for all current players
        List<UUID> currentPlayers = new ArrayList<>(this.nearbyPlayers);
        for (UUID playerUUID : currentPlayers) {
            ServerPlayer player = UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                for (HologramLine line : this.lines) {
                    line.despawnForPlayer(player);
                }
            }
        }
        
        // Clear the nearby players list
        this.nearbyPlayers.clear();

        // Update position of all lines
        for (int i = 0; i < this.lines.size(); ++i) {
            HologramLine line = this.lines.get(i);
            line.setWorld(this.world);
            line.setPosition(x, y - (HOLOGRAM_LINE_GAP * i), z);
        }

        // Re-apply backlight at the new location
        applyBacklight();

        // The hologram manager will handle respawning for nearby players automatically in the next tick
        HologramManager.save();
    }
    
    @Override
    public Hologram copy(String newId, String world, double x, double y, double z) {
        String[] lines = new String[this.lines.size()];
        
        for (int i = 0; i < this.lines.size(); ++i) {
            lines[i] = this.lines.get(i).getText();
        }
        
        return new ForgeHologram(newId, UtilWorld.findWorld(world), new Vec3(x, y, z), this.range, true, lines);
    }

    private void spawnLine(HologramLine armorStand) {
        for (UUID nearbyPlayer : this.nearbyPlayers) {
            ServerPlayer player = UtilPlayer.getOnlinePlayer(nearbyPlayer);

            if (player == null) {
                continue;
            }

            armorStand.spawnForPlayer(player);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Level getWorld() {
        return this.world;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public List<HologramLine> getLines() {
        return this.lines;
    }

    public int getRange() {
        return this.range;
    }

    List<UUID> getNearbyPlayers() {
        return this.nearbyPlayers;
    }
    
    public double getDistance(ServerPlayer player) {
        return player.position().distanceTo(this.position);
    }
    
    public boolean inRadius(ServerPlayer player, int radius) {
        return this.getDistance(player) <= radius;
    }
    
    /**
     * Refreshes the hologram's visibility by clearing the nearby players list
     * This will cause the hologram manager to check and respawn the hologram for all nearby players
     */
    public void refreshVisibility() {
        System.out.println("[EliteHolograms] Refreshing visibility for hologram: " + this.id);
        
        // Clear the nearby players list - this will force the hologram manager
        // to recalculate which players should see this hologram
        this.nearbyPlayers.clear();
        
        // Update the world and position for all lines in case they were reloaded
        for (int i = 0; i < this.lines.size(); ++i) {
            HologramLine line = this.lines.get(i);
            line.setWorld(this.world);
            line.setPosition(this.position.x, this.position.y - (HOLOGRAM_LINE_GAP * i), this.position.z);
        }
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
     */
    public void setBacklight(boolean enabled, int level) {
        this.backlightLevel = UtilBacklight.clampLevel(level);
        if (this.backlightEnabled && !enabled) {
            clearBacklight();
            this.backlightEnabled = false;
        } else if (enabled) {
            this.backlightEnabled = true;
            clearBacklight();
            applyBacklight();
        }
        HologramManager.save();
    }

    /**
     * Restores backlight state from config without saving. Used during load.
     */
    public void restoreBacklightState(boolean enabled, int level) {
        this.backlightEnabled = enabled;
        this.backlightLevel = UtilBacklight.clampLevel(level);
    }

    /**
     * Places the backlight blocks in the world if backlight is enabled.
     */
    public void applyBacklight() {
        if (!backlightEnabled || this.world == null || this.position == null) {
            return;
        }

        // The column spans from the ground beneath the lowest line up through
        // the top line, so every row of a tall hologram (e.g. a Top-10
        // scoreboard) sits inside the lit column.
        int lineCount = this.lines.isEmpty() ? 1 : this.lines.size();
        double topY = this.position.y;
        double bottomY = this.position.y - ((lineCount - 1) * HOLOGRAM_LINE_GAP);

        this.backlightPositions = UtilBacklight.placeColumn(this.world, this.position.x, bottomY,
                topY, this.position.z, backlightLevel);
    }

    /**
     * Removes the backlight blocks from the world if any are currently placed.
     */
    public void clearBacklight() {
        if (backlightPositions == null || backlightPositions.isEmpty() || this.world == null) {
            return;
        }
        UtilBacklight.removeLights(this.world, backlightPositions);
        this.backlightPositions = new ArrayList<>();
    }

    /**
     * Add an animated line to the hologram
     * @param frames List of text frames to cycle through
     * @param intervalSeconds Seconds between frame changes
     */
    public void addAnimatedLine(List<String> frames, int intervalSeconds) {
        if (!ServerLifecycleHooks.getCurrentServer().isSameThread()) {
            UtilConcurrency.runSync(() -> this.addAnimatedLine(frames, intervalSeconds));
            return;
        }

        AnimatedHologramLine animatedLine = new AnimatedHologramLine(
            new ArmorStand(this.world, this.position.x,
                this.position.y - (HOLOGRAM_LINE_GAP * this.lines.size()), this.position.z),
            frames,
            intervalSeconds * 20 // Convert seconds to ticks
        );

        this.lines.add(animatedLine);
        this.spawnLine(animatedLine);
        HologramManager.save();
    }
    
    /**
     * Convert an existing line to an animated line
     * @param lineIndex The line index (1-based)
     * @param frames List of text frames to cycle through
     * @param intervalSeconds Seconds between frame changes
     */
    public void setLineAnimated(int lineIndex, List<String> frames, int intervalSeconds) throws HologramException {
        if (lineIndex < 1 || lineIndex > this.lines.size()) {
            throw new HologramException("§4Line index out of bounds!");
        }
        
        int index = lineIndex - 1;
        HologramLine oldLine = this.lines.get(index);
        
        // Despawn old line for all nearby players
        for (UUID playerUUID : this.nearbyPlayers) {
            ServerPlayer player = UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                oldLine.despawnForPlayer(player);
            }
        }
        
        // Create new animated line at same position
        AnimatedHologramLine newLine = new AnimatedHologramLine(
            new ArmorStand(this.world, this.position.x,
                this.position.y - (HOLOGRAM_LINE_GAP * index), this.position.z),
            frames,
            intervalSeconds * 20 // Convert seconds to ticks
        );
        
        this.lines.set(index, newLine);
        
        // Spawn new line for all nearby players
        for (UUID playerUUID : this.nearbyPlayers) {
            ServerPlayer player = UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                newLine.spawnForPlayer(player);
            }
        }
        
        HologramManager.save();
    }
} 
