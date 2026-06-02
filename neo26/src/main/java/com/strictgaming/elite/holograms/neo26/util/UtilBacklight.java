package com.strictgaming.elite.holograms.neo26.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for placing and removing invisible {@code minecraft:light} blocks
 * used by the hologram backlight feature.
 *
 * <p>The light block was added in Minecraft 1.17 (Caves &amp; Cliffs) and is
 * the same block WorldEdit's {@code //set light} uses. It produces light from
 * 0 to 15 with no visible model.</p>
 *
 * <p>The backlight is a <em>vertical column</em> of light blocks placed at the
 * hologram's horizontal position. The column starts on the ground directly
 * below the hologram and rises straight up. The configured value is the column
 * <strong>height in blocks</strong> ({@code 1}-{@code 10}): {@code 1} is a
 * single light block on the ground, {@code 10} is a ten-block-tall pillar.
 * Every block in the column emits at full brightness ({@code 15}) so the beam
 * reads as a clean vertical light rather than a patch spread across the
 * floor.</p>
 */
public final class UtilBacklight {

    /** Default column height (in blocks) when enabling a backlight. */
    public static final int DEFAULT_LEVEL = 3;
    /** Minimum column height (in blocks). */
    public static final int MIN_LEVEL = 1;
    /** Maximum column height (in blocks). */
    public static final int MAX_LEVEL = 10;
    /** Brightness emitted by every light block in the column (0-15). */
    public static final int LIGHT_EMISSION = 15;
    /** How far below the hologram we search for the ground before giving up. */
    private static final int MAX_GROUND_SCAN = 24;

    private UtilBacklight() {}

    /**
     * Clamps a requested column height into the legal {@code 1..10} range.
     */
    public static int clampLevel(int level) {
        if (level < MIN_LEVEL) return MIN_LEVEL;
        if (level > MAX_LEVEL) return MAX_LEVEL;
        return level;
    }

    /**
     * Places a vertical column of invisible light blocks. The column begins at
     * the ground directly below {@code bottomY} (found by scanning) and is made
     * tall enough to cover the whole hologram - from the ground up through
     * {@code topY} (the top line) - using {@code height} as a minimum so larger
     * values extend the column further up. Cells already holding solid world
     * content are skipped so the column never clobbers player builds.
     *
     * @param bottomY the hologram's lowest line Y; the ground is searched for
     *                from here
     * @param topY    the hologram's highest line Y; the column always rises at
     *                least this high so every text row is covered
     * @param height  the minimum number of light blocks in the column ({@code 1}-{@code 10})
     * @return the positions where a light block was actually placed
     */
    public static List<BlockPos> placeColumn(ServerLevel level, double x, double bottomY, double topY, double z, int height) {
        List<BlockPos> placed = new ArrayList<>();
        if (level == null) {
            return placed;
        }

        BlockPos base = BlockPos.containing(x, bottomY, z);
        int bx = base.getX();
        int bz = base.getZ();

        // Anchor the column to the ground beneath the lowest line, but never
        // above that line, so rows sitting near or below ground are still
        // covered. Then light every cell from there up through the top line,
        // extending further if the requested height asks for more.
        int groundY = findGroundY(level, bx, base.getY(), bz);
        int bottomCell = Math.min(groundY, (int) Math.floor(bottomY));
        int topCell = (int) Math.ceil(topY);
        int endCell = Math.max(topCell, bottomCell + clampLevel(height) - 1);

        for (int y = bottomCell; y <= endCell; y++) {
            BlockPos pos = new BlockPos(bx, y, bz);
            if (placeSingle(level, pos, LIGHT_EMISSION)) {
                placed.add(pos.immutable());
            }
        }
        return placed;
    }

    /**
     * Scans downward from {@code startY} to find the air/water cell resting on
     * the first solid surface, so the column's base sits on the ground beneath
     * the hologram. If {@code startY} is already inside solid terrain (or no
     * ground is found within {@link #MAX_GROUND_SCAN} blocks) the original
     * {@code startY} is returned.
     */
    private static int findGroundY(ServerLevel level, int x, int startY, int z) {
        int y = startY;
        for (int i = 0; i < MAX_GROUND_SCAN; i++) {
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            if (isReplaceable(below)) {
                y--;
            } else {
                return y;
            }
        }
        return startY;
    }

    /**
     * Removes light blocks at every position in {@code positions}, restoring
     * water where a light block was waterlogged.
     */
    public static void removeLights(ServerLevel level, List<BlockPos> positions) {
        if (level == null || positions == null) {
            return;
        }
        for (BlockPos pos : positions) {
            removeSingle(level, pos);
        }
    }

    /**
     * @return {@code true} if {@code state} is air, water, or an existing light
     * block - i.e. something the backlight may freely overwrite.
     */
    private static boolean isReplaceable(BlockState state) {
        return state.isAir()
                || state.getFluidState().getType() == Fluids.WATER
                || state.getBlock() == Blocks.LIGHT;
    }

    /**
     * Place a single invisible light block at {@code pos} with the given
     * emission level. Refuses to overwrite existing solid world content; will
     * overwrite air, water, or an existing light block.
     *
     * @return {@code true} if a light block is now present at {@code pos}.
     */
    private static boolean placeSingle(ServerLevel level, BlockPos pos, int emission) {
        BlockState existing = level.getBlockState(pos);
        Block existingBlock = existing.getBlock();

        // No-op (but still "ours") if a light at the exact level is already there
        if (existingBlock == Blocks.LIGHT
                && existing.hasProperty(LightBlock.LEVEL)
                && existing.getValue(LightBlock.LEVEL) == emission) {
            return true;
        }

        boolean isWater = existing.getFluidState().getType() == Fluids.WATER;

        if (!isReplaceable(existing)) {
            // Don't clobber player-built / world content; just skip this cell
            return false;
        }

        BlockState lightState = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, emission);
        if (lightState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            lightState = lightState.setValue(BlockStateProperties.WATERLOGGED, isWater);
        }

        return level.setBlock(pos, lightState, Block.UPDATE_ALL);
    }

    /**
     * Removes a single light block at {@code pos}, restoring water if the block
     * was waterlogged. If the block is not a light block this is a no-op.
     */
    private static boolean removeSingle(ServerLevel level, BlockPos pos) {
        if (pos == null) {
            return true;
        }

        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() != Blocks.LIGHT) {
            // Already not a light block, nothing to remove
            return true;
        }

        boolean waterlogged = existing.hasProperty(BlockStateProperties.WATERLOGGED)
                && existing.getValue(BlockStateProperties.WATERLOGGED);

        BlockState replacement = waterlogged
                ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState();

        return level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }
}
