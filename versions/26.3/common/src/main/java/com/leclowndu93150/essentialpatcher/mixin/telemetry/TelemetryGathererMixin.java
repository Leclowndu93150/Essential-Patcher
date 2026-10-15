package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.telemetry.TelemetryGatherer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Essential 1.5.1 moved telemetry gathering out of TelemetryManager into TelemetryGatherer. */
@Mixin(value = TelemetryGatherer.class, remap = false)
public class TelemetryGathererMixin {

    @Inject(method = {
            "queueHardwareTelemetry",
            "sendInitialAutoUpdateState",
            "setupAutoUpdateTracking",
            "sendAutoUpdateTelemetry",
            "sendDisplayData"
    }, at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$blockTelemetry(CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }
}
