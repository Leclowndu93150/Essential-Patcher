package com.leclowndu93150.essentialpatcher.mixin.notices;

import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.network.connectionmanager.notices.NoticesManager;
import gg.essential.notices.NoticeType;
import gg.essential.notices.model.Notice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Mixin(value = NoticesManager.class, remap = false)
public class NoticesManagerMixin {

    @Inject(method = "populateNotices", at = @At("HEAD"))
    private void filterNotices(Collection<? extends Notice> notices, CallbackInfo ci) {
        PatcherConfig config = PatcherConfig.get();

        if (config.disableAllNotices) {
            notices.clear();
            return;
        }

        Set<NoticeType> blocked = EnumSet.noneOf(NoticeType.class);
        if (config.disableSaleNotices) blocked.add(NoticeType.SALE);
        if (config.disableWardrobeBanners) blocked.add(NoticeType.WARDROBE_BANNER);
        if (config.disablePromoToasts) blocked.add(NoticeType.DISMISSIBLE_TOAST);
        if (config.disableNewCosmeticDots) blocked.add(NoticeType.NEW_BANNER);
        if (config.disableGiftedCosmeticToasts) blocked.add(NoticeType.GIFTED_COSMETIC_TOAST);

        if (!blocked.isEmpty()) {
            notices.removeIf(notice -> blocked.contains(notice.getType()));
        }
    }
}
