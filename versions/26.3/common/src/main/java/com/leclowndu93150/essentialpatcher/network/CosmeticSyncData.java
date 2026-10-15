package com.leclowndu93150.essentialpatcher.network;

import com.leclowndu93150.essentialpatcher.cosmetics.CosmeticSaver;
import com.leclowndu93150.essentialpatcher.httpsync.SyncedCosmeticOutfit;
import gg.essential.Essential;
import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticSetting;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager;
import gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager;
import kotlinx.serialization.json.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CosmeticSyncData {

    public static Map<String, String> getLocalEquipped() {
        return CosmeticSaver.loadEquippedCosmetics();
    }

    public static SyncedCosmeticOutfit getLocalOutfit() {
        return CosmeticSaver.loadOutfit();
    }

    public static SyncedCosmeticOutfit getLiveLocalOutfit() {
        try {
            CosmeticsManager cosmeticsManager = Essential.getInstance().getConnectionManager().getCosmeticsManager();
            InfraEquippedOutfitsManager infraManager = cosmeticsManager.getInfraEquippedOutfitsManager();
            return fromEquippedOutfit(infraManager.getEquippedCosmetics());
        } catch (Exception e) {
            return SyncedCosmeticOutfit.empty();
        }
    }

    public static void applyRemoteCosmetics(UUID playerUuid, Map<String, String> cosmetics) {
        applyRemoteOutfit(playerUuid, new SyncedCosmeticOutfit(cosmetics, Collections.emptyMap()));
    }

    public static void applyRemoteOutfit(UUID playerUuid, SyncedCosmeticOutfit outfit) {
        try {
            CosmeticsManager cosmeticsManager = Essential.getInstance().getConnectionManager().getCosmeticsManager();
            InfraEquippedOutfitsManager infraManager = cosmeticsManager.getInfraEquippedOutfitsManager();

            infraManager.update(playerUuid, new InfraEquippedOutfitsManager.InfraOutfit(
                    toInfraCosmetics(outfit.equipped()),
                    toInfraSettings(outfit.settings()),
                    null
            ));
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to apply remote cosmetics for " + playerUuid + ": " + e.getMessage());
        }
    }

    public static SyncedCosmeticOutfit fromEquippedOutfit(EquippedOutfitsManager.Outfit outfit) {
        Map<String, String> equipped = new HashMap<>();
        Map<String, List<String>> settings = new HashMap<>();
        for (var entry : outfit.getCosmetics().entrySet()) {
            EquippedCosmeticId equippedCosmetic = entry.getValue();
            if (equippedCosmetic == null) continue;
            equipped.put(entry.getKey().getId(), equippedCosmetic.getId());
            settings.put(equippedCosmetic.getId(), serializeSettings(equippedCosmetic.getSettings()));
        }
        return new SyncedCosmeticOutfit(equipped, settings);
    }

    public static SyncedCosmeticOutfit fromInfraOutfit(InfraEquippedOutfitsManager.InfraOutfit outfit) {
        Map<String, String> equipped = new HashMap<>();
        Map<String, List<String>> settings = new HashMap<>();
        for (var entry : outfit.getCosmetics().entrySet()) {
            equipped.put(entry.getKey().getId(), entry.getValue());
        }
        for (var entry : outfit.getSettings().entrySet()) {
            settings.put(entry.getKey(), serializeSettings(entry.getValue()));
        }
        return new SyncedCosmeticOutfit(equipped, settings);
    }

    public static Map<CosmeticSlot, String> toInfraCosmetics(Map<String, String> cosmetics) {
        Map<CosmeticSlot, String> cosmeticMap = new HashMap<>();
        for (var entry : cosmetics.entrySet()) {
            CosmeticSlot slot = slotById(entry.getKey());
            if (slot != null) {
                cosmeticMap.put(slot, entry.getValue());
            }
        }
        return cosmeticMap;
    }

    public static Map<String, List<CosmeticSetting>> toInfraSettings(Map<String, List<String>> settings) {
        Map<String, List<CosmeticSetting>> decoded = new HashMap<>();
        for (var entry : settings.entrySet()) {
            if (entry.getValue() == null) continue;
            List<CosmeticSetting> cosmeticSettings = new ArrayList<>();
            for (String json : entry.getValue()) {
                try {
                    cosmeticSettings.add(CosmeticSetting.Companion.fromJson(json));
                } catch (Exception ignored) {
                }
            }
            if (!cosmeticSettings.isEmpty()) {
                decoded.put(entry.getKey(), cosmeticSettings);
            }
        }
        return decoded;
    }

    private static List<String> serializeSettings(List<CosmeticSetting> settings) {
        if (settings == null || settings.isEmpty()) {
            return Collections.emptyList();
        }
        Json serializer = CosmeticSetting.Companion.getJson();
        List<String> encoded = new ArrayList<>();
        for (CosmeticSetting setting : settings) {
            encoded.add(serializer.encodeToString(CosmeticSetting.Companion.serializer(), setting));
        }
        return encoded;
    }

    private static CosmeticSlot slotById(String id) {
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            if (slot.getId().equals(id)) {
                return slot;
            }
        }
        return null;
    }
}
