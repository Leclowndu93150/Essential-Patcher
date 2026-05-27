package com.leclowndu93150.essentialpatcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatcherConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PatcherConfig INSTANCE;

    public boolean unlockAllCosmetics = true;
    public boolean showAllFree = true;
    public boolean disableNewCosmeticDots = true;

    public boolean disableTelemetry = true;
    public boolean disableFPSTelemetry = true;
    public boolean disableImpressionTracking = true;

    public boolean disableAllNotices = false;
    public boolean disableSaleNotices = true;
    public boolean disableWardrobeBanners = true;
    public boolean disablePromoToasts = true;
    public boolean disableGiftedCosmeticToasts = false;

    public boolean disableCoinPurchases = true;
    public boolean disableCheckoutPackets = true;
    public boolean disablePurchaseFlows = true;
    public boolean disableCoinPurchaseModal = true;

    public boolean disableWardrobeNagModals = true;
    public boolean skipTosPopup = true;
    public boolean disableWikiToast = true;
    public boolean skipCommunityRules = true;
    public boolean hideAnnouncementChannel = true;
    public boolean disableAutoUpdate = true;

    public boolean httpCosmeticSync = true;
    public String httpCosmeticSyncBaseUrl = "https://cosmetics.leclowndu93150.dev";

    public static PatcherConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("essentialpatcher.json");
    }

    private static PatcherConfig load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                PatcherConfig config = GSON.fromJson(json, PatcherConfig.class);
                if (config != null) {
                    config.save();
                    return config;
                }
            } catch (Exception e) {
                System.err.println("[EssentialPatcher] Failed to load config, using defaults: " + e.getMessage());
            }
        }
        PatcherConfig config = new PatcherConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[EssentialPatcher] Failed to save config: " + e.getMessage());
        }
    }
}
