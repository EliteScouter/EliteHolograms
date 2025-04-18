package com.strictgaming.elite.holograms.forge.hologram.entity;

import com.strictgaming.elite.holograms.forge.ForgeHolograms;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge.util.UtilPlaceholder;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 *
 * Manages a single line in a hologram
 *
 */
public class HologramLine {

    private static int ENTITY_ID = -1000; // Start with a negative ID to avoid conflicts with actual entities

    private final ArmorStand armorStand;
    private String text;

    public HologramLine(ArmorStand armorStand) {
        this.armorStand = armorStand;
        this.initEntity();
    }

    private void initEntity() {
        this.armorStand.setInvisible(true);
        this.armorStand.setNoGravity(true);
        this.armorStand.setCustomNameVisible(true);
        this.armorStand.setBoundingBox(this.armorStand.getBoundingBox().inflate(-0.95, -0.95, -0.95));
        this.armorStand.setId(ENTITY_ID--); // Assign a unique entity ID
    }

    public void setText(String text) {
        this.text = text;
        
        if (this.text.equals("{empty}")) {
            this.armorStand.setCustomNameVisible(false);
            this.armorStand.setCustomName(Component.literal(" "));
        } else {
            if (ForgeHolograms.getInstance().arePlaceholdersEnabled()) {
                // Just set the raw text, placeholder replacement happens in updateForPlayer
                this.armorStand.setCustomName(UtilChatColour.parse(text));
            } else {
                this.armorStand.setCustomName(UtilChatColour.parse(text.replace("%", "%%")));
            }
        }
    }

    public void despawnForPlayer(ServerPlayer player) {
        player.connection.send(new ClientboundRemoveEntitiesPacket(this.armorStand.getId()));
    }

    public void spawnForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(this.armorStand);
        player.connection.send(packet);
        this.updateForPlayer(player);
    }

    public void updateForPlayer(ServerPlayer player) {
        if (ForgeHolograms.getInstance().arePlaceholdersEnabled()) {
            try {
                String replaced = UtilPlaceholder.replacePlaceholders(player, this.text);
                this.armorStand.setCustomName(UtilChatColour.parse(replaced));
            } catch (Exception e) {
                // Handle the case where placeholder API might throw an exception
                this.armorStand.setCustomName(UtilChatColour.parse(this.text.replace("%", "%%")));
            }
        }
        
        // In 1.19.2, use the correct constructor
        try {
            // The constructor accepts (int, SynchedEntityData, boolean)
            player.connection.send(new ClientboundSetEntityDataPacket(
                    this.armorStand.getId(), 
                    this.armorStand.getEntityData(), 
                    true));
        } catch (Exception e) {
            // Silently fail if packet sending fails
            e.printStackTrace();
        }
    }

    public void sendTeleportPacket(ServerPlayer player) {
        player.connection.send(new ClientboundTeleportEntityPacket(this.armorStand));
    }

    public void setWorld(Level world) {
        // In 1.19.2, we need to handle world changes differently
        if (this.armorStand.level != world) {
            double x = this.armorStand.getX();
            double y = this.armorStand.getY();
            double z = this.armorStand.getZ();
            
            // First detach from current world safely
            if (this.armorStand.level != null) {
                // Instead of using unRide() and removeEntityComplete, use standard methods
                this.armorStand.stopRiding();
                this.armorStand.remove(ArmorStand.RemovalReason.DISCARDED);
            }
            
            // Then set level and position
            this.armorStand.level = world;
            this.armorStand.setPos(x, y, z);
        }
    }

    public void setPosition(double x, double y, double z) {
        this.armorStand.setPos(x, y, z);
    }

    public String getText() {
        return this.text;
    }
} 
