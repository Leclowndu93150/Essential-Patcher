package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.social.RulesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RulesManager.class, remap = false)
public class RulesManagerMixin {

    @Inject(method = "getAcceptedRules", at = @At("HEAD"), cancellable = true)
    private void onGetAcceptedRules(CallbackInfoReturnable<Boolean> cir) {
        if (PatcherConfig.get().skipCommunityRules) {
            cir.setReturnValue(true);
        }
    }
}
