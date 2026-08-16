package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.notices.SaleNoticeManager;
import gg.essential.notices.model.Notice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SaleNoticeManager.class, remap = false)
public class SaleNoticeManagerMixin {

    @Inject(method = "noticeAdded", at = @At("HEAD"), cancellable = true)
    private void onSaleNotice(Notice notice, CallbackInfo ci) {
        if (PatcherConfig.get().disableSaleNotices) {
            ci.cancel();
        }
    }
}
