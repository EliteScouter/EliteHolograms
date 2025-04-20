package com.strictgaming.elite.holograms.forge20.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologramBuilder;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The Forge implementation of the {@link HologramFactory}
 */
public class ForgeHologramFactory implements HologramFactory {

    @Override
    public HologramBuilder builder() {
        return new ForgeHologramBuilder();
    }

    @Override
    public Hologram getById(String id) {
        return HologramManager.getById(id);
    }

    @Override
    public List<Hologram> getNearby(String worldName, double x, double y, double z, double radius) {
        Level world = UtilWorld.findWorld(worldName);
        if (world == null) {
            return List.of();
        }
        
        Vec3 position = new Vec3(x, y, z);
        
        return HologramManager.getAllHolograms().stream()
                .filter(hologram -> {
                    if (hologram.getWorldName() == null || !hologram.getWorldName().equals(world.dimension().location().toString())) {
                        return false;
                    }

                    double[] hologramPos = hologram.getLocation();
                    double dx = x - hologramPos[0];
                    double dy = y - hologramPos[1];
                    double dz = z - hologramPos[2];

                    return dx * dx + dy * dy + dz * dz <= radius * radius;
                })
                .collect(Collectors.toList());
    }
} 