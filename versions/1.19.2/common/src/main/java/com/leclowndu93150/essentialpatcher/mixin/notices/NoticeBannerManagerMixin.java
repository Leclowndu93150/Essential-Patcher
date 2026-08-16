package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.notices.NoticeBannerManager;
import gg.essential.notices.model.Notice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NoticeBannerManager.class, remap = false)
public class NoticeBannerManagerMixin {

    @Inject(method = "noticeAdded", at = @At("HEAD"), cancellable = true)
    private void onBannerNotice(Notice notice, CallbackInfo ci) {
        if (PatcherConfig.get().disableWardrobeBanners) {
            ci.cancel();
        }
    }
}
