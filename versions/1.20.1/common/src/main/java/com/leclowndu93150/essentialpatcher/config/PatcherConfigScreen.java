package com.leclowndu93150.essentialpatcher.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatcherConfigScreen {

    public static Screen create(Screen parent) {
        PatcherConfig config = PatcherConfig.get();
        PatcherConfig defaults = new PatcherConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Essential Patcher"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Cosmetics"))
                        .option(toggle("Unlock All Cosmetics",
                                "Unlocks all cosmetics client-side. Re-applies every session when cosmetics load. Others won't see unowned ones.",
                                defaults.unlockAllCosmetics, () -> config.unlockAllCosmetics, v -> config.unlockAllCosmetics = v))
                        .option(toggle("Show All as Free",
                                "Shows all cosmetic prices as FREE (0 coins) in the wardrobe.",
                                defaults.showAllFree, () -> config.showAllFree, v -> config.showAllFree = v))
                        .option(toggle("Disable New Cosmetic Dots",
                                "Removes the \"new\" indicator dots on recently added cosmetics (FOMO bait).",
                                defaults.disableNewCosmeticDots, () -> config.disableNewCosmeticDots, v -> config.disableNewCosmeticDots = v))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Telemetry"))
                        .option(toggle("Disable Telemetry",
                                "Blocks all telemetry packets — hardware info, display data, feature usage, action tracking.",
                                defaults.disableTelemetry, () -> config.disableTelemetry, v -> config.disableTelemetry = v))
                        .option(toggle("Disable FPS Telemetry",
                                "Blocks FPS session monitoring and reporting.",
                                defaults.disableFPSTelemetry, () -> config.disableFPSTelemetry, v -> config.disableFPSTelemetry = v))
                        .option(toggle("Disable Impression Tracking",
                                "Blocks tracking of which cosmetics you see on other players.",
                                defaults.disableImpressionTracking, () -> config.disableImpressionTracking, v -> config.disableImpressionTracking = v))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Notices"))
                        .option(toggle("Disable All Notices (Nuclear)",
                                "Blocks ALL server-pushed notices. WARNING: This also kills friend request toasts! Only enable if you know what you're doing.",
                                defaults.disableAllNotices, () -> config.disableAllNotices, v -> config.disableAllNotices = v))
                        .option(toggle("Disable Sale Notices",
                                "Blocks sale/discount promotions pushed by Essential servers.",
                                defaults.disableSaleNotices, () -> config.disableSaleNotices, v -> config.disableSaleNotices = v))
                        .option(toggle("Disable Wardrobe Banners",
                                "Removes colored ad banners from the wardrobe UI.",
                                defaults.disableWardrobeBanners, () -> config.disableWardrobeBanners, v -> config.disableWardrobeBanners = v))
                        .option(toggle("Disable Promo Toasts",
                                "Blocks server-pushed promotional toast notifications on the main menu.",
                                defaults.disablePromoToasts, () -> config.disablePromoToasts, v -> config.disablePromoToasts = v))
                        .option(toggle("Disable Gifted Cosmetic Toasts",
                                "Blocks toast notifications when someone gifts you a cosmetic.",
                                defaults.disableGiftedCosmeticToasts, () -> config.disableGiftedCosmeticToasts, v -> config.disableGiftedCosmeticToasts = v))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Monetization"))
                        .option(toggle("Disable Purchase Flows",
                                "Blocks all cosmetic/emote/bundle purchase flows in the wardrobe. The wardrobe still works for browsing and equipping.",
                                defaults.disablePurchaseFlows, () -> config.disablePurchaseFlows, v -> config.disablePurchaseFlows = v))
                        .option(toggle("Disable Coin Purchases",
                                "Blocks the coin purchase flow in CoinsManager. Prevents real-money transactions.",
                                defaults.disableCoinPurchases, () -> config.disableCoinPurchases, v -> config.disableCoinPurchases = v))
                        .option(toggle("Disable Coin Purchase Modal",
                                "Prevents the \"Buy Coins\" modal from opening.",
                                defaults.disableCoinPurchaseModal, () -> config.disableCoinPurchaseModal, v -> config.disableCoinPurchaseModal = v))
                        .option(toggle("Disable Checkout Packets",
                                "Blocks all checkout-related network packets at the connection level.",
                                defaults.disableCheckoutPackets, () -> config.disableCheckoutPackets, v -> config.disableCheckoutPackets = v))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("GUI"))
                        .option(toggle("Disable Wardrobe Nag Modals",
                                "Removes the \"unowned items equipped\" warning when closing the wardrobe. The back button just works.",
                                defaults.disableWardrobeNagModals, () -> config.disableWardrobeNagModals, v -> config.disableWardrobeNagModals = v))
                        .option(toggle("Skip TOS Popup",
                                "Automatically accepts the Essential Terms of Service popup on first launch.",
                                defaults.skipTosPopup, () -> config.skipTosPopup, v -> config.skipTosPopup = v))
                        .option(toggle("Disable Wiki Toast",
                                "Removes the \"Visit the Essential Wiki\" toast notification.",
                                defaults.disableWikiToast, () -> config.disableWikiToast, v -> config.disableWikiToast = v))
                        .option(toggle("Skip Community Rules",
                                "Skips the community chat rules agreement popup.",
                                defaults.skipCommunityRules, () -> config.skipCommunityRules, v -> config.skipCommunityRules = v))
                        .option(toggle("Hide Announcement Channel",
                                "Hides the Essential announcement channel from the social tab.",
                                defaults.hideAnnouncementChannel, () -> config.hideAnnouncementChannel, v -> config.hideAnnouncementChannel = v))
                        .option(toggle("Disable Auto-Update",
                                "Prevents Essential from auto-updating by writing autoUpdate=false to its stage2 config. Takes effect next launch.",
                                defaults.disableAutoUpdate, () -> config.disableAutoUpdate, v -> config.disableAutoUpdate = v))
                        .build())
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> toggle(String name, String description, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .controller(TickBoxControllerBuilder::create)
                .binding(def, getter, setter)
                .build();
    }
}
