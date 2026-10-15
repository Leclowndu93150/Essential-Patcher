package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.StateKt;
import gg.essential.network.connectionmanager.notices.CosmeticNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CosmeticNotices.class, remap = false)
public class CosmeticNoticesMixin {

    @Inject(method = "getNewState", at = @At("HEAD"), cancellable = true)
    private void onGetNewState(String cosmeticId, CallbackInfoReturnable<State<Boolean>> cir) {
        if (PatcherConfig.get().disableNewCosmeticDots) {
            cir.setReturnValue(StateKt.stateOf(false));
        }
    }

    @Inject(method = "getHasAnyNewCosmetics", at = @At("HEAD"), cancellable = true)
    private void onGetHasAnyNewCosmetics(CallbackInfoReturnable<State<Boolean>> cir) {
        if (PatcherConfig.get().disableNewCosmeticDots) {
            cir.setReturnValue(StateKt.stateOf(false));
        }
    }
}
