package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.telemetry.FeatureSessionTelemetry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FeatureSessionTelemetry.class, remap = false)
public class FeatureSessionTelemetryMixin {

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void onStart(CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }

    @Inject(method = "startEvent", at = @At("HEAD"), cancellable = true)
    private void onStartEvent(String name, CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }

    @Inject(method = "endEvent", at = @At("HEAD"), cancellable = true)
    private void onEndEvent(String name, CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }
}
