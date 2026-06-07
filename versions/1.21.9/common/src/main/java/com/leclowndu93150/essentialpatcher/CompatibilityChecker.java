package com.leclowndu93150.essentialpatcher;

import java.lang.reflect.Method;

public final class CompatibilityChecker {

    private static boolean checked = false;

    public static void runChecks() {
        if (checked) return;
        checked = true;

        probe("gg.essential.network.connectionmanager.telemetry.TelemetryManager",
                "enqueue", "sendHardwareTelemetry", "sendDisplayData",
                "clientActionPerformed", "sendAutoUpdateTelemetry");

        probe("gg.essential.network.connectionmanager.telemetry.FPSTelemetryManager",
                "initialize");

        probe("gg.essential.network.connectionmanager.telemetry.ImpressionTelemetryManager",
                "addImpression", "initialize");

        probe("gg.essential.network.connectionmanager.telemetry.FeatureSessionTelemetry",
                "start", "startEvent", "endEvent");

        probe("gg.essential.network.connectionmanager.cosmetics.CosmeticsManager",
                "unlockAllCosmetics", "addUnlockedCosmeticsData", "tick",
                "resetState", "removeUnlockedCosmetics", "claimFreeItems");

        probe("gg.essential.network.connectionmanager.coins.CoinsManager",
                "purchaseBundle", "tryClaimingWelcomeCoins");

        probe("gg.essential.network.connectionmanager.ConnectionManager",
                "send");

        probe("gg.essential.network.connectionmanager.notices.NoticesManager",
                "populateNotices");

        probe("gg.essential.network.connectionmanager.notices.SaleNoticeManager",
                "noticeAdded");

        probe("gg.essential.network.connectionmanager.notices.NoticeBannerManager",
                "noticeAdded");

        probe("gg.essential.network.connectionmanager.notices.PersistentToastNoticeListener",
                "noticeAdded");

        probe("gg.essential.network.connectionmanager.notices.CosmeticNotices",
                "cosmeticAdded");

        probe("gg.essential.data.OnboardingData",
                "hasAcceptedTos");

        probe("gg.essential.network.connectionmanager.social.RulesManager",
                "getAcceptedRules");

        probe("gg.essential.network.connectionmanager.chat.ChatManager",
                "addChannel", "getAnnouncementChannelIds", "getPrimaryAnnouncementChannelId");

        probe("gg.essential.gui.wardrobe.Wardrobe",
                "displayCartWarningModal", "hasUnownedItems");

        probe("gg.essential.handlers.WikiToastListener",
                "onTosAccepted");

        int failures = CompatibilityTracker.getFailedInjections().size();
        if (failures > 0) {
            System.err.println("[EssentialPatcher] " + failures + " method(s) missing in Essential. Some patches will not apply.");
            for (String f : CompatibilityTracker.getFailedInjections()) {
                System.err.println("[EssentialPatcher]   - " + f);
            }
        }
    }

    private static void probe(String className, String... methodNames) {
        try {
            Class<?> clazz = Class.forName(className, false,
                    CompatibilityChecker.class.getClassLoader());
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (String target : methodNames) {
                boolean found = false;
                for (Method m : declaredMethods) {
                    if (m.getName().equals(target)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String shortClass = className.substring(className.lastIndexOf('.') + 1);
                    CompatibilityTracker.recordFailure(shortClass, target);
                }
            }
        } catch (ClassNotFoundException e) {
            String shortClass = className.substring(className.lastIndexOf('.') + 1);
            CompatibilityTracker.recordFailure(shortClass, "(class not found)");
        }
    }
}
