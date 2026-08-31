package com.leclowndu93150.essentialpatcher.httpsync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.leclowndu93150.essentialpatcher.network.CosmeticSyncData;
import gg.essential.Essential;
import gg.essential.mod.cosmetics.CosmeticSlot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class CosmeticHttpSync {

    public interface LocalProfileProvider {
        String username();

        UUID uuid();
    }

    private static final Gson GSON = new Gson();
    private static final CosmeticHttpSync INSTANCE = new CosmeticHttpSync();

    private static final long[] RETRY_BACKOFF_MS = {2_000L, 5_000L, 15_000L, 30_000L, 60_000L};
    private static final int MAX_HEARTBEAT_FAILURES = 3;

    public static CosmeticHttpSync get() {
        return INSTANCE;
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "EssentialPatcher-HttpSync");
        t.setDaemon(true);
        return t;
    });

    private volatile LocalProfileProvider profileProvider;
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicBoolean joined = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean authenticating = new AtomicBoolean();
    private final AtomicInteger sessionGeneration = new AtomicInteger();
    private final AtomicInteger heartbeatFailures = new AtomicInteger();
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile Thread sessionThread;

    private volatile String lastKeyIngredient;
    private volatile String lastLabel;
    private final ConcurrentHashMap<UUID, SyncedCosmeticOutfit> sessionPeers = new ConcurrentHashMap<>();

    public void setLocalProfileProvider(LocalProfileProvider p) {
        this.profileProvider = p;
    }

    public boolean isEnabled() {
        PatcherConfig c = PatcherConfig.get();
        return c.httpCosmeticSync && c.httpCosmeticSyncBaseUrl != null && !c.httpCosmeticSyncBaseUrl.isBlank();
    }

    private String baseUrl() {
        String url = PatcherConfig.get().httpCosmeticSyncBaseUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    public void onServerJoin(String keyIngredient, String label) {
        if (!isEnabled() || profileProvider == null) return;
        closeSession(false);
        closing.set(false);
        lastKeyIngredient = keyIngredient;
        lastLabel = label;

        int generation = sessionGeneration.incrementAndGet();
        String sid = computeSessionId(keyIngredient, label);
        heartbeatFailures.set(0);
        heartbeatTask = scheduler.scheduleAtFixedRate(this::heartbeat, 30, 30, TimeUnit.SECONDS);

        Thread thread = new Thread(() -> runSession(generation, sid), "EssentialPatcher-Session");
        thread.setDaemon(true);
        sessionThread = thread;
        thread.start();
    }

    public void onServerLeave() {
        closeSession(true);
    }

    private void closeSession(boolean notifyServer) {
        sessionGeneration.incrementAndGet();
        boolean wasJoined = joined.getAndSet(false);
        closing.set(true);

        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(false);
            heartbeatTask = null;
        }
        Thread thread = sessionThread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
        sessionThread = null;
        sessionPeers.clear();

        String tk = token.get();
        sessionId.set(null);
        if (!notifyServer || !wasJoined || tk == null) {
            return;
        }
        scheduler.submit(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/session/leave"))
                        .header("Authorization", "Bearer " + tk)
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                http.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        });
    }

    public void onLocalCosmeticChange(SyncedCosmeticOutfit outfit) {
        if (!joined.get()) return;
        scheduler.submit(() -> {
            try {
                pushCosmetics(outfit);
            } catch (Exception e) {
                System.err.println("[EssentialPatcher] http sync push failed: " + e.getMessage());
            }
        });
    }

    public void onLocalEmoteTrigger(String slot, String emoteId) {
        if (!joined.get()) return;
        if (slot == null || emoteId == null) return;
        scheduler.submit(() -> {
            try {
                pushTrigger(slot, emoteId);
            } catch (Exception e) {
                System.err.println("[EssentialPatcher] http sync trigger failed: " + e.getMessage());
            }
        });
    }

    private void pushCurrentOutfit() {
        if (!joined.get()) return;
        try {
            SyncedCosmeticOutfit local = CosmeticSyncData.getLiveLocalOutfit();
            if (!local.isEmpty()) {
                pushCosmetics(local);
            }
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] http sync current outfit push failed: " + e.getMessage());
        }
    }

    private void runSession(int generation, String sid) {
        int attempt = 0;
        while (!closing.get() && generation == sessionGeneration.get()) {
            boolean streamed = false;
            try {
                ensureAuthenticated();
                joinSession(sid);
                if (closing.get() || generation != sessionGeneration.get()) return;

                joined.set(true);
                heartbeatFailures.set(0);
                attempt = 0;

                pushCurrentOutfit();
                scheduler.schedule(this::pushCurrentOutfit, 2, TimeUnit.SECONDS);
                scheduler.schedule(this::pushCurrentOutfit, 8, TimeUnit.SECONDS);

                streamed = runSseStream();
            } catch (Exception e) {
                if (!closing.get() && generation == sessionGeneration.get()) {
                    System.err.println("[EssentialPatcher] http sync session failed: " + e.getMessage());
                }
            }
            joined.set(false);
            if (closing.get() || generation != sessionGeneration.get()) return;

            attempt = streamed ? 0 : attempt + 1;
            if (!sleepQuietly(backoffMs(attempt))) return;
        }
    }

    private boolean runSseStream() {
        String tk = token.get();
        if (tk == null) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/stream"))
                    .header("Authorization", "Bearer " + tk)
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() == 401) {
                token.set(null);
                return false;
            }
            if (resp.statusCode() != 200) {
                System.err.println("[EssentialPatcher] SSE stream rejected: " + resp.statusCode());
                return false;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                String line;
                while (!closing.get() && (line = r.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        try {
                            handleEvent(GSON.fromJson(line.substring(6), JsonObject.class));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            if (!closing.get()) {
                System.err.println("[EssentialPatcher] SSE stream ended: " + e.getMessage());
            }
            return false;
        }
    }

    private static long backoffMs(int attempt) {
        return RETRY_BACKOFF_MS[Math.min(Math.max(attempt, 0), RETRY_BACKOFF_MS.length - 1)];
    }

    private static boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void ensureAuthenticated() throws Exception {
        if (token.get() != null) return;
        if (profileProvider == null) throw new IllegalStateException("LocalProfileProvider not set");

        if (!authenticating.compareAndSet(false, true)) {
            long deadline = System.currentTimeMillis() + 15_000L;
            while (authenticating.get() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            if (token.get() != null) return;
            if (!authenticating.compareAndSet(false, true)) {
                throw new IOException("auth busy");
            }
        }
        try {
            UUID uuid = profileProvider.uuid();
            String username = profileProvider.username();
            if (uuid == null || username == null || username.isEmpty()) {
                throw new IllegalStateException("no local profile");
            }

            String body = "{\"uuid\":\"" + escape(uuid.toString()) + "\",\"username\":\"" + escape(username) + "\"}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IOException("auth/login: " + resp.statusCode() + " " + resp.body());
            }
            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            token.set(json.get("token").getAsString());
        } finally {
            authenticating.set(false);
        }
    }

    private void joinSession(String sid) throws Exception {
        String body = "{\"session_id\":\"" + escape(sid) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/session/join"))
                .header("Authorization", "Bearer " + token.get())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            token.set(null);
            throw new IOException("session/join: unauthorized");
        }
        if (resp.statusCode() != 200) {
            throw new IOException("session/join: " + resp.statusCode() + " " + resp.body());
        }
        sessionId.set(sid);

        JsonObject obj = GSON.fromJson(resp.body(), JsonObject.class);
        if (obj.has("snapshot")) {
            for (JsonElement el : obj.getAsJsonArray("snapshot")) {
                applyPeer(el.getAsJsonObject());
            }
        }
    }

    private void handleEvent(JsonObject ev) {
        String type = ev.has("type") ? ev.get("type").getAsString() : "";
        switch (type) {
            case "cosmetic_changed" -> applyPeer(ev);
            case "cosmetic_trigger" -> applyTrigger(ev);
            case "player_joined" -> {
                UUID uuid = UUID.fromString(ev.get("uuid").getAsString());
                scheduler.submit(() -> fetchAndApply(uuid));
            }
            case "player_left" -> {
                try {
                    UUID uuid = UUID.fromString(ev.get("uuid").getAsString());
                    sessionPeers.remove(uuid);
                } catch (Exception ignored) {
                }
            }
            default -> { }
        }
    }

    private void applyTrigger(JsonObject obj) {
        try {
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            if (profileProvider != null && uuid.equals(profileProvider.uuid())) return;
            String slot = obj.has("slot") ? obj.get("slot").getAsString() : null;
            String triggerName = obj.has("trigger") ? obj.get("trigger").getAsString() : null;
            if (slot == null || triggerName == null) return;

            Essential.getInstance().getCosmeticEventEmitter()
                    .triggerEvent(uuid, CosmeticSlot.Companion.of(slot), triggerName);
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] applyTrigger failed: " + e.getMessage());
        }
    }

    private void applyPeer(JsonObject obj) {
        try {
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            if (profileProvider != null && uuid.equals(profileProvider.uuid())) return;
            SyncedCosmeticOutfit outfit = parseOutfit(obj);
            sessionPeers.put(uuid, outfit);
            CosmeticSyncData.applyRemoteOutfit(uuid, outfit);
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] applyPeer failed: " + e.getMessage());
        }
    }

    public SyncedCosmeticOutfit getPeerCosmetics(UUID uuid) {
        return sessionPeers.get(uuid);
    }

    public void fetchPeerAsync(UUID uuid) {
        if (!joined.get()) return;
        scheduler.submit(() -> fetchAndApply(uuid));
    }

    private void fetchAndApply(UUID uuid) {
        String tk = token.get();
        if (tk == null) return;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/cosmetics/" + uuid))
                    .header("Authorization", "Bearer " + tk)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                applyPeer(GSON.fromJson(resp.body(), JsonObject.class));
            }
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] fetch peer failed: " + e.getMessage());
        }
    }

    private void heartbeat() {
        if (!joined.get()) return;
        String tk = token.get();
        if (tk == null) return;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/session/heartbeat"))
                    .header("Authorization", "Bearer " + tk)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 401) {
                token.set(null);
                restartSession("heartbeat unauthorized");
                return;
            }
            if (resp.statusCode() != 200) {
                onHeartbeatFailure("heartbeat: " + resp.statusCode());
                return;
            }
            heartbeatFailures.set(0);
        } catch (Exception e) {
            onHeartbeatFailure("heartbeat: " + e.getMessage());
        }
    }

    private void onHeartbeatFailure(String reason) {
        if (heartbeatFailures.incrementAndGet() >= MAX_HEARTBEAT_FAILURES) {
            restartSession(reason);
        }
    }

    private void restartSession(String reason) {
        String ki = lastKeyIngredient;
        if (ki == null || !isEnabled()) return;
        System.err.println("[EssentialPatcher] http sync reconnecting: " + reason);
        onServerJoin(ki, lastLabel != null ? lastLabel : "reconnect");
    }

    private void pushCosmetics(SyncedCosmeticOutfit outfit) throws Exception {
        String tk = token.get();
        if (tk == null) return;
        String body = GSON.toJson(Map.of("equipped", outfit.equipped(), "settings", outfit.settings()));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/cosmetics"))
                .header("Authorization", "Bearer " + tk)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            token.set(null);
            restartSession("cosmetics push unauthorized");
            return;
        }
        if (resp.statusCode() != 200) {
            throw new IOException("PUT /api/cosmetics: " + resp.statusCode() + " " + resp.body());
        }
    }

    private static SyncedCosmeticOutfit parseOutfit(JsonObject obj) {
        Map<String, String> equipped = new HashMap<>();
        JsonObject equippedJson = obj.has("equipped") && obj.get("equipped").isJsonObject()
                ? obj.getAsJsonObject("equipped")
                : new JsonObject();
        for (String slot : equippedJson.keySet()) {
            equipped.put(slot, equippedJson.get(slot).getAsString());
        }

        Map<String, List<String>> settings = new HashMap<>();
        JsonObject settingsJson = obj.has("settings") && obj.get("settings").isJsonObject()
                ? obj.getAsJsonObject("settings")
                : new JsonObject();
        for (String cosmeticId : settingsJson.keySet()) {
            JsonElement value = settingsJson.get(cosmeticId);
            if (!value.isJsonArray()) continue;
            List<String> cosmeticSettings = new ArrayList<>();
            JsonArray array = value.getAsJsonArray();
            for (JsonElement setting : array) {
                if (setting.isJsonPrimitive() && setting.getAsJsonPrimitive().isString()) {
                    cosmeticSettings.add(setting.getAsString());
                }
            }
            settings.put(cosmeticId, cosmeticSettings);
        }

        return new SyncedCosmeticOutfit(equipped, settings);
    }

    private void pushTrigger(String slot, String triggerName) throws Exception {
        String tk = token.get();
        if (tk == null) return;
        String body = "{\"slot\":\"" + escape(slot) + "\",\"trigger\":\"" + escape(triggerName) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/trigger"))
                .header("Authorization", "Bearer " + tk)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("POST /api/trigger: " + resp.statusCode() + " " + resp.body());
        }
    }

    private static String computeSessionId(String ingredient, String label) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(ingredient.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 12; i++) sb.append(String.format("%02x", d[i]));
            return label + ":" + sb;
        } catch (Exception e) {
            return label + ":" + Integer.toHexString(ingredient.hashCode());
        }
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
