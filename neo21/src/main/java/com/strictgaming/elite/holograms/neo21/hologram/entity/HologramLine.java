package com.strictgaming.elite.holograms.neo21.hologram.entity;

import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPlaceholder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HologramLine {
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-2000000000);
    
    protected final ArmorStand armorStand;
    protected String rawText;

    public HologramLine(ServerLevel level, double x, double y, double z, String rawText) {
        this.rawText = rawText;
        this.armorStand = new ArmorStand(level, x, y, z);
        this.armorStand.setId(NEXT_ENTITY_ID.getAndIncrement());
        configureArmorStand();
        // Set initial name with server-side placeholders resolved, player-side raw
        this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(rawText, null)));
    }
    
    protected HologramLine(ArmorStand armorStand, String rawText) {
        this.armorStand = armorStand;
        this.rawText = rawText;
        if (this.armorStand.getId() == 0) { 
             this.armorStand.setId(NEXT_ENTITY_ID.getAndIncrement());
        }
        configureArmorStand();
        this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(rawText, null)));
    }

    private void configureArmorStand() {
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setSilent(true);
        armorStand.setInvulnerable(true);
        armorStand.getPersistentData().putBoolean("Marker", true);
        armorStand.addTag("spectral_vision_unaffected");
    }

    public void spawnToPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        
        ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(
            this.armorStand.getId(),
            this.armorStand.getUUID(),
            this.armorStand.getX(),
            this.armorStand.getY(),
            this.armorStand.getZ(),
            this.armorStand.getXRot(), 
            this.armorStand.getYRot(), 
            this.armorStand.getType(),
            0, 
            Vec3.ZERO, 
            this.armorStand.getYHeadRot() 
        );
        player.connection.send(addPacket);
        updateForPlayer(player, true);
    }

    public void updateForPlayer(ServerPlayer player) {
        updateForPlayer(player, false);
    }

    public void updateForPlayer(ServerPlayer player, boolean isInitialSpawn) {
        if (player == null || player.connection == null) return;

        String processedText = this.rawText;
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
            packedData = armorStand.getEntityData().packDirty(); 
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
        this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(newRawText, null)));
        playersToRefreshFor.forEach(p -> this.updateForPlayer(p, false));
    }
    
    public String getText() {
        return rawText;
    }
    
    public ArmorStand getArmorStand() {
        return armorStand;
    }
}

