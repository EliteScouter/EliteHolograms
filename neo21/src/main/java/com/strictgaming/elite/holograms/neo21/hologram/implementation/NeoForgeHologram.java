package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPlaceholder;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
    private static int nextEntityId = -2000000000; 
    
    private final String id;
    private String world;
    private double x;
    private double y;
    private double z;
    
    // Content of lines: String or AnimatedLineData
    private List<Object> linesContent = new ArrayList<>();
    
    // Live entities
    private final List<HologramLine> hologramLines = new ArrayList<>();
    
    private final List<UUID> nearbyPlayers = Collections.synchronizedList(new ArrayList<>());
    private boolean spawned = false;
    
    public static class AnimatedLineData {
        public final List<String> frames;
        public final int interval;
        public AnimatedLineData(List<String> frames, int interval) {
            this.frames = frames;
            this.interval = interval;
        }
    }
    
    public NeoForgeHologram(String id, String world, double x, double y, double z, List<String> lines) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
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
            HologramLine line;
            if (content instanceof AnimatedLineData) {
                AnimatedLineData data = (AnimatedLineData) content;
                // Create ArmorStand manually to pass to AnimatedHologramLine
                // We need a helper since HologramLine usually creates it
                // Actually, AnimatedHologramLine extends HologramLine, so we can just instantiate it
                // But AnimatedHologramLine constructor takes ArmorStand.
                // We should change AnimatedHologramLine to take Level, x,y,z like HologramLine?
                // Or create ArmorStand here.
                ArmorStand as = new ArmorStand(level, this.x, currentY, this.z);
                as.setId(getNextEntityId());
                configureArmorStand(as);
                // Set initial name
                String initialText = data.frames.isEmpty() ? "" : data.frames.get(0);
                as.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(initialText, null)));
                
                line = new AnimatedHologramLine(as, data.frames, data.interval * 20); // interval is seconds usually, convert to ticks
            } else {
                String text = (content != null) ? content.toString() : "";
                line = new HologramLine(level, this.x, currentY, this.z, text);
            }
            
            hologramLines.add(line);
            currentY -= LINE_SPACING;
        }

        if (this.spawned) {
            for (UUID playerUUID : new ArrayList<>(nearbyPlayers)) {
                ServerPlayer player = getPlayerByUUID(playerUUID);
                if (player != null) {
                    hologramLines.forEach(line -> line.spawnToPlayer(player));
                }
            }
        }
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

    private static synchronized int getNextEntityId() {
        return nextEntityId++;
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

        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        double currentY = this.y;
        for (HologramLine line : hologramLines) {
            line.setPosition(this.x, currentY, this.z);
            currentY -= LINE_SPACING;
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
        saveToConfig();
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
        LOGGER.debug("Hologram {} marked as despawned/inactive.", id);
    }
    
    public void spawnForPlayer(ServerPlayer player) {
        if (player == null || !this.spawned || nearbyPlayers.contains(player.getUUID())) {
            return;
        }
        if (!isPlayerInCorrectWorld(player)) return;

        LOGGER.debug("Spawning hologram {} for player {}", id, player.getName().getString());
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
            if (!(line instanceof AnimatedHologramLine)) { // Don't spam animated lines here, tick handles them
                line.updateForPlayer(player, false);
            }
        });
    }
    
    public void tick() {
        for (HologramLine line : hologramLines) {
            if (line instanceof AnimatedHologramLine animated) {
                if (animated.tick()) {
                     // Update for nearby players
                     for (UUID uuid : nearbyPlayers) {
                         ServerPlayer p = getPlayerByUUID(uuid);
                         if (p != null) animated.updateForPlayer(p, false);
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
}