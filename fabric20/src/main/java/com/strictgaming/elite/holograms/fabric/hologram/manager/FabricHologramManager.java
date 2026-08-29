package com.strictgaming.elite.holograms.fabric.hologram.manager;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.fabric.FabricHolograms;
import com.strictgaming.elite.holograms.fabric.hologram.FabricHologramBuilder;
import com.strictgaming.elite.holograms.fabric.hologram.HologramManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 *
 * The forge implementation of the {@link PlatformHologramManager} interface
 *
 */
public class FabricHologramManager implements PlatformHologramManager {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private final FabricHologramFactory factory = new FabricHologramFactory();

    @Override
    public HologramFactory getFactory() {
        return this.factory;
    }

    @Override
    public boolean arePlaceholdersEnabled() {
        return FabricHolograms.getInstance().arePlaceholdersEnabled();
    }

    @Override
    public void reload() throws IOException {
        try {
            // Save synchronously under SAVE_LOAD_LOCK before clearing/reloading.
            // Async saves that read HOLOGRAMS after clear() used to write [] and wipe the file.
            HologramManager.saveSync();

            FabricHolograms.getInstance().getConfig().load();
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
        return new FabricHologramBuilder();
    }

    @Override
    public HologramBuilder builder(String id) {
        return new FabricHologramBuilder().id(id);
    }

    @Override
    public HologramBuilder builder(String... lines) {
        HologramBuilder builder = new FabricHologramBuilder();
        builder.lines(lines);
        return builder;
    }

    @Override
    public HologramBuilder builder(String world, int x, int y, int z) {
        HologramBuilder builder = new FabricHologramBuilder();
        builder.world(world);
        builder.position(x, y, z);
        return builder;
    }
}
