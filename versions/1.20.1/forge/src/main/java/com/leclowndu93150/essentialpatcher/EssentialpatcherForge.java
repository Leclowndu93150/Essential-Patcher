package com.leclowndu93150.essentialpatcher;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.config.PatcherConfigScreen;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.EssentialSpsSyncListener;
import com.leclowndu93150.essentialpatcher.httpsync.SessionKey;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

@Mod("essentialpatcher")
public class EssentialpatcherForge {

    public EssentialpatcherForge() {
        CompatibilityTracker.setPlatformVersionProvider(() -> {
            try {
                return ModList.get()
                        .getModContainerById("essential")
                        .map(c -> c.getModInfo().getVersion().toString())
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
        PatcherConfig config = PatcherConfig.get();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> PatcherConfigScreen.create(parent)));
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);

        CosmeticHttpSync.get().setMojangJoiner(new CosmeticHttpSync.MojangJoiner() {
            @Override
            public void joinServer(String serverId) throws Exception {
                Minecraft mc = Minecraft.getInstance();
                mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), serverId);
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
            disableEssentialAutoUpdate("1.20.1");
        }
    }

    private void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!PatcherConfig.get().unlockAllCosmetics) return;

        String[] key = SessionKey.compute();
        if (key != null) {
            CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
        }
    }

    private void onPlayerLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        CosmeticHttpSync.get().onServerLeave();
    }

    static void disableEssentialAutoUpdate(String mcVersion) {
        try {
            Path essentialDir = Minecraft.getInstance().gameDirectory.toPath().resolve("essential");
            Path configFile = essentialDir.resolve("stage2." + mcVersion + ".properties");
            Properties props = new Properties();
            if (Files.exists(configFile)) {
                try (InputStream in = Files.newInputStream(configFile)) {
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
