package com.strictgaming.elite.holograms.forge.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologramBuilder;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
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
        builder.position(x, y, z);
        return builder;
    }

    public void reload() throws IOException {
        try {
            List<Hologram> holograms = HologramManager.getAllHolograms();
            if (holograms != null && !holograms.isEmpty()) {
                LOGGER.info("Saving " + holograms.size() + " holograms before reload");
                HologramManager.getSaver().save(java.util.Arrays.asList(holograms.toArray(new Hologram[0])));
            }
            HologramManager.clear();
            HologramManager.load();
        } catch (Exception e) {
            LOGGER.error("Error during reload", e);
            throw new IOException("Error during reload", e);
        }
    }
} 
