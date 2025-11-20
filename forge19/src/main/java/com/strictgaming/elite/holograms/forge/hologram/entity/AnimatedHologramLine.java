package com.strictgaming.elite.holograms.forge.hologram.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.List;

/**
 * A hologram line that animates through multiple text frames
 */
public class AnimatedHologramLine extends HologramLine {
    
    private final List<String> frames;
    private final int intervalTicks;
    private int currentFrameIndex;
    private long lastUpdateTick;
    
    /**
     * Create an animated hologram line
     * 
     * @param armorStand The armor stand entity
     * @param frames List of text frames to cycle through
     * @param intervalTicks Ticks between frame changes (20 ticks = 1 second)
     */
    public AnimatedHologramLine(ArmorStand armorStand, List<String> frames, int intervalTicks) {
        super(armorStand);
        this.frames = new ArrayList<>(frames);
        this.intervalTicks = Math.max(20, intervalTicks); // Minimum 1 second
        this.currentFrameIndex = 0;
        this.lastUpdateTick = 0;
        
        // Set initial frame
        if (!frames.isEmpty()) {
            super.setText(frames.get(0));
        }
    }
    
    /**
     * Tick this animated line - updates frame if needed
     * @param currentTick Current server tick count
     * @return true if frame was updated
     */
    public boolean tick(long currentTick) {
        if (frames.size() <= 1) {
            return false; // No animation needed
        }
        
        if (currentTick - lastUpdateTick >= intervalTicks) {
            // Time to switch frames
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            super.setText(frames.get(currentFrameIndex));
            lastUpdateTick = currentTick;
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all frames
     */
    public List<String> getFrames() {
        return new ArrayList<>(frames);
    }
    
    /**
     * Get current frame index
     */
    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }
    
    /**
     * Get interval in ticks
     */
    public int getIntervalTicks() {
        return intervalTicks;
    }
    
    /**
     * Check if this line is animated
     */
    public boolean isAnimated() {
        return frames.size() > 1;
    }
    
    /**
     * Override setText to update only the current frame
     * (This maintains backwards compatibility if someone tries to set text directly)
     */
    @Override
    public void setText(String text) {
        if (!frames.isEmpty()) {
            frames.set(currentFrameIndex, text);
        }
        super.setText(text);
    }
    
    /**
     * Get current frame text
     */
    public String getCurrentFrameText() {
        if (frames.isEmpty()) {
            return "";
        }
        return frames.get(currentFrameIndex);
    }
}


