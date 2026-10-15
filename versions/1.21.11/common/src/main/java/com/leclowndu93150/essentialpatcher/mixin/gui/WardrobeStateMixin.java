package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.gui.wardrobe.WardrobeCategory;
import gg.essential.gui.wardrobe.WardrobeState;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = WardrobeState.class, remap = false)
public class WardrobeStateMixin {

    @Inject(method = "availableCosmetics$lambda$0", at = @At("RETURN"), cancellable = true)
    private static void essentialPatcher$keepOnlyCapes(CallbackInfoReturnable<List<Cosmetic>> cir) {
        if (!PatcherConfig.get().hideEssentialCosmetics) return;

        List<Cosmetic> capes = new ArrayList<>();
        for (Cosmetic cosmetic : cir.getReturnValue()) {
            if (cosmetic.getSlot() == CosmeticSlot.CAPE) {
                capes.add(cosmetic);
            }
        }
        cir.setReturnValue(capes);
    }

    @Inject(method = "allCategories$lambda$0", at = @At("RETURN"), cancellable = true)
    private static void essentialPatcher$hideStoreCategories(CallbackInfoReturnable<List<WardrobeCategory>> cir) {
        if (!PatcherConfig.get().hideEssentialCosmetics) return;

        List<WardrobeCategory> categories = new ArrayList<>(cir.getReturnValue());
        categories.removeIf(category -> category instanceof WardrobeCategory.FeaturedRefresh
                || category instanceof WardrobeCategory.Emotes);
        cir.setReturnValue(categories);
    }
}
