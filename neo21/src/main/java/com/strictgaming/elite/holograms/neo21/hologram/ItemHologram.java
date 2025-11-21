package com.strictgaming.elite.holograms.neo21.hologram;

import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public class ItemHologram extends NeoForgeHologram {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double ITEM_TEXT_GAP = 0.8; 
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-2000000000);
    
    private final String itemId;
    private ArmorStand itemStand;
    private ItemStack itemStack;
    
    public ItemHologram(String id, String world, double x, double y, double z, String itemId, List<String> lines) {
        super(id, world, x, y, z, lines);
        this.itemId = itemId;
        // Don't init yet - constructor is called during load, before server level is available
        LOGGER.debug("ItemHologram {} created, will initialize item stand on spawn.", id);
    }
    
    private void initItemStand() {
        ServerLevel level = getServerLevel();
        if (level == null) {
            LOGGER.debug("Could not initialize item stand for hologram {}: level not found.", getId());
            return;
        }
        
        this.itemStack = createItemStack(itemId);
        
        // Create the item display armor stand ABOVE the text lines
        this.itemStand = new ArmorStand(level, getX(), getY() + ITEM_TEXT_GAP, getZ());
        this.itemStand.setId(NEXT_ENTITY_ID.getAndIncrement());
        
        this.itemStand.setInvisible(true);
        this.itemStand.setNoGravity(true);
        this.itemStand.setCustomNameVisible(false);
        this.itemStand.setSilent(true);
        this.itemStand.setInvulnerable(true);
        this.itemStand.getPersistentData().putBoolean("Marker", true);
        
        this.itemStand.setItemSlot(EquipmentSlot.HEAD, itemStack);
        LOGGER.debug("Initialized item stand for hologram {}.", getId());
    }
    
    private ItemStack createItemStack(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Fall back
        }
        return new ItemStack(Items.DIRT);
    }
    
    @Override
    public void spawn() {
        super.spawn();
        // Ensure item is initialized when spawning
        if (itemStand == null) {
            initItemStand();
        }
    }
    
    @Override
    public void spawnForPlayer(ServerPlayer player) {
        super.spawnForPlayer(player);
        spawnItemFor(player);
    }
    
    @Override
    public void despawnForPlayer(ServerPlayer player) {
        despawnItemFor(player);
        super.despawnForPlayer(player);
    }
    
    private void spawnItemFor(ServerPlayer player) {
        // Try to init if null
        if (itemStand == null) {
            initItemStand();
        }

        if (player == null || player.connection == null || itemStand == null) return;
        
        ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(
            this.itemStand.getId(),
            this.itemStand.getUUID(),
            this.itemStand.getX(),
            this.itemStand.getY(),
            this.itemStand.getZ(),
            this.itemStand.getXRot(), 
            this.itemStand.getYRot(), 
            this.itemStand.getType(),
            0, 
            Vec3.ZERO, 
            this.itemStand.getYHeadRot() 
        );
        player.connection.send(addPacket);
        
        try {
            List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> equipment = 
                List.of(com.mojang.datafixers.util.Pair.of(EquipmentSlot.HEAD, itemStack));
            player.connection.send(new ClientboundSetEquipmentPacket(this.itemStand.getId(), equipment));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            player.connection.send(new ClientboundSetEntityDataPacket(
                this.itemStand.getId(), 
                this.itemStand.getEntityData().getNonDefaultValues()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void despawnItemFor(ServerPlayer player) {
        if (player == null || player.connection == null || itemStand == null) return;
        player.connection.send(new ClientboundRemoveEntitiesPacket(this.itemStand.getId()));
    }
    
    @Override
    public void setPosition(String world, double x, double y, double z) {
        super.setPosition(world, x, y, z);
        
        if (itemStand == null) {
            initItemStand();
        }

        if (itemStand != null) {
            itemStand.setPos(x, y + ITEM_TEXT_GAP, z);
            
            if (isSpawned()) {
                 for (UUID uuid : getNearbyPlayersView()) {
                     if (ServerLifecycleHooks.getCurrentServer() == null) continue;
                     ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
                     if (p != null) {
                         p.connection.send(new ClientboundTeleportEntityPacket(this.itemStand));
                     }
                 }
            }
        }
    }
    
    public String getItemId() {
        return itemId;
    }
    
    private ServerLevel getServerLevel() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        String worldName = getWorld();
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(worldName)) {
                return level;
            }
        }
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }

    // Also need to override update() to ensure item is re-initialized if needed during full refresh
    @Override
    public void update() {
        super.update();
        if (itemStand == null && isSpawned()) {
            initItemStand();
            // If it initialized successfully, spawn for nearby players
            if (itemStand != null) {
                 for (UUID uuid : getNearbyPlayersView()) {
                     if (ServerLifecycleHooks.getCurrentServer() == null) continue;
                     ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
                     if (p != null) {
                         spawnItemFor(p);
                     }
                 }
            }
        }
    }
}
