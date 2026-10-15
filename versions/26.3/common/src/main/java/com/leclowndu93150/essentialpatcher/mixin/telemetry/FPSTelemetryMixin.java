package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.telemetry.FPSTelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FPSTelemetryManager.class, remap = false)
public class FPSTelemetryMixin {

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true)
    private void onInitialize(CallbackInfo ci) {
        if (PatcherConfig.get().disableFPSTelemetry) {
            ci.cancel();
        }
    }
}
