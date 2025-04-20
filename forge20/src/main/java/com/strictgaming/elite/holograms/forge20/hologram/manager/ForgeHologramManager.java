package com.strictgaming.elite.holograms.forge20.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologramBuilder;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

/**
 *
 * The forge implementation of the {@link PlatformHologramManager} interface
 *
 */
public class ForgeHologramManager implements PlatformHologramManager {

    private final ForgeHologramFactory factory = new ForgeHologramFactory();

    @Override
    public HologramFactory getFactory() {
        return this.factory;
    }

    @Override
    public boolean arePlaceholdersEnabled() {
        return Forge20Holograms.getInstance().arePlaceholdersEnabled();
    }

    @Override
    public void reload() throws IOException {
        HologramManager.clear();
        Forge20Holograms.getInstance().getConfig().load();
        HologramManager.load();
    }

    @Override
    public void clear() {
        HologramManager.clear();
    }

    @Override
    public HologramBuilder builder() {
        return new ForgeHologramBuilder();
    }

    @Override
    public HologramBuilder builder(String id) {
        return new ForgeHologramBuilder().id(id);
    }

    @Override
    public HologramBuilder builder(String... lines) {
        HologramBuilder builder = new ForgeHologramBuilder();
        builder.lines(lines);
        return builder;
    }

    @Override
    public HologramBuilder builder(String world, int x, int y, int z) {
        HologramBuilder builder = new ForgeHologramBuilder();
        builder.world(world);
        builder.position(x, y, z);
        return builder;
    }
} 