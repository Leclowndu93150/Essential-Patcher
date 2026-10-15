package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.sparkuniverse.toolbox.chat.model.Channel;
import gg.essential.network.connectionmanager.chat.ChatManager;
import gg.essential.util.ChannelExtensionsKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Set;

@Mixin(value = ChatManager.class, remap = false)
public class ChatManagerMixin {

    @Inject(method = "addChannel", at = @At("HEAD"), cancellable = true)
    private void onAddChannel(Channel channel, CallbackInfo ci) {
        if (PatcherConfig.get().hideAnnouncementChannel && ChannelExtensionsKt.isAnnouncement(channel)) {
            ci.cancel();
        }
    }

    @Inject(method = "getAnnouncementChannelIds", at = @At("HEAD"), cancellable = true)
    private void onGetAnnouncementChannelIds(CallbackInfoReturnable<Set<Long>> cir) {
        if (PatcherConfig.get().hideAnnouncementChannel) {
            cir.setReturnValue(Collections.emptySet());
        }
    }

    @Inject(method = "getPrimaryAnnouncementChannelId", at = @At("HEAD"), cancellable = true)
    private void onGetPrimaryAnnouncementChannelId(CallbackInfoReturnable<Long> cir) {
        if (PatcherConfig.get().hideAnnouncementChannel) {
            cir.setReturnValue(-1L);
        }
    }
}
