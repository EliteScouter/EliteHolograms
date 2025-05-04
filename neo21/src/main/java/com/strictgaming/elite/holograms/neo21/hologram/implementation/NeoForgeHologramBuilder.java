package com.strictgaming.elite.holograms.neo21.hologram.implementation;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * NeoForge implementation of hologram builder
 */
public class NeoForgeHologramBuilder implements HologramBuilder {
    
    private String id;
    private String world;
    private double x;
    private double y;
    private double z;
    private final List<String> lines = new ArrayList<>();
    
    @Override
    public HologramBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    @Override
    public HologramBuilder lines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        return this;
    }
    
    @Override
    public HologramBuilder lines(String... lines) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(lines));
        return this;
    }
    
    @Override
    public HologramBuilder world(String world) {
        this.world = world;
        return this;
    }
    
    @Override
    public HologramBuilder position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    @Override
    public Hologram build() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        
        return new NeoForgeHologram(id, world, x, y, z, lines);
    }
    
    @Override
    public Hologram buildAndSpawn() {
        Hologram hologram = build();
        hologram.spawn();
        return hologram;
    }
} 