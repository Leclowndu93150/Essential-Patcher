package com.leclowndu93150.essentialpatcher.mixin.network;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.network.connectionmanager.ConnectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Mixin(value = ConnectionManager.class, remap = false)
public class ConnectionManagerMixin {

    @Inject(method = "send(Lgg/essential/connectionmanager/common/packet/Packet;Ljava/util/function/Consumer;Ljava/util/concurrent/TimeUnit;Ljava/lang/Long;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet packet, Consumer<Optional<Packet>> callback, TimeUnit unit, Long timeout, CallbackInfo ci) {
        if (PatcherConfig.get().disableCheckoutPackets && isMonetizationPacket(packet)) {
            ci.cancel();
        }
    }

    private static boolean isMonetizationPacket(Packet packet) {
        String className = packet.getClass().getSimpleName();
        return className.equals("ClientCheckoutCoinBundlePacket")
                || className.equals("ClientCheckoutDynamicCoinBundlePacket")
                || className.equals("ClientCheckoutStoreBundlePacket")
                || className.equals("ClientCheckoutPartnerCodeRequestDataPacket")
                || className.equals("ClientCheckoutCosmeticsPacket");
    }
}
