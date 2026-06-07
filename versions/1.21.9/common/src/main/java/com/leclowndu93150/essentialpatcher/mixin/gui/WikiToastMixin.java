package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.event.essential.TosAcceptedEvent;
import gg.essential.handlers.WikiToastListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WikiToastListener.class, remap = false)
public class WikiToastMixin {

    @Inject(method = "onTosAccepted", at = @At("HEAD"), cancellable = true)
    private void onWikiToast(TosAcceptedEvent event, CallbackInfo ci) {
        if (PatcherConfig.get().disableWikiToast) {
            ci.cancel();
        }
    }
}
