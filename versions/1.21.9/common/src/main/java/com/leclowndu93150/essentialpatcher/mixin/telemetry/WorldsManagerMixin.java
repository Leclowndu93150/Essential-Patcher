package com.leclowndu93150.essentialpatcher.mixin.telemetry;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "gg.essential.sps.WorldsManager", remap = false)
public class WorldsManagerMixin {

    @Inject(method = "sendSharedWorldTelemetry", at = @At("HEAD"), cancellable = true)
    private void onSendSharedWorldTelemetry(CallbackInfoReturnable<Object> cir) {
        if (PatcherConfig.get().disableTelemetry) {
            cir.setReturnValue(Unit.INSTANCE);
        }
    }
}
