package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * A hologram that displays a floating item with text lines below it
 */
public class ItemHologram extends ForgeHologram {
    
    private static final double ITEM_TEXT_GAP = 0.8; // Gap between item and first text line
    private static int entityIdCounter = -5000;
    
    private final String itemId;
    private transient ArmorStand itemStand;
    private transient ItemStack itemStack;
    
    public ItemHologram(String id, Level world, Vec3 position, int range, String itemId, String... lines) {
        super(id, world, position, range, false); // Don't save in parent constructor
        
        this.itemId = itemId;
        this.itemStack = createItemStack(itemId);
        
        // Create the item display armor stand ABOVE the text lines
        this.itemStand = new ArmorStand(world, position.x, position.y + ITEM_TEXT_GAP, position.z);
        initItemStand();
        
        // Add text lines
        if (lines != null && lines.length > 0) {
            this.addLines(lines);
        }
        
        HologramManager.save();
    }
    
    private void initItemStand() {
        this.itemStand.setInvisible(true);
        this.itemStand.setNoGravity(true);
        this.itemStand.setCustomNameVisible(false);
        this.itemStand.setBoundingBox(this.itemStand.getBoundingBox().inflate(-0.95, -0.95, -0.95));
        this.itemStand.setId(getNextEntityId());
        
        // Set the item in the armor stand's head slot for better visibility
        this.itemStand.setItemSlot(EquipmentSlot.HEAD, itemStack);
    }
    
    private ItemStack createItemStack(String itemId) {
        try {
            ResourceLocation location = new ResourceLocation(itemId);
            Item item = Registry.ITEM.get(location);
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Fall back to dirt if invalid item
        }
        return new ItemStack(Registry.ITEM.get(new ResourceLocation("minecraft:dirt")));
    }
    
    /**
     * Spawn the item display for a player
     */
    public void spawnItemFor(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        
        // Send spawn packet
        player.connection.send(new ClientboundAddEntityPacket(this.itemStand));
        
        // Send equipment packet to show the item
        try {
            List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> equipment = 
                List.of(com.mojang.datafixers.util.Pair.of(EquipmentSlot.HEAD, itemStack));
            player.connection.send(new ClientboundSetEquipmentPacket(this.itemStand.getId(), equipment));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Send entity data
        try {
            player.connection.send(new ClientboundSetEntityDataPacket(
                this.itemStand.getId(), 
                this.itemStand.getEntityData(), 
                true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Despawn the item display for a player
     */
    public void despawnItemFor(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        
        player.connection.send(new ClientboundRemoveEntitiesPacket(this.itemStand.getId()));
    }
    
    public String getItemId() {
        return this.itemId;
    }
    
    public ArmorStand getItemStand() {
        return this.itemStand;
    }
    
    @Override
    public void teleport(String worldName, double x, double y, double z) {
        // Despawn item for all nearby players
        for (UUID playerUUID : getNearbyPlayers()) {
            ServerPlayer player = com.strictgaming.elite.holograms.forge.util.UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                despawnItemFor(player);
            }
        }

        // Call parent teleport to handle lines and world/position update
        super.teleport(worldName, x, y, z);

        // Update item stand position
        this.itemStand.moveTo(x, y + ITEM_TEXT_GAP, z);
        
        // Note: super.teleport() clears nearbyPlayers, so HologramManager will re-detect nearby players
        // and call spawnItemFor() automatically. We just needed to update the itemStand position.
    }
    
    @Override
    public void move(String world, double x, double y, double z) {
        // First despawn the item for all nearby players
        for (UUID playerUUID : getNearbyPlayers()) {
            ServerPlayer player = com.strictgaming.elite.holograms.forge.util.UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                despawnItemFor(player);
            }
        }
        
        // Call parent to move text lines
        super.move(world, x, y, z);
        
        // Update item stand position
        this.itemStand.moveTo(x, y + ITEM_TEXT_GAP, z);
        
        // Respawn item for all nearby players
        for (UUID playerUUID : getNearbyPlayers()) {
            ServerPlayer player = com.strictgaming.elite.holograms.forge.util.UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                spawnItemFor(player);
            }
        }
        
        HologramManager.save();
    }
    
    @Override
    public void despawn() {
        // Despawn item for all nearby players
        for (UUID playerUUID : getNearbyPlayers()) {
            ServerPlayer player = com.strictgaming.elite.holograms.forge.util.UtilPlayer.getOnlinePlayer(playerUUID);
            if (player != null) {
                despawnItemFor(player);
            }
        }
        
        // Call parent to despawn text lines
        super.despawn();
    }
    
    private static synchronized int getNextEntityId() {
        return entityIdCounter--;
    }
}

