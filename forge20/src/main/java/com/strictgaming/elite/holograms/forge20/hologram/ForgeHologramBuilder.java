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
    private HologramDisplayType displayType = HologramDisplayType.FACING;
    private float yaw = 0.0F;
    private float pitch = 0.0F;

    public ForgeHologramBuilder() {}

    @Override
    public ForgeHologramBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ForgeHologramBuilder world(String worldName) {
        this.worldName = worldName;
        return this;
    }

    @Override
    public ForgeHologramBuilder position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Sets how the hologram renders its lines.
     *
     * @param displayType facing (armor stand nameplates) or fixed (text displays)
     * @return This builder
     */
    public ForgeHologramBuilder displayType(HologramDisplayType displayType) {
        this.displayType = displayType == null ? HologramDisplayType.FACING : displayType;
        return this;
    }

    /**
     * Sets the orientation used when the display type is fixed.
     *
     * @param yaw   rotation around the Y axis in degrees
     * @param pitch rotation around the X axis in degrees
     * @return This builder
     */
    public ForgeHologramBuilder rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
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
    public ForgeHologramBuilder position(int x, int y, int z) {
        return position((double)x, (double)y, (double)z);
    }

    @Override
    public ForgeHologramBuilder range(int range) {
        this.range = range;
        return this;
    }

    @Override
    public ForgeHologramBuilder line(String line) {
        this.lines.add(line);
        return this;
    }

    @Override
    public ForgeHologramBuilder lines(String... lines) {
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
        return new ForgeHologram(this.id, world, pos, this.range, save,
                this.displayType, this.yaw, this.pitch, this.lines.toArray(new String[0]));
    }
} 