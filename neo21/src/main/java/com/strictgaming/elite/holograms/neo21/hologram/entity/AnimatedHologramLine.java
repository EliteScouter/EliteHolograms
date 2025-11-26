package com.strictgaming.elite.holograms.neo21.hologram.entity;

import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPlaceholder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import java.util.List;

public class AnimatedHologramLine extends HologramLine {

    private final List<String> frames;
    private final int intervalTicks;
    private int currentFrameIndex = 0;
    private int ticksSinceLastUpdate = 0;

    public AnimatedHologramLine(ArmorStand armorStand, List<String> frames, int intervalTicks) {
        super(armorStand, frames.isEmpty() ? "" : frames.get(0));
        this.frames = frames;
        this.intervalTicks = intervalTicks;
    }

    /**
     * Ticks the animation
     * @return true if the frame changed
     */
    public boolean tick() {
        if (frames.isEmpty() || frames.size() < 2) {
            return false;
        }

        ticksSinceLastUpdate++;
        
        if (ticksSinceLastUpdate >= intervalTicks) {
            ticksSinceLastUpdate = 0;
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            
            // Update the raw text of the parent so it holds the current frame
            this.rawText = frames.get(currentFrameIndex);
            
            // We also update the armorstand's custom name locally
            this.armorStand.setCustomName(UtilChatColour.parse(UtilPlaceholder.replacePlaceholders(this.rawText, null)));
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public void updateForPlayer(ServerPlayer player, boolean isInitialSpawn) {
        if (frames.isEmpty()) return;
        
        // Ensure rawText matches current frame
        this.rawText = frames.get(currentFrameIndex);
        
        // Delegate to parent which handles placeholders and packets
        super.updateForPlayer(player, isInitialSpawn);
    }
    
    public List<String> getFrames() {
        return frames;
    }
    
    public int getIntervalTicks() {
        return intervalTicks;
    }
}

