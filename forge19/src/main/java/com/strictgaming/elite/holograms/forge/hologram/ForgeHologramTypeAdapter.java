package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.forge.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilWorld;
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

        // Save hologram type
        if (hologram instanceof ItemHologram) {
            object.addProperty("type", "item");
            object.addProperty("itemId", ((ItemHologram) hologram).getItemId());
        } else {
            object.addProperty("type", "standard");
        }

        // Save lines with animation data if present
        JsonArray lines = new JsonArray();
        for (HologramLine line : hologram.getLines()) {
            if (line instanceof AnimatedHologramLine) {
                AnimatedHologramLine animLine = (AnimatedHologramLine) line;
                JsonObject lineObj = new JsonObject();
                lineObj.addProperty("type", "animated");
                lineObj.addProperty("interval", animLine.getIntervalTicks() / 20); // Convert to seconds
                
                JsonArray frames = new JsonArray();
                for (String frame : animLine.getFrames()) {
                    frames.add(frame);
                }
                lineObj.add("frames", frames);
                lines.add(lineObj);
            } else {
                JsonObject lineObj = new JsonObject();
                lineObj.addProperty("type", "standard");
                lineObj.addProperty("text", line.getText());
                lines.add(lineObj);
            }
        }

        object.add("lines", lines);
        object.addProperty("range", hologram.getRange());
        return object;
    }

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
            
            // Check hologram type
            String hologramType = object.has("type") ? object.get("type").getAsString() : "standard";
            JsonArray lines = object.getAsJsonArray("lines");
            
            ForgeHologram hologram;
            
            // Create appropriate hologram type
            if ("item".equals(hologramType)) {
                String itemId = object.get("itemId").getAsString();
                String[] textLines = parseLinesArray(lines);
                hologram = new ItemHologram(id, world, new Vec3(x, y, z), range, itemId, textLines);
            } else {
                // Standard hologram - parse lines first to check for animations
                String[] textLines = parseLinesArray(lines);
                hologram = new ForgeHologram(id, world, new Vec3(x, y, z), range, false, textLines);
                
                // Now handle animated lines
                for (int i = 0; i < lines.size(); i++) {
                    JsonElement lineElement = lines.get(i);
                    if (lineElement.isJsonObject()) {
                        JsonObject lineObj = lineElement.getAsJsonObject();
                        if (lineObj.has("type") && "animated".equals(lineObj.get("type").getAsString())) {
                            int interval = lineObj.get("interval").getAsInt();
                            JsonArray frames = lineObj.getAsJsonArray("frames");
                            List<String> frameList = Lists.newArrayList();
                            for (JsonElement frame : frames) {
                                frameList.add(frame.getAsString());
                            }
                            hologram.setLineAnimated(i + 1, frameList, interval);
                        }
                    }
                }
            }
            
            return hologram;
        } catch (Exception e) {
            System.out.println("[EliteHolograms] Error deserializing hologram: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String[] parseLinesArray(JsonArray lines) {
        String[] textLines = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            JsonElement lineElement = lines.get(i);
            if (lineElement.isJsonPrimitive()) {
                // Old format - just a string
                textLines[i] = lineElement.getAsString();
            } else if (lineElement.isJsonObject()) {
                JsonObject lineObj = lineElement.getAsJsonObject();
                if (lineObj.has("text")) {
                    // Standard line with new format
                    textLines[i] = lineObj.get("text").getAsString();
                } else if (lineObj.has("frames")) {
                    // Animated line - use first frame as placeholder
                    JsonArray frames = lineObj.getAsJsonArray("frames");
                    textLines[i] = frames.size() > 0 ? frames.get(0).getAsString() : "";
                } else {
                    textLines[i] = "";
                }
            } else {
                textLines[i] = "";
            }
        }
        return textLines;
    }
} 
