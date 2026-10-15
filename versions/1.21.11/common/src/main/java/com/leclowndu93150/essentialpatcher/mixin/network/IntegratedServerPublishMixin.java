package com.leclowndu93150.essentialpatcher.mixin.network;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import com.leclowndu93150.essentialpatcher.httpsync.SessionKey;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Essential 1.5 removed SPSStartEvent; hosting a world now publishes the integrated server.
 * Re-key cosmetic sync so the host joins the same session as its guests.
 */
@Mixin(IntegratedServer.class)
public class IntegratedServerPublishMixin {

    @Inject(method = "publishServer", at = @At("RETURN"))
    private void essentialPatcher$onPublished(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !PatcherConfig.get().unlockAllCosmetics) return;

        String[] key = SessionKey.compute();
        if (key != null) {
            CosmeticHttpSync.get().onServerJoin(key[0], key[1]);
        }
    }
}
