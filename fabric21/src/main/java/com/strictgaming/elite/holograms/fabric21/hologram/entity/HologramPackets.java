package com.strictgaming.elite.holograms.fabric21.hologram.entity;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class HologramPackets {

    private HologramPackets() {
    }

    public static ClientboundAddEntityPacket addEntity(Entity entity) {
        return new ClientboundAddEntityPacket(
                entity.getId(),
                entity.getUUID(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getXRot(),
                entity.getYRot(),
                entity.getType(),
                0,
                Vec3.ZERO,
                entity.getYHeadRot()
        );
    }
}
