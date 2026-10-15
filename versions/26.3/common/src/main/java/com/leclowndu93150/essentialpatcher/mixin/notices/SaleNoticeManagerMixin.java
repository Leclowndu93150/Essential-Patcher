package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.elementa.state.v2.ListKt;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.StateKt;
import gg.essential.gui.state.Sale;
import gg.essential.network.connectionmanager.notices.SaleNoticeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;

@Mixin(value = SaleNoticeManager.class, remap = false)
public class SaleNoticeManagerMixin {

    @Inject(method = "getSaleState", at = @At("RETURN"), cancellable = true)
    private void onGetSaleState(CallbackInfoReturnable<State<?>> cir) {
        if (PatcherConfig.get().disableSaleNotices) {
            cir.setReturnValue(cir.getReturnValue().getUntracked() instanceof Set
                    ? StateKt.stateOf(Collections.<Sale>emptySet())
                    : ListKt.<Sale>listStateOf());
        }
    }
}
