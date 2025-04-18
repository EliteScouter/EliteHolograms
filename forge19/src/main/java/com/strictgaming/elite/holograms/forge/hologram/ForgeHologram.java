package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.api.exception.HologramException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
import com.strictgaming.elite.holograms.forge.util.UtilWorld;
import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final List<UUID> nearbyPlayers = Lists.newArrayList();

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
    }
    
    @Override
    public void teleport(String worldName, double x, double y, double z) {
        Level foundWorld = UtilWorld.findWorld(worldName);

        if (foundWorld == null) {
            return;
        }

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
        System.out.println("[AdvancedHolograms] Refreshing visibility for hologram: " + this.id);
        
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
} 
