package com.leclowndu93150.essentialpatcher;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.EssentialSpsSyncListener;
import com.leclowndu93150.essentialpatcher.httpsync.SessionKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.UUID;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class EssentialpatcherFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CompatibilityTracker.setPlatformVersionProvider(() -> {
            try {
                return FabricLoader.getInstance()
                        .getModContainer("essential")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
        PatcherConfig config = PatcherConfig.get();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!PatcherConfig.get().unlockAllCosmetics) return;

            String[] key = SessionKey.compute();
            if (key != null) {
                CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CosmeticHttpSync.get().onServerLeave());

        CosmeticHttpSync.get().setMojangJoiner(new CosmeticHttpSync.MojangJoiner() {
            @Override
            public void joinServer(String serverId) throws Exception {
                Minecraft mc = Minecraft.getInstance();
                mc.getMinecraftSessionService().joinServer(mc.getUser().getProfileId(), mc.getUser().getAccessToken(), serverId);
            }

            @Override
            public String username() {
                return Minecraft.getInstance().getUser().getName();
            }

            @Override
            public UUID uuid() {
                return Minecraft.getInstance().getUser().getProfileId();
            }
        });
        EssentialSpsSyncListener.register();

        if (config.disableAutoUpdate) {
            disableEssentialAutoUpdate("1.21.1");
        }
    }

    static void disableEssentialAutoUpdate(String mcVersion) {
        try {
            Path essentialDir = Minecraft.getInstance().gameDirectory.toPath().resolve("essential");
            Path configFile = essentialDir.resolve("stage2." + mcVersion + ".properties");
            Properties props = new Properties();
            if (Files.exists(configFile)) {
                try (var in = Files.newInputStream(configFile)) {
                    props.load(in);
                }
            }
            props.setProperty("autoUpdate", "false");
            Files.createDirectories(essentialDir);
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, null);
            }
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to disable auto-update: " + e.getMessage());
        }
    }
}
