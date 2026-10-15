package com.leclowndu93150.essentialpatcher.httpsync;

import gg.essential.sps.SpsAddress;
import gg.essential.util.USession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.util.Locale;
import java.util.UUID;

/**
 * Picks a session_id ingredient + label appropriate for the current connection.
 * - Essential-hosted world (guest): the host's UUID from the SPS address.
 * - Multiplayer: ip:port (lowercased).
 * - Published singleplayer (host): the host's own UUID, matching what guests compute.
 * - Singleplayer (not published): the world name.
 *
 * The CosmeticHttpSync hashes this so the actual server_id we send to the panel is opaque.
 */
public final class SessionKey {
    private static final String DEFAULT_PORT_SUFFIX = ":25565";

    private SessionKey() {
    }

    public static String[] compute() {
        Minecraft mc = Minecraft.getInstance();
        ServerData sd = mc.getCurrentServer();
        if (sd != null && sd.ip != null && !sd.ip.isBlank()) {
            String[] sps = fromEssentialSpsAddress(sd.ip);
            return sps != null ? sps : new String[] { normalizeAddress(sd.ip), "smp" };
        }
        IntegratedServer integ = mc.hasSingleplayerServer() ? mc.getSingleplayerServer() : null;
        if (integ != null) {
            // Essential 1.5+ hosts worlds by publishing the integrated server; guests key on the host UUID,
            // which Essential takes from its active session (can differ from mc.getUser() after an account switch).
            if (integ.isPublished()) {
                return essentialSpsKey(USession.Companion.activeNow().getUuid());
            }
            String world = integ.getWorldData() != null ? integ.getWorldData().getLevelName() : "world";
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
