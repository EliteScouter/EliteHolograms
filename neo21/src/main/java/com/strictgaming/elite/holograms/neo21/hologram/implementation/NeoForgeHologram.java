package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
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
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.network.syncher.SynchedEntityData;
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
    private static int nextEntityId = -2000000000; // Ensure unique entity IDs
    
    private final String id;
    private String world;
    private double x;
    private double y;
    private double z;
    private List<String> rawLines = new ArrayList<>();
    private final List<HologramLine> hologramLines = new ArrayList<>();
    private final List<UUID> nearbyPlayers = Collections.synchronizedList(new ArrayList<>()); // Thread-safe list
    private boolean spawned = false; // Represents if the hologram *should* be trying to show to players
    
    /**
     * Creates a new hologram
     * 
     * @param id The hologram ID
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param lines The text lines
     */
    public NeoForgeHologram(String id, String world, double x, double y, double z, List<String> lines) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        if (lines != null) {
            this.rawLines.addAll(lines);
        }
        synchronized (com.strictgaming.elite.holograms.neo21.hologram.HologramManager.class) {
            HologramManager.addHologram(this); // Add to manager first
        }
        rebuildHologramLines(); // Then build lines
    }
    
    private void rebuildHologramLines() {
        // Despawn old lines from all currently tracked players before clearing
        for (UUID playerUUID : new ArrayList<>(nearbyPlayers)) { // Iterate a copy
            ServerPlayer player = getPlayerByUUID(playerUUID);
            if (player != null) {
                hologramLines.forEach(line -> line.despawnFromPlayer(player));
            }
        }

        hologramLines.clear();
        ServerLevel level = getServerLevel();
        if (level == null) {
            LOGGER.warn("Cannot rebuild hologram lines for {}: server level {} is null.", id, world);
            return;
        }

        double currentY = this.y;
        for (String rawLineText : this.rawLines) {
            HologramLine line = new HologramLine(level, this.x, currentY, this.z, rawLineText);
            hologramLines.add(line);
            currentY -= LINE_SPACING;
        }

        // Respawn new lines for all currently tracked players
        if (this.spawned) {
            for (UUID playerUUID : new ArrayList<>(nearbyPlayers)) { // Iterate a copy
                ServerPlayer player = getPlayerByUUID(playerUUID);
                if (player != null) {
                    hologramLines.forEach(line -> line.spawnToPlayer(player));
                }
            }
        }
    }

    private static synchronized int getNextEntityId() {
        return nextEntityId++;
    }

    private class HologramLine {
        private final ArmorStand armorStand;
        private String rawText;

        public HologramLine(ServerLevel level, double x, double y, double z, String rawText) {
            this.rawText = rawText;
            this.armorStand = new ArmorStand(level, x, y, z);
            this.armorStand.setId(getNextEntityId());
            configureArmorStand();
            // Set initial name with server-side placeholders resolved, player-side raw
            this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(rawText, null)));
        }

        private void configureArmorStand() {
            armorStand.setInvisible(true);
            armorStand.setNoGravity(true);
            armorStand.setCustomNameVisible(true);
            armorStand.setSilent(true);
            armorStand.setInvulnerable(true);
            // NeoForge uses persistent data for Marker status
            armorStand.getPersistentData().putBoolean("Marker", true);
            
            // Add Forbidden and Arcanus compatibility - prevent Spectral Eye Amulet highlighting
            armorStand.addTag("spectral_vision_unaffected");
        }

        public void spawnToPlayer(ServerPlayer player) {
            if (player == null || player.connection == null) return;
            // Use the verbose constructor for ClientboundAddEntityPacket
            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(
                this.armorStand.getId(),
                this.armorStand.getUUID(),
                this.armorStand.getX(),
                this.armorStand.getY(),
                this.armorStand.getZ(),
                this.armorStand.getXRot(), // pitch
                this.armorStand.getYRot(), // yaw
                this.armorStand.getType(),
                0, // entityData, usually 0 for armor stands unless specific state needs to be sent
                Vec3.ZERO, // deltaMovement
                this.armorStand.getYHeadRot() // yHeadRot
            );
            player.connection.send(addPacket);
            updateForPlayer(player, true); // Send initial personalized text, indicate it's the initial spawn
        }

        public void updateForPlayer(ServerPlayer player) {
            updateForPlayer(player, false); // Assume not initial spawn if called directly
        }

        public void updateForPlayer(ServerPlayer player, boolean isInitialSpawn) {
            if (player == null || player.connection == null) return;

            String processedText = this.rawText; // Start with raw text
            if (rawText.equals("{empty}")) {
                armorStand.setCustomNameVisible(false);
                armorStand.setCustomName(Component.literal(" "));
            } else {
                armorStand.setCustomNameVisible(true);
                if (Neo21Holograms.getInstance().arePlaceholdersEnabled()) {
                    processedText = UtilPlaceholder.replacePlaceholders(this.rawText, player);
                }
                armorStand.setCustomName(UtilChatColour.parse(processedText));
            }
            
            List<SynchedEntityData.DataValue<?>> packedData;
            if (isInitialSpawn) {
                packedData = armorStand.getEntityData().getNonDefaultValues();
            } else {
                packedData = armorStand.getEntityData().packDirty(); // Send only changed data for updates
            }
            
            if (packedData != null && !packedData.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(armorStand.getId(), packedData));
            }
        }

        public void despawnFromPlayer(ServerPlayer player) {
            if (player == null || player.connection == null) return;
            player.connection.send(new ClientboundRemoveEntitiesPacket(armorStand.getId()));
        }
        
        public void setPosition(double x, double y, double z) {
            armorStand.setPos(x,y,z);
        }
        
        public void sendTeleportPacket(ServerPlayer player) {
            if (player == null || player.connection == null) return;
            player.connection.send(new ClientboundTeleportEntityPacket(this.armorStand));
        }
        
        public void updateRawTextAndRefresh(String newRawText, List<ServerPlayer> playersToRefreshFor) {
            this.rawText = newRawText;
            // Update the base custom name (for server placeholders or if placeholders are off)
            this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(newRawText, null)));
            playersToRefreshFor.forEach(p -> this.updateForPlayer(p, false)); // Explicitly pass false for isInitialSpawn
        }
    }

    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public List<String> getLines() {
        return new ArrayList<>(rawLines);
    }
    
    @Override
    public void setLines(List<String> lines) {
        updateHologramContent(() -> {
            this.rawLines.clear();
            if (lines != null) {
                this.rawLines.addAll(lines);
            }
        });
    }
    
    @Override
    public void setLines(String... lines) {
        setLines(Arrays.asList(lines));
    }
    
    @Override
    public String getLine(int index) {
        return (index >= 0 && index < rawLines.size()) ? rawLines.get(index) : null;
    }
    
    @Override
    public void setLine(int index, String text) {
        if (index >= 0 && index < rawLines.size()) {
            updateHologramContent(() -> rawLines.set(index, text));
        }
    }
    
    @Override
    public void addLine(String text) {
        updateHologramContent(() -> rawLines.add(text));
    }
    
    @Override
    public void insertLine(int index, String text) {
        if (index >= 0 && index <= rawLines.size()) {
            updateHologramContent(() -> rawLines.add(index, text));
        }
    }
    
    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < rawLines.size()) {
            updateHologramContent(() -> rawLines.remove(index));
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
        List<UUID> currentPlayers = new ArrayList<>(this.nearbyPlayers); // Cache current viewers

        if (worldChanged && spawned) {
            // Despawn from all current viewers in the old world
             currentPlayers.forEach(uuid -> {
                ServerPlayer p = getPlayerByUUID(uuid);
                if (p != null) despawnForPlayer(p); // This will use the old world context
            });
            nearbyPlayers.clear(); // Clear list as they are no longer "nearby" in the new world
        }

        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        // Update position for each armorstand, they'll be in the new world if worldChanged
        double currentY = this.y;
        for (HologramLine line : hologramLines) {
            line.setPosition(this.x, currentY, this.z);
            currentY -= LINE_SPACING;
        }
        
        if (spawned) {
            if (worldChanged) {
                // Hologram moved to a new world, try to spawn for players in the new world/location
                ServerLevel newLevel = getServerLevel();
                if (newLevel != null) {
                    newLevel.getServer().getPlayerList().getPlayers().forEach(player -> {
                        if (isPlayerNearby(player)) { // Check proximity in new world
                            spawnForPlayer(player);
                        }
                    });
                }
            } else {
                // World didn't change, just teleport existing armorstands for current viewers
                currentPlayers.forEach(uuid -> {
                    ServerPlayer p = getPlayerByUUID(uuid);
                    if (p != null) {
                         hologramLines.forEach(hl -> hl.sendTeleportPacket(p)); // Send teleport for existing entities
                    }
                });
            }
        }
        saveToConfig();
    }
    
    @Override
    public void spawn() { // This is more like "enable" or "make active"
        if (this.spawned) return;
        this.spawned = true;
        // Attempt to spawn for any currently nearby players
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
    public void despawn() { // This is more like "disable" or "make inactive"
        if (!this.spawned) return;
        this.spawned = false;
        // Despawn from all players currently viewing it
        new ArrayList<>(nearbyPlayers).forEach(uuid -> { // Iterate a copy
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                despawnForPlayer(player); // This removes from nearbyPlayers list
            }
        });
         // nearbyPlayers should be empty now
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
            return; // Not tracking this player or player is null
        }
        LOGGER.debug("Despawning hologram {} for player {}", id, player.getName().getString());
        hologramLines.forEach(line -> line.despawnFromPlayer(player));
    }

    // Called periodically or on demand to refresh text for a specific player
    public void updateTextForPlayer(ServerPlayer player) {
        if (player == null || !this.spawned || !nearbyPlayers.contains(player.getUUID())) {
            return;
        }
        if (!isPlayerInCorrectWorld(player)) { // Player might have changed worlds
             despawnForPlayer(player);
             return;
        }
        LOGGER.trace("Updating text for player {} on hologram {}", player.getName().getString(), id);
        hologramLines.forEach(line -> line.updateForPlayer(player, false)); // Explicitly pass false for isInitialSpawn
    }
    
    // Called when the hologram's base lines are globally updated (e.g. /eh setline)
    @Override
    public void update() { // This method is for global updates after line changes
        if (!spawned) return;
        
        // Rebuild lines and update for all current viewers
        List<ServerPlayer> currentViewers = nearbyPlayers.stream()
            .map(this::getPlayerByUUID)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
            
        // Despawn old entities for these viewers
        hologramLines.forEach(line -> currentViewers.forEach(line::despawnFromPlayer));
        
        // Recreate the HologramLine objects with new raw text if necessary (handled by rebuildHologramLines)
        rebuildHologramLines(); // This will create new ArmorStands and HologramLines
        
        // The rebuildHologramLines method itself handles respawning to players in nearbyPlayers if spawned is true
        LOGGER.debug("Hologram {} updated globally for {} viewers.", id, currentViewers.size());
    }

    @Override
    public boolean isSpawned() {
        return spawned; // If it's "active"
    }
    
    public boolean isVisibleTo(ServerPlayer player) {
        return player != null && nearbyPlayers.contains(player.getUUID());
    }

    @Override
    public void delete() {
        LOGGER.info("Deleting hologram: {}", id);
        despawn(); // Ensure it's despawned from all players and marked inactive
        HologramManager.removeHologram(this.id); // This will also save config
    }
    
    private boolean isPlayerInCorrectWorld(ServerPlayer player) {
        return player != null && player.level().dimension().location().toString().equals(this.world);
    }

    public boolean isPlayerNearby(ServerPlayer player) { // Public for HologramManager
        if (!isPlayerInCorrectWorld(player)) {
            return false;
        }
        double distSq = player.distanceToSqr(x, y, z);
        return distSq <= (64 * 64); // Using squared distance for efficiency (64 block range)
    }

    private ServerPlayer getPlayerByUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid) : null;
    }
    
    public List<UUID> getNearbyPlayersView() { // For HologramManager, returns a copy
        return new ArrayList<>(nearbyPlayers);
    }

    private ServerLevel getServerLevel() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        if (world == null || world.isEmpty()) {
            LOGGER.warn("Hologram {}: World name is null or empty, attempting to use overworld", id);
            return ServerLifecycleHooks.getCurrentServer().overworld();
        }
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(world)) {
                return level;
            }
        }
        LOGGER.warn("Hologram {}: World '{}' not found, attempting to use overworld", id, world);
        return ServerLifecycleHooks.getCurrentServer().overworld(); // Fallback
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
        rebuildHologramLines(); // Rebuilds lines and updates for nearby players if spawned
        saveToConfig();         // Persist changes
    }
} 