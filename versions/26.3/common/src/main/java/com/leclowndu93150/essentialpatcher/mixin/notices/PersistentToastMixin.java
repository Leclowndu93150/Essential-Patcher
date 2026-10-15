package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.notices.PersistentToastNoticeListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PersistentToastNoticeListener.class, remap = false)
public class PersistentToastMixin {

    @Inject(method = "pushNoticeToast", at = @At("HEAD"), cancellable = true)
    private void onPushNoticeToast(CallbackInfo ci) {
        if (PatcherConfig.get().disablePromoToasts) {
            ci.cancel();
        }
    }
}
