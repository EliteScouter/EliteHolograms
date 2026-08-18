package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.neo21.hologram.HologramDisplayType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * NeoForge implementation of hologram builder.
 *
 * <p>The inherited methods narrow their return type so display type and rotation can be set in
 * the same chain when building through this class directly.
 */
public class NeoForgeHologramBuilder implements HologramBuilder {
    
    private String id;
    private String world;
    private double x;
    private double y;
    private double z;
    private final List<String> lines = new ArrayList<>();
    private HologramDisplayType displayType = HologramDisplayType.FACING;
    private float yaw = 0.0F;
    private float pitch = 0.0F;
    
    @Override
    public NeoForgeHologramBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    @Override
    public NeoForgeHologramBuilder lines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        return this;
    }
    
    @Override
    public NeoForgeHologramBuilder lines(String... lines) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(lines));
        return this;
    }
    
    @Override
    public NeoForgeHologramBuilder world(String world) {
        this.world = world;
        return this;
    }
    
    @Override
    public NeoForgeHologramBuilder position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Sets how the hologram renders its lines.
     *
     * @param displayType facing (armor stand nameplates) or fixed (text displays)
     * @return The builder instance
     */
    public NeoForgeHologramBuilder displayType(HologramDisplayType displayType) {
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        return this;
    }

    /**
     * Sets the orientation used when the display type is fixed.
     *
     * @param yaw   rotation around the Y axis in degrees
     * @param pitch rotation around the X axis in degrees
     * @return The builder instance
     */
    public NeoForgeHologramBuilder rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        return this;
    }
    
    @Override
    public Hologram build() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        
        return new NeoForgeHologram(id, world, x, y, z, lines, displayType, yaw, pitch);
    }
    
    @Override
    public Hologram buildAndSpawn() {
        Hologram hologram = build();
        hologram.spawn();
        return hologram;
    }
} 
