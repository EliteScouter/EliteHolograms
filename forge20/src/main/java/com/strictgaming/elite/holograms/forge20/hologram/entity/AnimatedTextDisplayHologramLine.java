package com.strictgaming.elite.holograms.forge20.hologram.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A fixed-rotation hologram line that cycles through frames, the {@code text_display}
 * counterpart of {@link AnimatedHologramLine}.
 */
public class AnimatedTextDisplayHologramLine extends TextDisplayHologramLine {

    private final List<String> frames;
    private final int intervalTicks;
    private int currentFrameIndex = 0;
    private int ticksSinceLastUpdate = 0;

    public AnimatedTextDisplayHologramLine(Level world, double x, double y, double z,
                                           float yaw, float pitch, List<String> frames, int intervalTicks) {
        this(world, x, y, z, yaw, pitch, frames, intervalTicks, HologramTextDisplay.DEFAULT_BACKGROUND);
    }

    public AnimatedTextDisplayHologramLine(Level world, double x, double y, double z,
                                           float yaw, float pitch, List<String> frames, int intervalTicks,
                                           int backgroundArgb) {
        super(world, x, y, z, yaw, pitch, backgroundArgb);
        this.frames = frames;
        this.intervalTicks = intervalTicks;

        if (!frames.isEmpty()) {
            setText(frames.get(0));
        }
    }

    @Override
    public boolean isAnimated() {
        return true;
    }

    @Override
    public boolean tickAnimation(long currentTick) {
        if (this.frames.size() < 2) {
            return false;
        }

        this.ticksSinceLastUpdate++;

        if (this.ticksSinceLastUpdate < this.intervalTicks) {
            return false;
        }

        this.ticksSinceLastUpdate = 0;
        this.currentFrameIndex = (this.currentFrameIndex + 1) % this.frames.size();
        setText(this.frames.get(this.currentFrameIndex));
        return true;
    }

    @Override
    public void updateForPlayer(ServerPlayer player) {
        if (this.frames.isEmpty()) {
            return;
        }

        // Keep the inherited text in step with the frame the animation is on.
        setText(this.frames.get(this.currentFrameIndex));
        super.updateForPlayer(player);
    }

    @Override
    public List<String> getFrames() {
        return this.frames;
    }

    @Override
    public int getIntervalTicks() {
        return this.intervalTicks;
    }
}
