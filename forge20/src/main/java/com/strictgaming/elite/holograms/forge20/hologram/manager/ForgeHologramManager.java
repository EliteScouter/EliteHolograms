package com.strictgaming.elite.holograms.forge20.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologramBuilder;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

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
            // Flush memory to disk first so nothing edited in game is lost, unless a file was
            // edited by hand, in which case flushing would overwrite the very edit this reload
            // is meant to pick up. Every in-game edit is already saved as it happens, so
            // skipping the flush then costs nothing.
            if (HologramManager.storesChangedOnDisk()) {
                LOGGER.info("Hologram files were edited outside the game, loading those edits");
            } else {
                HologramManager.saveSync();
            }

            Forge20Holograms.getInstance().getConfig().load();
            // load() despawns, clears, and reloads from disk under the same lock
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
