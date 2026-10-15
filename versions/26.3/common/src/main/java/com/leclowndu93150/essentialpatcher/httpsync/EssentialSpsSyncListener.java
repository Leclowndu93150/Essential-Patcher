package com.leclowndu93150.essentialpatcher.httpsync;

import gg.essential.Essential;
import gg.essential.event.sps.SPSStartEvent;
import gg.essential.lib.kbrewster.eventbus.Subscribe;

public final class EssentialSpsSyncListener {

    private static boolean registered;

    private EssentialSpsSyncListener() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        try {
            Essential.EVENT_BUS.register(new EssentialSpsSyncListener());
            registered = true;
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to register SPS sync listener: " + e.getMessage());
        }
    }

    @Subscribe
    public void onSpsStart(SPSStartEvent event) {
        String[] key = SessionKey.fromEssentialSpsAddress(event.getAddress());
        if (key != null) {
            CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
        }
    }
}
