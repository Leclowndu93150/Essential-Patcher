package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.telemetry.ImpressionTelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ImpressionTelemetryManager.class, remap = false)
public class ImpressionTelemetryMixin {

    @Inject(method = "addImpression", at = @At("HEAD"), cancellable = true)
    private void onAddImpression(String cosmeticId, UUID playerId, CallbackInfo ci) {
        if (PatcherConfig.get().disableImpressionTracking) {
            ci.cancel();
        }
    }

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true)
    private void onInitialize(CallbackInfo ci) {
        if (PatcherConfig.get().disableImpressionTracking) {
            ci.cancel();
        }
    }
}
