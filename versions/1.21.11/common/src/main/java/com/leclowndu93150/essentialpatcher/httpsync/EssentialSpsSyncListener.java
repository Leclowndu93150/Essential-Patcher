package com.leclowndu93150.essentialpatcher.httpsync;

import net.minecraft.client.Minecraft;

import java.util.UUID;

public final class EssentialSpsSyncListener {

    private static final boolean LEGACY_SPS = classExists("gg.essential.network.connectionmanager.sps.SPSManager");

    private static boolean registered;

    private EssentialSpsSyncListener() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        if (!LEGACY_SPS) {
            Minecraft.getInstance().execute(ModernSps::listenForHosting);
            return;
        }
        try {
            LegacySps.listenForHosting();
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to register SPS sync listener: " + e.getMessage());
        }
    }

    static UUID localHost() {
        return LEGACY_SPS ? LegacySps.localHost() : ModernSps.localHost();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, EssentialSpsSyncListener.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
