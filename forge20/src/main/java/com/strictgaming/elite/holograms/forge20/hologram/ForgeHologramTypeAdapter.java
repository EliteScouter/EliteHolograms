package com.strictgaming.elite.holograms.forge20.hologram;

import com.strictgaming.elite.holograms.forge20.hologram.entity.HologramLineRenderer;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.strictgaming.elite.holograms.forge20.util.UtilBacklight;
import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Type adapter for serializing and deserializing ForgeHologram objects
 */
public class ForgeHologramTypeAdapter implements JsonSerializer<ForgeHologram>, JsonDeserializer<ForgeHologram> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    @Override
    public JsonElement serialize(ForgeHologram hologram, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("id", hologram.getId());
        object.add("loc", this.getLocationObject(hologram));

        JsonArray lines = new JsonArray();

        for (HologramLineRenderer line : hologram.getLines()) {
            // Asking the line for its frames rather than type-checking one implementation keeps
            // animated lines working on both display types - a fixed hologram's animated lines
            // are a different class but serialise identically.
            List<String> lineFrames = line.getFrames();

            if (lineFrames != null) {
                JsonObject animObj = new JsonObject();
                animObj.addProperty("type", "animated");
                animObj.addProperty("interval", line.getIntervalTicks() / 20); // Convert ticks to seconds
                
                JsonArray frames = new JsonArray();
                for (String frame : lineFrames) {
                    frames.add(frame);
                }
                animObj.add("frames", frames);
                lines.add(animObj);
            } else {
                lines.add(line.getText());
            }
        }

        object.add("lines", lines);
        object.addProperty("range", hologram.getRange());

        // Backlight state
        object.addProperty("backlightEnabled", hologram.isBacklightEnabled());
        object.addProperty("backlightLevel", hologram.getBacklightLevel());

        // How the lines render, and the orientation used when fixed
        object.addProperty("displayType", hologram.getDisplayType().getSerializedName());
        object.addProperty("yaw", hologram.getYaw());
        object.addProperty("pitch", hologram.getPitch());

        // Background behind a fixed hologram's text. Written as hex so the file stays
        // readable and hand-editable.
        object.addProperty("backgroundColour", String.format("#%06X", hologram.getBackgroundColour()));
        object.addProperty("backgroundOpacity", hologram.getBackgroundOpacity());

        // Include hologram type metadata for specialized holograms
        if (hologram instanceof ItemHologram) {
            object.addProperty("type", "item");
            object.addProperty("itemId", ((ItemHologram) hologram).getItemId());
        } else {
            object.addProperty("type", "basic");
        }
        return object;
    }
    
    // ... getLocationObject ...
    private JsonObject getLocationObject(ForgeHologram hologram) {
        JsonObject object = new JsonObject();

        object.addProperty("x", hologram.getPosition().x);
        object.addProperty("y", hologram.getPosition().y);
        object.addProperty("z", hologram.getPosition().z);
        object.addProperty("world", UtilWorld.getName(hologram.getWorld()));

        return object;
    }

    @Override
    public ForgeHologram deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject object = element.getAsJsonObject();
            String id = object.get("id").getAsString();
    
            JsonObject loc = object.getAsJsonObject("loc");
    
            String worldName = loc.get("world").getAsString();
            double x = loc.get("x").getAsDouble();
            double y = loc.get("y").getAsDouble();
            double z = loc.get("z").getAsDouble();
            int range = 64;
    
            if (object.has("range")) {
                range = object.get("range").getAsInt();
    
                if (range == 0) {
                    range = 64;
                }
            }
    
            // Get the world from the name
            Level world = UtilWorld.findWorld(worldName);
            
            if (world == null) {
                System.out.println("[EliteHolograms] Could not find world: " + worldName);
                return null;
            }
            
            // Absent for holograms saved before fixed holograms existed, which read back as
            // player-facing. Resolved up front so the lines below are built as the right
            // entity type.
            HologramDisplayType displayType = HologramDisplayType.fromStringOrDefault(
                    object.has("displayType") ? object.get("displayType").getAsString() : null,
                    HologramDisplayType.FACING);
            float yaw = object.has("yaw") ? object.get("yaw").getAsFloat() : 0.0F;
            float pitch = object.has("pitch") ? object.get("pitch").getAsFloat() : 0.0F;

            // Absent for holograms saved before backgrounds were configurable, which read
            // back as vanilla's 25% black.
            int backgroundColour = ForgeHologram.DEFAULT_BACKGROUND_COLOUR;
            if (object.has("backgroundColour")) {
                try {
                    backgroundColour = Integer.parseInt(
                            object.get("backgroundColour").getAsString().replace("#", "").trim(), 16);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid backgroundColour for hologram {}, using the default", id);
                }
            }
            int backgroundOpacity = object.has("backgroundOpacity")
                    ? object.get("backgroundOpacity").getAsInt()
                    : ForgeHologram.DEFAULT_BACKGROUND_OPACITY;

            String hologramType = object.has("type") ? object.get("type").getAsString() : "basic";
            ForgeHologram hologram;

            if ("item".equalsIgnoreCase(hologramType) && object.has("itemId")) {
                String itemId = object.get("itemId").getAsString();
                hologram = new ItemHologram(id, world, new Vec3(x, y, z), range, itemId,
                        displayType, yaw, pitch);
            } else {
                // Create the hologram base first (without lines)
                hologram = new ForgeHologram(id, world, new Vec3(x, y, z), range, false,
                        displayType, yaw, pitch);
            }

            hologram.restoreBackgroundState(backgroundColour, backgroundOpacity);

            // Restore backlight state (does not place block yet - spawn() will do that)
            boolean backlightEnabled = object.has("backlightEnabled") && object.get("backlightEnabled").getAsBoolean();
            int backlightLevel = object.has("backlightLevel") ? object.get("backlightLevel").getAsInt() : UtilBacklight.DEFAULT_LEVEL;
            hologram.restoreBacklightState(backlightEnabled, backlightLevel);
            
            // Process lines manually to handle animations
            JsonArray lines = object.getAsJsonArray("lines");
            
            for (int i = 0; i < lines.size(); i++) {
                JsonElement lineElement = lines.get(i);
                
                if (lineElement.isJsonObject()) {
                    // It's likely an animated line or complex object
                    JsonObject lineObj = lineElement.getAsJsonObject();
                    if (lineObj.has("type") && lineObj.get("type").getAsString().equals("animated")) {
                        int interval = lineObj.get("interval").getAsInt();
                        JsonArray framesArray = lineObj.getAsJsonArray("frames");
                        List<String> frames = Lists.newArrayList();
                        for (JsonElement frame : framesArray) {
                            frames.add(frame.getAsString());
                        }
                        hologram.addAnimatedLine(frames, interval);
                    } else {
                        // Fallback if type unknown
                         hologram.addLine("Error: Unknown line type");
                    }
                } else {
                    // Simple string line
                        hologram.addLine(lineElement.getAsString());
                }
            }
            
            return hologram;
        } catch (Exception e) {
            System.out.println("[EliteHolograms] Error deserializing hologram: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 