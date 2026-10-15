package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.menu.LeftSideBar;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LeftSideBar.class, remap = false)
public class LeftSideBarMixin {

    @Inject(method = "lambda$0$0", at = @At("HEAD"), cancellable = true)
    private static void essentialPatcher$hidePaperdoll(CallbackInfoReturnable<Unit> cir) {
        if (PatcherConfig.get().hideMenuPaperdoll) {
            cir.setReturnValue(Unit.INSTANCE);
        }
    }
}
