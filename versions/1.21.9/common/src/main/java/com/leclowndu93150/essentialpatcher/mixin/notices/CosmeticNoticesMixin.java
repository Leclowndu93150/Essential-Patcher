package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.notices.CosmeticNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CosmeticNotices.class, remap = false)
public class CosmeticNoticesMixin {

    @Inject(method = "cosmeticAdded", at = @At("HEAD"), cancellable = true)
    private void onCosmeticAdded(String cosmeticId, CallbackInfo ci) {
        if (PatcherConfig.get().disableNewCosmeticDots) {
            ci.cancel();
        }
    }
}
