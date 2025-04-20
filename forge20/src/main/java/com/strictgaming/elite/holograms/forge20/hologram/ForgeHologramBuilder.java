package com.strictgaming.elite.holograms.forge20.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import com.google.common.collect.Lists;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 *
 * The forge implementation of the {@link HologramBuilder} interface
 *
 */
public class ForgeHologramBuilder implements HologramBuilder {

    private String id;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private int range;
    private List<String> lines = Lists.newArrayList();

    public ForgeHologramBuilder() {}

    @Override
    public HologramBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public HologramBuilder world(String worldName) {
        this.worldName = worldName;
        return this;
    }

    @Override
    public HologramBuilder position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    /**
     * Sets the position with integer coordinates
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return This builder
     */
    public HologramBuilder position(int x, int y, int z) {
        return position((double)x, (double)y, (double)z);
    }

    @Override
    public HologramBuilder range(int range) {
        this.range = range;
        return this;
    }

    @Override
    public HologramBuilder line(String line) {
        this.lines.add(line);
        return this;
    }

    @Override
    public HologramBuilder lines(String... lines) {
        this.lines.addAll(Arrays.asList(lines));
        return this;
    }

    @Override
    public Hologram build(boolean save) {
        Level world = UtilWorld.findWorld(this.worldName);

        if (world == null) {
            System.out.println("ERROR THE WORLD CANNOT BE FOUND");
            return null;
        }

        Vec3 pos = new Vec3(this.x, this.y, this.z);
        return new ForgeHologram(this.id, world, pos, this.range, save, this.lines.toArray(new String[0]));
    }
} 