package com.leclowndu93150.essentialpatcher.httpsync;

import gg.essential.Essential;
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.StateKt;
import gg.essential.sps.WorldManager;
import kotlin.Unit;
import net.minecraft.client.Minecraft;

import java.util.UUID;

final class ModernSps {

    private static ReferenceHolderImpl hostingWatcher;

    private ModernSps() {
    }

    static void listenForHosting() {
        hostingWatcher = new ReferenceHolderImpl();
        State<WorldManager> integratedServerWorld = integratedServerWorld();
        StateKt.effect(hostingWatcher, observer -> {
            WorldManager world = observer.invoke(integratedServerWorld);
            if (world != null && observer.invoke(world.getLocalWorldOpen())) {
                String[] key = SessionKey.compute();
                if (key != null) {
                    CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
                }
            }
            return Unit.INSTANCE;
        });
    }

    static UUID localHost() {
        WorldManager world = integratedServerWorld().getUntracked();
        return world != null && world.getLocalWorldOpen().getUntracked()
                ? Minecraft.getInstance().getUser().getProfileId()
                : null;
    }

    private static State<WorldManager> integratedServerWorld() {
        return Essential.getInstance().getWorldsManager().getIntegratedServerWorld();
    }
}
