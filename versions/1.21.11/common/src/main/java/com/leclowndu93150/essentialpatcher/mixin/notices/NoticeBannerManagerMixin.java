package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.elementa.state.v2.ListKt;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.network.connectionmanager.notices.NoticeBanner;
import gg.essential.network.connectionmanager.notices.NoticeBannerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NoticeBannerManager.class, remap = false)
public class NoticeBannerManagerMixin {

    @Inject(method = "getNoticeBanners", at = @At("HEAD"), cancellable = true)
    private void onGetNoticeBanners(CallbackInfoReturnable<State<?>> cir) {
        if (PatcherConfig.get().disableWardrobeBanners) {
            cir.setReturnValue(ListKt.<NoticeBanner>mutableListStateOf());
        }
    }
}
