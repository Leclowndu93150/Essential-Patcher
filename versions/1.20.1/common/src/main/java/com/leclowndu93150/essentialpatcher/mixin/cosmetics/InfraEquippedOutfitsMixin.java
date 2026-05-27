package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.cosmetics.CosmeticSaver;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.SyncedCosmeticOutfit;
import com.leclowndu93150.essentialpatcher.network.CosmeticSyncData;
import gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = InfraEquippedOutfitsManager.class, remap = false)
public class InfraEquippedOutfitsMixin {

    @ModifyVariable(method = "update", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private InfraEquippedOutfitsManager.InfraOutfit essentialPatcher$mergeHttpCosmetics(InfraEquippedOutfitsManager.InfraOutfit outfit, UUID playerId) {
        if (!PatcherConfig.get().unlockAllCosmetics) return outfit;

        UUID ownUuid = essentialPatcher$getOwnUuid((InfraEquippedOutfitsManager) (Object) this);
        if (ownUuid != null && ownUuid.equals(playerId)) return outfit;

        SyncedCosmeticOutfit peerData = CosmeticHttpSync.get().getPeerCosmetics(playerId);
        if (peerData == null || peerData.isEmpty()) return outfit;

        return new InfraEquippedOutfitsManager.InfraOutfit(
                CosmeticSyncData.toInfraCosmetics(peerData.equipped()),
                CosmeticSyncData.toInfraSettings(peerData.settings()),
                outfit.getSkin()
        );
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void essentialPatcher$saveLocal(UUID playerId, InfraEquippedOutfitsManager.InfraOutfit outfit, CallbackInfo ci) {
        if (!PatcherConfig.get().unlockAllCosmetics) return;

        UUID ownUuid = essentialPatcher$getOwnUuid((InfraEquippedOutfitsManager) (Object) this);
        if (ownUuid == null || !ownUuid.equals(playerId)) return;

        try {
            SyncedCosmeticOutfit serializable = CosmeticSyncData.fromInfraOutfit(outfit);
            CosmeticSaver.saveOutfit(serializable);
            CosmeticHttpSync.get().onLocalCosmeticChange(serializable);
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to save equipped cosmetics: " + e.getMessage());
        }
    }

    private static UUID essentialPatcher$getOwnUuid(InfraEquippedOutfitsManager manager) {
        try {
            var method = manager.getClass().getDeclaredMethod("getOwnUuid");
            method.setAccessible(true);
            return (UUID) method.invoke(manager);
        } catch (Exception e) {
            return null;
        }
    }
}
