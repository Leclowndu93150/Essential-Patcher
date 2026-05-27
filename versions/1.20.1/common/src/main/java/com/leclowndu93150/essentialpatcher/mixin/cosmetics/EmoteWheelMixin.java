package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import gg.essential.model.BedrockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sync emote plays to peers via the HTTP cosmetics service at the intent layer:
 * one mixin per user click, no packet-level filtering needed.
 *
 * First-time emote equips also flow through InfraEquippedOutfitsManager.update
 * (covered by InfraEquippedOutfitsMixin -> cosmetic_changed event). This catches
 * re-plays of an already-equipped emote, which only update the wire packet, not
 * the equipped map.
 */
@Mixin(targets = "gg.essential.gui.emotes.EmoteWheel$Companion", remap = false)
public class EmoteWheelMixin {

    @Inject(method = "equipEmote", at = @At("HEAD"))
    private void onEquipEmote(BedrockModel emote, CallbackInfo ci) {
        try {
            // Tell peers' Essential to fire a "reset" animation event on the EMOTE slot.
            // Equip-state changes (a *different* emote) are already covered by the
            // InfraEquippedOutfitsMixin -> cosmetic_changed path; this catches the
            // replay-same-emote case where Essential doesn't touch the equipped map.
            CosmeticHttpSync.get().onLocalEmoteTrigger("EMOTE", "reset");
        } catch (Throwable ignored) {
        }
    }
}
