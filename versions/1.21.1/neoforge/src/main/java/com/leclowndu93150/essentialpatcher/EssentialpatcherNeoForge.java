package com.leclowndu93150.essentialpatcher;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.config.PatcherConfigScreen;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.EssentialSpsSyncListener;
import com.leclowndu93150.essentialpatcher.httpsync.SessionKey;
import com.leclowndu93150.essentialpatcher.network.CosmeticSyncData;
import com.leclowndu93150.essentialpatcher.network.CosmeticSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

@Mod("essentialpatcher")
public class EssentialpatcherNeoForge {

    public EssentialpatcherNeoForge(IEventBus modBus) {
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
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (container, parent) -> PatcherConfigScreen.create(parent));
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> CosmeticHttpSync.get().onServerLeave());

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

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("essentialpatcher").optional();
        registrar.playBidirectional(
                CosmeticSyncPayload.TYPE,
                CosmeticSyncPayload.CODEC,
                (payload, context) -> {
                    if (context.flow().isClientbound()) {
                        context.enqueueWork(() -> CosmeticSyncData.applyRemoteCosmetics(payload.playerUuid(), payload.equippedCosmetics()));
                    } else {
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer sender) {
                                var server = sender.getServer();
                                if (server == null) return;
                                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                    if (!player.getUUID().equals(payload.playerUuid())) {
                                        PacketDistributor.sendToPlayer(player, payload);
                                    }
                                }
                            }
                        });
                    }
                }
        );
    }

    private void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!PatcherConfig.get().unlockAllCosmetics) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        try {
            var cosmetics = CosmeticSyncData.getLocalEquipped();
            if (!cosmetics.isEmpty()) {
                PacketDistributor.sendToServer(new CosmeticSyncPayload(player.getUUID(), cosmetics));
            }
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to send cosmetic sync: " + e.getMessage());
        }

        String[] key = SessionKey.compute();
        if (key != null) {
            CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
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
