package com.strictgaming.elite.holograms.forge20.hologram.entity;

import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge20.util.UtilPlaceholder;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import java.util.List;

/**
 * A hologram line that rotates through multiple text frames
 */
public class AnimatedHologramLine extends HologramLine {

    private transient final List<String> frames;
    private transient final int intervalTicks;
    private transient int currentFrameIndex = 0;
    private transient int ticksSinceLastUpdate = 0;

    public AnimatedHologramLine(ArmorStand armorStand, List<String> frames, int intervalTicks) {
        super(armorStand);
        this.frames = frames;
        this.intervalTicks = intervalTicks;
        
        // Set initial text
        if (!frames.isEmpty()) {
            this.setText(frames.get(0));
        }
    }

    /**
     * Update the animation state
     * @return true if the frame changed
     */
    public boolean tick(long currentTick) {
        if (frames.isEmpty() || frames.size() < 2) {
            return false;
        }

        ticksSinceLastUpdate++;
        
        if (ticksSinceLastUpdate >= intervalTicks) {
            ticksSinceLastUpdate = 0;
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            
            // Use the raw text setter from parent to update the armor stand's name internally
            // This ensures that if updateForPlayer is called later (e.g. new player), it sees the correct text
            this.setText(frames.get(currentFrameIndex));
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public void updateForPlayer(ServerPlayer player) {
        if (frames.isEmpty()) {
            return;
        }
        
        ArmorStand armorStand = getArmorStand();
        if (armorStand == null) return;

        // Get current frame text
        String currentText = frames.get(currentFrameIndex);
        
        // Update parent text so it matches current frame
        // This is important so that super.updateForPlayer or other logic uses the correct base text
        this.setText(currentText);
        
        // Delegate to parent update logic which handles placeholders, packet sending, etc.
        // properly using the text we just set
        super.updateForPlayer(player);
    }
    
    public List<String> getFrames() {
        return frames;
    }
    
    public int getIntervalTicks() {
        return intervalTicks;
    }
}

