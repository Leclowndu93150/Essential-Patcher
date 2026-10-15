package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.data.OnboardingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OnboardingData.class, remap = false)
public class OnboardingMixin {

    @Unique
    private static boolean essentialPatcher$autoAccepted = false;

    @Inject(method = "hasAcceptedTos", at = @At("HEAD"), cancellable = true)
    private static void onHasAcceptedTos(CallbackInfoReturnable<Boolean> cir) {
        if (PatcherConfig.get().skipTosPopup) {
            if (!essentialPatcher$autoAccepted) {
                essentialPatcher$autoAccepted = true;
                OnboardingData.setAcceptedTos();
            }
            cir.setReturnValue(true);
        }
    }
}
