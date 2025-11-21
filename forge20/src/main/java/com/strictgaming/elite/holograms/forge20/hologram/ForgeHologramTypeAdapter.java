package com.strictgaming.elite.holograms.forge20.hologram;

import com.strictgaming.elite.holograms.forge20.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.forge20.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge20.util.UtilWorld;
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

    @Override
    public JsonElement serialize(ForgeHologram hologram, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("id", hologram.getId());
        object.add("loc", this.getLocationObject(hologram));

        JsonArray lines = new JsonArray();

        for (HologramLine line : hologram.getLines()) {
            if (line instanceof AnimatedHologramLine) {
                AnimatedHologramLine animated = (AnimatedHologramLine) line;
                JsonObject animObj = new JsonObject();
                animObj.addProperty("type", "animated");
                animObj.addProperty("interval", animated.getIntervalTicks() / 20); // Convert ticks to seconds
                
                JsonArray frames = new JsonArray();
                for (String frame : animated.getFrames()) {
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
            
            String hologramType = object.has("type") ? object.get("type").getAsString() : "basic";
            ForgeHologram hologram;

            if ("item".equalsIgnoreCase(hologramType) && object.has("itemId")) {
                String itemId = object.get("itemId").getAsString();
                hologram = new ItemHologram(id, world, new Vec3(x, y, z), range, itemId);
            } else {
                // Create the hologram base first (without lines)
                hologram = new ForgeHologram(id, world, new Vec3(x, y, z), range, false);
            }
            
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