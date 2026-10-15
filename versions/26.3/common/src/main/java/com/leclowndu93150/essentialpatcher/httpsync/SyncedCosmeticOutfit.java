package com.leclowndu93150.essentialpatcher.httpsync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncedCosmeticOutfit {

    private final Map<String, String> equipped;
    private final Map<String, List<String>> settings;

    public SyncedCosmeticOutfit(Map<String, String> equipped, Map<String, List<String>> settings) {
        this.equipped = copyEquipped(equipped);
        this.settings = copySettings(settings);
    }

    public static SyncedCosmeticOutfit empty() {
        return new SyncedCosmeticOutfit(Collections.emptyMap(), Collections.emptyMap());
    }

    public Map<String, String> equipped() {
        return copyEquipped(equipped);
    }

    public Map<String, List<String>> settings() {
        return copySettings(settings);
    }

    public boolean isEmpty() {
        return equipped.isEmpty();
    }

    private static Map<String, String> copyEquipped(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }

    private static Map<String, List<String>> copySettings(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return copy;
        }
        for (var entry : source.entrySet()) {
            List<String> value = entry.getValue();
            copy.put(entry.getKey(), value != null ? new ArrayList<>(value) : new ArrayList<>());
        }
        return copy;
    }
}
