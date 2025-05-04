package com.strictgaming.elite.holograms.neo21.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologramBuilder;

/**
 * NeoForge implementation of hologram factory
 */
public class NeoForgeHologramFactory implements HologramFactory {
    
    @Override
    public HologramBuilder builder() {
        return new NeoForgeHologramBuilder();
    }
    
    @Override
    public Hologram createHologram(String id, String world, double x, double y, double z, String... lines) {
        return builder()
                .id(id)
                .world(world)
                .position(x, y, z)
                .lines(lines)
                .build();
    }
} 