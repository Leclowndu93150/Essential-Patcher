package com.leclowndu93150.essentialpatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CompatibilityTracker {

    public static final Set<String> SUPPORTED_VERSIONS = Set.of("1.3.10.8", "1.3.10.9");

    private static volatile String detectedVersion = null;
    private static volatile boolean versionChecked = false;
    private static final CopyOnWriteArrayList<String> failedInjections = new CopyOnWriteArrayList<>();

    public static void recordFailure(String mixinClass, String method) {
        failedInjections.add(mixinClass + " -> " + method);
    }

    public static List<String> getFailedInjections() {
        return Collections.unmodifiableList(new ArrayList<>(failedInjections));
    }

    public static boolean hasFailures() {
        return !failedInjections.isEmpty();
    }

    public static String getDetectedVersion() {
        if (!versionChecked) {
            detectedVersion = detectEssentialVersion();
            versionChecked = true;
        }
        return detectedVersion;
    }

    public static boolean isSupported() {
        String v = getDetectedVersion();
        return v != null && SUPPORTED_VERSIONS.contains(v);
    }

    public static boolean needsWarning() {
        return !isSupported() || hasFailures();
    }

    private static volatile PlatformVersionProvider versionProvider;

    public static void setPlatformVersionProvider(PlatformVersionProvider provider) {
        versionProvider = provider;
    }

    private static String detectEssentialVersion() {
        if (versionProvider != null) {
            String v = versionProvider.getEssentialVersion();
            if (v != null) return v;
        }

        try {
            var field = Class.forName("gg.essential.data.VersionInfo")
                    .getDeclaredField("essentialVersion");
            field.setAccessible(true);
            Object val = field.get(null);
            if (val != null) return val.toString();
        } catch (Exception ignored) {
        }

        return null;
    }

    public interface PlatformVersionProvider {
        String getEssentialVersion();
    }
}
