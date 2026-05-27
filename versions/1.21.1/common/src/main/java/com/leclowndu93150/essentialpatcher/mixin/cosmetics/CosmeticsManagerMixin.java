package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.cosmetics.CosmeticSaver;
import com.leclowndu93150.essentialpatcher.httpsync.SyncedCosmeticOutfit;
import com.leclowndu93150.essentialpatcher.network.CosmeticSyncData;
import gg.essential.cosmetics.model.CosmeticUnlockData;
import gg.essential.event.client.ClientTickEvent;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsData;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(value = CosmeticsManager.class, remap = false)
public abstract class CosmeticsManagerMixin {

    @Shadow
    public abstract void unlockAllCosmetics();

    @Shadow
    public abstract InfraEquippedOutfitsManager getInfraEquippedOutfitsManager();

    @Shadow
    private MutableState<Boolean> cosmeticsLoaded;

    @Shadow
    private CosmeticsData cosmeticsData;

    @Shadow
    private MutableState<Map<String, CosmeticUnlockData>> unlockedCosmeticsData;

    @Unique
    private boolean essentialPatcher$hasUnlockedThisSession = false;

    @Unique
    private int essentialPatcher$lastCatalogSize = -1;

    @Inject(method = "addUnlockedCosmeticsData", at = @At("RETURN"))
    private void onUnlockedCosmeticsReceived(Map<String, CosmeticUnlockData> unlockedCosmeticsData, CallbackInfo ci) {
        if (PatcherConfig.get().unlockAllCosmetics) {
            unlockAllCosmetics();
            essentialPatcher$hasUnlockedThisSession = true;
            essentialPatcher$restoreOutfit();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(ClientTickEvent event, CallbackInfo ci) {
        if (!PatcherConfig.get().unlockAllCosmetics) return;
        if (!cosmeticsLoaded.getUntracked()) return;

        int catalogSize = cosmeticsData.getCosmetics().get().size();
        if (catalogSize == essentialPatcher$lastCatalogSize) return;
        essentialPatcher$lastCatalogSize = catalogSize;

        unlockAllCosmetics();
        if (!essentialPatcher$hasUnlockedThisSession) {
            essentialPatcher$hasUnlockedThisSession = true;
            essentialPatcher$restoreOutfit();
        }
    }

    @Inject(method = "resetState", at = @At("HEAD"))
    private void onResetState(CallbackInfo ci) {
        essentialPatcher$hasUnlockedThisSession = false;
        essentialPatcher$lastCatalogSize = -1;
    }

    @Inject(method = "removeUnlockedCosmetics", at = @At("HEAD"), cancellable = true)
    private void onRevokeCosmetics(CallbackInfo ci) {
        if (PatcherConfig.get().unlockAllCosmetics) {
            ci.cancel();
        }
    }

    @Inject(method = "claimFreeItems", at = @At("HEAD"), cancellable = true)
    private void onClaimFreeItems(Set<String> ids, CallbackInfoReturnable<CompletableFuture<Boolean>> cir) {
        if (PatcherConfig.get().unlockAllCosmetics) {
            cir.setReturnValue(CompletableFuture.completedFuture(true));
        }
    }

    @Unique
    private void essentialPatcher$restoreOutfit() {
        try {
            SyncedCosmeticOutfit saved = CosmeticSaver.loadOutfit();
            if (saved.isEmpty()) return;

            InfraEquippedOutfitsManager manager = getInfraEquippedOutfitsManager();
            var ownUuidMethod = manager.getClass().getDeclaredMethod("getOwnUuid");
            ownUuidMethod.setAccessible(true);
            var ownUuid = (UUID) ownUuidMethod.invoke(manager);

            var infraOutfitClass = Class.forName("gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager$InfraOutfit");
            var constructor = infraOutfitClass.getConstructors()[0];
            var outfit = constructor.newInstance(
                    CosmeticSyncData.toInfraCosmetics(saved.equipped()),
                    CosmeticSyncData.toInfraSettings(saved.settings()),
                    null
            );

            manager.update(ownUuid, (InfraEquippedOutfitsManager.InfraOutfit) outfit);
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] Failed to restore equipped cosmetics: " + e.getMessage());
        }
    }
}
