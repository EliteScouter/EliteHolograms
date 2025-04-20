package com.strictgaming.elite.holograms.forge20.hologram;

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
            lines.add(line.getText());
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
    
            JsonArray lines = object.getAsJsonArray("lines");
            String[] textLines = new String[lines.size()];
    
            for (int i = 0; i < lines.size(); i++) {
                textLines[i] = lines.get(i).getAsString();
            }
    
            // Get the world from the name
            Level world = UtilWorld.findWorld(worldName);
            
            if (world == null) {
                System.out.println("[EliteHolograms] Could not find world: " + worldName);
                return null;
            }
            
            // Create the hologram directly
            return new ForgeHologram(id, world, new Vec3(x, y, z), range, false, textLines);
        } catch (Exception e) {
            System.out.println("[EliteHolograms] Error deserializing hologram: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 