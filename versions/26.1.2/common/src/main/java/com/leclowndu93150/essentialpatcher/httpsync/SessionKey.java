package com.leclowndu93150.essentialpatcher.httpsync;

import gg.essential.Essential;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.sps.SpsAddress;
import gg.essential.upnp.model.UPnPSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.util.Locale;
import java.util.UUID;

public final class SessionKey {
    private static final String DEFAULT_PORT_SUFFIX = ":25565";

    private SessionKey() {
    }

    public static String[] compute() {
        Minecraft mc = Minecraft.getInstance();
        String[] sps = computeEssentialSps(mc);
        if (sps != null) {
            return sps;
        }

        ServerData sd = mc.getCurrentServer();
        if (sd != null && sd.ip != null && !sd.ip.isBlank()) {
            return new String[] { normalizeAddress(sd.ip), "smp" };
        }
        IntegratedServer integ = mc.hasSingleplayerServer() ? mc.getSingleplayerServer() : null;
        if (integ != null) {
            String world = integ.getWorldData() != null ? integ.getWorldData().getLevelName() : "world";
            if (integ.isPublished()) {
                String host = mc.getUser() != null && mc.getUser().getProfileId() != null
                        ? mc.getUser().getProfileId().toString()
                        : "host";
                return new String[] { host + "|" + world, "lan" };
            }
            return new String[] { world, "sp" };
        }
        return null;
    }

    public static String[] fromEssentialSpsAddress(String address) {
        SpsAddress spsAddress = SpsAddress.parse(address);
        if (spsAddress == null) {
            return null;
        }
        return essentialSpsKey(spsAddress.getHost());
    }

    private static String[] computeEssentialSps(Minecraft mc) {
        try {
            SPSManager spsManager = Essential.getInstance().getConnectionManager().getSpsManager();
            UPnPSession localSession = spsManager.getLocalSession();
            if (localSession != null) {
                return essentialSpsKey(localSession.getHostUUID());
            }

            ServerData serverData = mc.getCurrentServer();
            if (serverData != null && serverData.ip != null) {
                return fromEssentialSpsAddress(serverData.ip);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalizeAddress(String address) {
        String normalized = address.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(DEFAULT_PORT_SUFFIX)) {
            normalized = normalized.substring(0, normalized.length() - DEFAULT_PORT_SUFFIX.length());
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String[] essentialSpsKey(UUID hostUuid) {
        return new String[] { "sps|" + hostUuid, "sps" };
    }
}
