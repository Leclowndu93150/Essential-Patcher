package com.leclowndu93150.essentialpatcher.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.leclowndu93150.essentialpatcher.httpsync.SyncedCosmeticOutfit;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmeticSaver {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Type SETTINGS_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private static Path savePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("essentialpatcher-equipped.json");
    }

    public static void saveEquippedCosmetics(Map<String, String> slotToCosmetic) {
        saveOutfit(new SyncedCosmeticOutfit(slotToCosmetic, Map.of()));
    }

    public static void saveOutfit(SyncedCosmeticOutfit outfit) {
        try {
            Path path = savePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(outfit));
        } catch (IOException e) {
            System.err.println("[EssentialPatcher] Failed to save equipped cosmetics: " + e.getMessage());
        }
    }

    public static Map<String, String> loadEquippedCosmetics() {
        return loadOutfit().equipped();
    }

    public static SyncedCosmeticOutfit loadOutfit() {
        Path path = savePath();
        if (!Files.exists(path)) {
            return SyncedCosmeticOutfit.empty();
        }
        try {
            String json = Files.readString(path);
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                return SyncedCosmeticOutfit.empty();
            }
            JsonObject object = root.getAsJsonObject();
            if (object.has("equipped") && object.get("equipped").isJsonObject()) {
                Map<String, String> equipped = GSON.fromJson(object.get("equipped"), MAP_TYPE);
                Map<String, List<String>> settings = object.has("settings") && object.get("settings").isJsonObject()
                        ? GSON.fromJson(object.get("settings"), SETTINGS_TYPE)
                        : new HashMap<>();
                return new SyncedCosmeticOutfit(equipped, settings);
            }
            Map<String, String> legacy = GSON.fromJson(object, MAP_TYPE);
            return new SyncedCosmeticOutfit(legacy, Map.of());
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to load equipped cosmetics: " + e.getMessage());
            return SyncedCosmeticOutfit.empty();
        }
    }
}
