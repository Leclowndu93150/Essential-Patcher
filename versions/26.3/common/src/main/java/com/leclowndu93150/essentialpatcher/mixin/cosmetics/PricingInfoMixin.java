package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "gg.essential.gui.wardrobe.Item$PricingInfo", remap = false)
public class PricingInfoMixin {

    @Inject(method = "getRealCost", at = @At("HEAD"), cancellable = true)
    private void onGetRealCost(CallbackInfoReturnable<Integer> cir) {
        if (PatcherConfig.get().showAllFree) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getBaseCost", at = @At("HEAD"), cancellable = true)
    private void onGetBaseCost(CallbackInfoReturnable<Integer> cir) {
        if (PatcherConfig.get().showAllFree) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getDiscountPercentage", at = @At("HEAD"), cancellable = true)
    private void onGetDiscount(CallbackInfoReturnable<Integer> cir) {
        if (PatcherConfig.get().showAllFree) {
            cir.setReturnValue(100);
        }
    }
}
