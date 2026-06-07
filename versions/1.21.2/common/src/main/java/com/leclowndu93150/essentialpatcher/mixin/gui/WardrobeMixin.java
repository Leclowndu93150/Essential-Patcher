package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.wardrobe.Wardrobe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Wardrobe.class, remap = false)
public class WardrobeMixin {

    @Inject(method = "displayCartWarningModal", at = @At("HEAD"), cancellable = true)
    private void onDisplayCartWarning(CallbackInfo ci) {
        if (PatcherConfig.get().disableWardrobeNagModals) {
            ci.cancel();
        }
    }

    @Inject(method = "hasUnownedItems", at = @At("HEAD"), cancellable = true)
    private void onHasUnownedItems(CallbackInfoReturnable<Boolean> cir) {
        if (PatcherConfig.get().disableWardrobeNagModals) {
            cir.setReturnValue(false);
        }
    }
}
