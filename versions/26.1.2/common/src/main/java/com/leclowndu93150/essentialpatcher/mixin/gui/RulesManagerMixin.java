package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket;
import gg.essential.connectionmanager.common.packet.social.ClientCommunityRulesAgreedPacket;
import gg.essential.network.CMConnection;
import gg.essential.network.connectionmanager.social.RulesManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = RulesManager.class, remap = false)
public class RulesManagerMixin {

    @Shadow
    @Final
    private CMConnection connectionManager;

    @Shadow
    private boolean acceptedRules;

    @Unique
    private boolean essentialPatcher$agreementSent = false;

    @Inject(method = "getAcceptedRules", at = @At("HEAD"), cancellable = true)
    private void onGetAcceptedRules(CallbackInfoReturnable<Boolean> cir) {
        if (!PatcherConfig.get().skipCommunityRules) {
            return;
        }
        if (!acceptedRules && !essentialPatcher$agreementSent && connectionManager.isOpen()) {
            essentialPatcher$agreementSent = true;
            connectionManager.send(new ClientCommunityRulesAgreedPacket(), this::essentialPatcher$onAgreed);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "resetState", at = @At("TAIL"))
    private void onResetState(CallbackInfo ci) {
        essentialPatcher$agreementSent = false;
    }

    @Unique
    private void essentialPatcher$onAgreed(Optional<Packet> response) {
        Packet packet = response.orElse(null);
        if (packet instanceof ResponseActionPacket && ((ResponseActionPacket) packet).isSuccessful()) {
            acceptedRules = true;
        } else {
            essentialPatcher$agreementSent = false;
        }
    }
}
