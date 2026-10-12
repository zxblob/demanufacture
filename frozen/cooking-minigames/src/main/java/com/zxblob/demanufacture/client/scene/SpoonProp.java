package com.zxblob.demanufacture.client.scene;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Reads the animation of the spoon (the extra `spoom` bone of the pot rig) from
 * assets/demanufacture/cook_props/fd_cooking_pot.json and evaluates it at a phase in turns.
 * Returns the rotation about the vertical axis in degrees.
 */
public final class SpoonProp
{
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID,
            "cook_props/fd_cooking_pot.json");
    private static final String ANIMATION = "fd_cooking_pot.work";
    private static final String BONE = "spoom";

    private record Key(double time, float yaw)
    {
    }

    private static List<Key> keys;
    private static double length = 1.5;
    private static boolean loaded;

    private SpoonProp()
    {
    }

    private static void load()
    {
        loaded = true;
        keys = new ArrayList<>();
        try
        {
            var resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
            if (resource.isEmpty())
                return;
            try (Reader reader = resource.get().openAsReader())
            {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject anim = root.getAsJsonObject("animations").getAsJsonObject(ANIMATION);
                length = anim.get("animation_length").getAsDouble();
                JsonObject rotation = anim.getAsJsonObject("bones").getAsJsonObject(BONE).getAsJsonObject("rotation");
                for (Map.Entry<String, JsonElement> e : rotation.entrySet())
                {
                    JsonElement value = e.getValue();
                    var vector = value.isJsonObject() ? value.getAsJsonObject().getAsJsonArray("vector") : value.getAsJsonArray();
                    keys.add(new Key(Double.parseDouble(e.getKey()), vector.get(1).getAsFloat()));
                }
                keys.sort(Comparator.comparingDouble(Key::time));
            }
        }
        catch (Exception ignored)
        {
            keys = new ArrayList<>();
        }
    }

    /** Call after a resource reload so a re-exported file is picked up. */
    public static void invalidate()
    {
        loaded = false;
    }

    public static boolean available()
    {
        if (!loaded)
            load();
        return !keys.isEmpty();
    }

    /** @return degrees about Y (as exported, i.e. the sign Player Animator and the pot rig use) */
    public static float yawAt(double turns)
    {
        if (!available())
            return 0;
        double t = (turns - Math.floor(turns)) * length;
        Key before = keys.get(0);
        Key after = keys.get(keys.size() - 1);
        for (int i = 0; i < keys.size(); i++)
        {
            if (keys.get(i).time() <= t)
                before = keys.get(i);
            if (keys.get(i).time() >= t)
            {
                after = keys.get(i);
                break;
            }
        }
        if (after.time() == before.time())
            return before.yaw();
        double f = (t - before.time()) / (after.time() - before.time());
        return (float) (before.yaw() + (after.yaw() - before.yaw()) * f);
    }
}
