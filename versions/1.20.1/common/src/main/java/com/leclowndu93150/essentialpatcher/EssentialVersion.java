package com.leclowndu93150.essentialpatcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class EssentialVersion {

    private EssentialVersion() {
    }

    public static void require(String minimum) {
        String installed = installed();
        if (installed != null && compare(installed, minimum) < 0) {
            throw new IllegalStateException("Essential Patcher needs Essential " + minimum + " or newer, but Essential "
                    + installed + " is installed. Update Essential.");
        }
    }

    private static String installed() {
        try (InputStream in = EssentialVersion.class.getClassLoader().getResourceAsStream("assets/essential/version.txt")) {
            return in != null ? new String(in.readAllBytes(), StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static int compare(String a, String b) {
        String[] left = a.split("\\.");
        String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int diff = Integer.compare(part(left, i), part(right, i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int part(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String digits = parts[index].replaceAll("\\D.*", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
