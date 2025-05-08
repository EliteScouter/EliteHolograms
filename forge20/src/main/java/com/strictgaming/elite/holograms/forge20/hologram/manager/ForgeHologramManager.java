package com.strictgaming.elite.holograms.forge20.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologramBuilder;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 *
 * The forge implementation of the {@link PlatformHologramManager} interface
 *
 */
public class ForgeHologramManager implements PlatformHologramManager {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
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
        try {
            // First save any pending changes synchronously to ensure they're written to disk
            List<Hologram> holograms = HologramManager.getAllHolograms();
            
            // Instead of async save, use direct synchronous save
            if (holograms != null && !holograms.isEmpty()) {
                // Use the saver directly
                LOGGER.info("Saving " + holograms.size() + " holograms before reload");
                HologramManager.getSaver().save(java.util.Arrays.asList(holograms.toArray(new Hologram[0])));
            }
            
            // Now proceed with normal reload
            HologramManager.clear();
            Forge20Holograms.getInstance().getConfig().load();
            HologramManager.load();
        } catch (Exception e) {
            LOGGER.error("Error during reload", e);
            throw new IOException("Error during reload", e);
        }
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