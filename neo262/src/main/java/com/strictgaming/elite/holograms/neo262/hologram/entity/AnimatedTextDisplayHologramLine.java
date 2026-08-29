package com.strictgaming.elite.holograms.neo262.hologram.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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

    public AnimatedTextDisplayHologramLine(ServerLevel level, double x, double y, double z,
                                           float yaw, float pitch, List<String> frames, int intervalTicks) {
        super(level, x, y, z, yaw, pitch, frames.isEmpty() ? "" : frames.get(0));
        this.frames = frames;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public boolean isAnimated() {
        return true;
    }

    @Override
    public boolean tickAnimation() {
        if (this.frames.size() < 2) {
            return false;
        }

        this.ticksSinceLastUpdate++;

        if (this.ticksSinceLastUpdate < this.intervalTicks) {
            return false;
        }

        this.ticksSinceLastUpdate = 0;
        this.currentFrameIndex = (this.currentFrameIndex + 1) % this.frames.size();
        this.rawText = this.frames.get(this.currentFrameIndex);
        this.display.setHologramText(renderText(null));
        return true;
    }

    @Override
    public void updateForPlayer(ServerPlayer player, boolean isInitialSpawn) {
        if (this.frames.isEmpty()) {
            return;
        }

        // Keep the inherited text in step with the frame the animation is on.
        this.rawText = this.frames.get(this.currentFrameIndex);
        super.updateForPlayer(player, isInitialSpawn);
    }

    public List<String> getFrames() {
        return this.frames;
    }

    public int getIntervalTicks() {
        return this.intervalTicks;
    }
}
