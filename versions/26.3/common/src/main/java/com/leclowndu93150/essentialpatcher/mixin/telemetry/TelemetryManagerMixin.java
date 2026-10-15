package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket;
import gg.essential.network.connectionmanager.telemetry.TelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TelemetryManager.class, remap = false)
public class TelemetryManagerMixin {

    @Inject(method = "enqueue", at = @At("HEAD"), cancellable = true)
    private void onEnqueue(ClientTelemetryPacket packet, CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }

    @Inject(method = "clientActionPerformed(Lgg/essential/network/connectionmanager/telemetry/TelemetryManager$Actions;)V", at = @At("HEAD"), cancellable = true)
    private void onClientAction(CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }

    @Inject(method = "clientActionPerformed(Lgg/essential/network/connectionmanager/telemetry/TelemetryManager$Actions;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onClientActionWithData(CallbackInfo ci) {
        if (PatcherConfig.get().disableTelemetry) {
            ci.cancel();
        }
    }
}
