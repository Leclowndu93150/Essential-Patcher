package com.leclowndu93150.essentialpatcher;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.spongepowered.asm.mixin.Mixins;

public class EssentialpatcherPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        System.out.println("[EssentialPatcher] Registering mixin config (post-Essential preLaunch)");
        Mixins.addConfiguration("essentialpatcher.mixins.json");
    }
}
