package com.strictgaming.elite.holograms.forge20.hologram.entity;

import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge20.util.UtilPlaceholder;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Manages a single line in a hologram
 */
public class HologramLine {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static int ENTITY_ID = -1000; // Start with a negative ID to avoid conflicts

    private ArmorStand armorStand;
    private String text;

    public HologramLine(ArmorStand armorStand) {
        this.armorStand = armorStand;
        this.setupArmorStand();
    }

    private void setupArmorStand() {
        try {
            // Set a unique entity ID first
            this.armorStand.setId(ENTITY_ID--);
            
            // Make the armor stand completely invisible but show the custom name
            this.armorStand.setInvisible(true);
            this.armorStand.setNoGravity(true);
            this.armorStand.setSilent(true);
            this.armorStand.setCustomNameVisible(true);
            this.armorStand.setInvulnerable(true);
            
            // Don't use potentially private methods
            // this.armorStand.setSmall(true); // This is private in 1.20
            // this.armorStand.setNoBasePlate(true); // Might also be private
            // this.armorStand.setShowArms(false); // Might also be private
            
            // Try setting marker flag via reflection as it's safer
            try {
                Field markerField = ArmorStand.class.getDeclaredField("marker");
                markerField.setAccessible(true);
                markerField.set(this.armorStand, true);
            } catch (Exception e) {
                LOGGER.debug("Failed to set marker flag via reflection - this is normal", e);
            }
            
            // Set minimal size bounding box
            try {
                this.armorStand.setBoundingBox(this.armorStand.getBoundingBox().inflate(-0.99, -0.99, -0.99));
            } catch (Exception e) {
                LOGGER.debug("Could not shrink bounding box", e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error setting up hologram line", e);
        }
    }

    public void setText(String text) {
        try {
            // Store the raw text
            this.text = text;
            
            if (text == null || text.isEmpty()) {
                // Provide a default empty text
                this.armorStand.setCustomName(Component.literal(" "));
                this.armorStand.setCustomNameVisible(true);
                return;
            }
            
            if (this.text.equals("{empty}")) {
                this.armorStand.setCustomNameVisible(false);
                this.armorStand.setCustomName(Component.literal(" "));
            } else {
                // Always ensure custom name is visible
                this.armorStand.setCustomNameVisible(true);
                
                // Process text with or without placeholders
                Component textComponent;
                if (Forge20Holograms.getInstance().arePlaceholdersEnabled()) {
                    textComponent = UtilChatColour.parse(text);
                } else {
                    textComponent = UtilChatColour.parse(text.replace("%", "%%"));
                }
                
                // Set the custom name
                this.armorStand.setCustomName(textComponent);
                
                // Log for debug
                LOGGER.debug("Set hologram text to: {}", text);
            }
            
            // No need to manually force an update - the game handles this automatically
            // when you set properties like CustomName and CustomNameVisible
        } catch (Exception e) {
            LOGGER.warn("Error setting hologram text: {}", text, e);
        }
    }

    public void despawnForPlayer(ServerPlayer player) {
        if (player != null && player.connection != null) {
            try {
                player.connection.send(new ClientboundRemoveEntitiesPacket(this.armorStand.getId()));
            } catch (Exception e) {
                LOGGER.debug("Error despawning hologram for player", e);
            }
        }
    }

    public void spawnForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }

        try {
            ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(this.armorStand);
            player.connection.send(packet);
            
            // Send an update after spawning
            updateForPlayer(player);
        } catch (Exception e) {
            LOGGER.warn("Error spawning hologram for player", e);
        }
    }

    public void updateForPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        
        try {
            // Update name with placeholders if enabled
            if (Forge20Holograms.getInstance().arePlaceholdersEnabled() && text != null) {
                try {
                    // Generate player-specific text with placeholders
                    String replaced = UtilPlaceholder.replacePlaceholders(player, this.text);
                    
                    // Only set if different to avoid unnecessary updates
                    if (!replaced.equals(this.armorStand.getCustomName().getString())) {
                        this.armorStand.setCustomName(UtilChatColour.parse(replaced));
                    }
                } catch (Exception e) {
                    // Fall back to regular text if placeholder processing fails
                    this.armorStand.setCustomName(UtilChatColour.parse(this.text.replace("%", "%%")));
                    LOGGER.debug("Error processing placeholders, using fallback text", e);
                }
            }
            
            // Create a custom entity data packet for the name and visibility
            try {
                // For 1.20.1, we need to send both teleport and entity data
                // First send teleport to ensure position is correct
                this.sendTeleportPacket(player);
                
                // Then try to send data if possible
                if (this.armorStand.getEntityData() != null) {
                    ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(
                            this.armorStand.getId(), 
                            this.armorStand.getEntityData().getNonDefaultValues());
                    player.connection.send(dataPacket);
                }
            } catch (Exception e) {
                // At minimum, ensure position is updated
                this.sendTeleportPacket(player);
                LOGGER.debug("Error sending entity data packet, falling back to teleport only", e);
            }
        } catch (Exception e) {
            LOGGER.debug("Error updating hologram for player", e);
        }
    }

    public void sendTeleportPacket(ServerPlayer player) {
        if (player != null && player.connection != null) {
            try {
                player.connection.send(new ClientboundTeleportEntityPacket(this.armorStand));
            } catch (Exception e) {
                LOGGER.debug("Error sending teleport packet", e);
            }
        }
    }

    public void setWorld(Level world) {
        try {
            // In 1.20, we need to handle world changes differently 
            // as the field name may have changed
            if (this.armorStand.level() != world) {
                double x = this.armorStand.getX();
                double y = this.armorStand.getY();
                double z = this.armorStand.getZ();
                
                // First detach from current world safely
                if (this.armorStand.level() != null) {
                    // Remove from existing world
                    this.armorStand.stopRiding();
                    this.armorStand.remove(ArmorStand.RemovalReason.DISCARDED);
                }
                
                // Create a new armor stand in the new world
                ArmorStand newStand = new ArmorStand(world, x, y, z);
                
                // Copy settings from old armor stand
                newStand.setId(this.armorStand.getId());
                newStand.setInvisible(true);
                newStand.setNoGravity(true);
                newStand.setSilent(true);
                newStand.setCustomNameVisible(true);
                newStand.setInvulnerable(true);
                newStand.setCustomName(this.armorStand.getCustomName());
                
                // Replace the armor stand reference
                this.armorStand = newStand;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update hologram line world", e);
        }
    }

    public void setPosition(double x, double y, double z) {
        this.armorStand.setPos(x, y, z);
    }

    public String getText() {
        return this.text;
    }

    public int getEntityId() {
        return this.armorStand.getId();
    }

    public Vec3 getPosition() {
        return this.armorStand.position();
    }
} 