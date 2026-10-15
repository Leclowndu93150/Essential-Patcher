package com.leclowndu93150.essentialpatcher;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.SessionKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.UUID;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class EssentialpatcherFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PatcherConfig config = PatcherConfig.get();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!PatcherConfig.get().unlockAllCosmetics) return;

            String[] key = SessionKey.compute();
            if (key != null) {
                CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CosmeticHttpSync.get().onServerLeave());

        CosmeticHttpSync.get().setLocalProfileProvider(new CosmeticHttpSync.LocalProfileProvider() {
            @Override
            public String username() {
                return Minecraft.getInstance().getUser().getName();
            }

            @Override
            public UUID uuid() {
                return Minecraft.getInstance().getUser().getProfileId();
            }
        });

        if (config.disableAutoUpdate) {
            disableEssentialAutoUpdate("26.3");
        }
    }

    static void disableEssentialAutoUpdate(String mcVersion) {
        try {
            Path essentialDir = Minecraft.getInstance().gameDirectory.toPath().resolve("essential");
            // stage2 file gates loader updates; essential-loader.properties gates Essential mod version updates
            Path[] configFiles = {
                    essentialDir.resolve("loader/stage1/fabric/stage2.fabric_" + mcVersion + ".properties"),
                    essentialDir.resolve("essential-loader.properties")
            };
            for (Path configFile : configFiles) {
                Properties props = new Properties();
                if (Files.exists(configFile)) {
                    try (var in = Files.newInputStream(configFile)) {
                        props.load(in);
                    }
                }
                props.setProperty("autoUpdate", "false");
                Files.createDirectories(configFile.getParent());
                // write then move, so a crash mid-write cannot wipe the loader's other keys
                Path tmp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
                try (OutputStream out = Files.newOutputStream(tmp)) {
                    props.store(out, null);
                }
                Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to disable auto-update: " + e.getMessage());
        }
    }
}
