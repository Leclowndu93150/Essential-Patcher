package com.leclowndu93150.essentialpatcher.httpsync;

import gg.essential.Essential;
import gg.essential.event.sps.SPSStartEvent;
import gg.essential.lib.kbrewster.eventbus.Subscribe;
import gg.essential.upnp.model.UPnPSession;

import java.util.UUID;

final class LegacySps {

    private LegacySps() {
    }

    static void listenForHosting() {
        Essential.EVENT_BUS.register(new LegacySps());
    }

    static UUID localHost() {
        UPnPSession localSession = Essential.getInstance().getConnectionManager().getSpsManager().getLocalSession();
        return localSession != null ? localSession.getHostUUID() : null;
    }

    @Subscribe
    public void onSpsStart(SPSStartEvent event) {
        String[] key = SessionKey.fromEssentialSpsAddress(event.getAddress());
        if (key != null) {
            CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
        }
    }
}
