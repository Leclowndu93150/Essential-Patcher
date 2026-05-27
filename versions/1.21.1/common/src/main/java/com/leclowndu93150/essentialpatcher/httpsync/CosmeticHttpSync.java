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
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP-side cosmetic sync against cosmetics.leclowndu93150.dev. Independent of the
 * in-game packet sync (which only works when both peers run the patcher and the MC
 * server forwards packets). This one works on any vanilla server because it goes
 * through our own HTTPS service.
 *
 * Per-version code creates a single instance and calls:
 *   - {@link #setMojangJoiner(MojangJoiner)}   wires the version-specific Mojang call
 *   - {@link #onServerJoin(String, String)}    when entering a server/LAN/SP world
 *   - {@link #onServerLeave()}                 when leaving
 *   - {@link #onLocalCosmeticChange(Map)}      when the local outfit changes
 */
public final class CosmeticHttpSync {

    public interface MojangJoiner {
        /** Calls Minecraft.getInstance().getMinecraftSessionService().joinServer(...). */
        void joinServer(String serverId) throws Exception;

        /** The player's username from Minecraft.getInstance().getUser().getName(). */
        String username();

        /** The player's UUID from Minecraft.getInstance().getUser().getProfileId(). */
        UUID uuid();
    }

    private static final Gson GSON = new Gson();
    private static final CosmeticHttpSync INSTANCE = new CosmeticHttpSync();

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

    private volatile MojangJoiner joiner;
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicBoolean joined = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private ScheduledFuture<?> heartbeatTask;
    private Thread sseThread;

    private volatile String lastKeyIngredient;
    private volatile String lastLabel;
    private final ConcurrentHashMap<UUID, SyncedCosmeticOutfit> sessionPeers = new ConcurrentHashMap<>();

    public void setMojangJoiner(MojangJoiner j) {
        this.joiner = j;
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

    /**
     * Called when the player joins a server/LAN/SP. {@code keyIngredient} is mixed into
     * the session_id hash so different servers/worlds get different broadcast groups.
     * {@code label} is a short tag like "smp", "lan", "sp" appended to the hash for
     * easier debugging (not security-relevant).
     */
    public void onServerJoin(String keyIngredient, String label) {
        if (!isEnabled() || joiner == null) return;
        if (joined.get()) {
            closeSession(false);
        }
        closing.set(false);
        lastKeyIngredient = keyIngredient;
        lastLabel = label;
        String sid = computeSessionId(keyIngredient, label);
        scheduler.submit(() -> {
            try {
                authenticate();
                joinSession(sid);
                openSseStream();
                heartbeatTask = scheduler.scheduleAtFixedRate(this::heartbeat, 30, 30, TimeUnit.SECONDS);
                joined.set(true);

                pushCurrentOutfit();
                scheduler.schedule(this::pushCurrentOutfit, 2, TimeUnit.SECONDS);
                scheduler.schedule(this::pushCurrentOutfit, 8, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("[EssentialPatcher] http sync join failed: " + e.getMessage());
                joined.set(false);
            }
        });
    }

    public void onServerLeave() {
        closeSession(true);
    }

    private void closeSession(boolean notifyServer) {
        if (!joined.getAndSet(false)) return;
        closing.set(true);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (sseThread != null) {
            sseThread.interrupt();
            sseThread = null;
        }
        sessionPeers.clear();
        String tk = token.get();
        String sid = sessionId.get();
        if (!notifyServer) {
            token.compareAndSet(tk, null);
            sessionId.compareAndSet(sid, null);
            return;
        }
        if (tk == null) {
            sessionId.compareAndSet(sid, null);
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
            token.compareAndSet(tk, null);
            sessionId.compareAndSet(sid, null);
        });
    }

    /** Hook called from InfraEquippedOutfitsMixin after the local outfit is saved. */
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

    /** Hook called from EmoteWheelMixin when the user clicks an emote in the wheel. */
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

    // ---------- internals ----------

    private void authenticate() throws Exception {
        if (joiner == null) throw new IllegalStateException("MojangJoiner not set");
        String username = joiner.username();
        if (username == null) throw new IllegalStateException("no username");

        HttpRequest begin = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/auth/begin"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + escape(username) + "\"}"))
                .build();
        HttpResponse<String> beginResp = http.send(begin, HttpResponse.BodyHandlers.ofString());
        if (beginResp.statusCode() != 200) {
            throw new IOException("auth/begin: " + beginResp.statusCode() + " " + beginResp.body());
        }
        JsonObject beginJson = GSON.fromJson(beginResp.body(), JsonObject.class);
        String serverId = beginJson.get("server_id").getAsString();

        joiner.joinServer(serverId);

        String body = "{\"server_id\":\"" + escape(serverId) + "\",\"username\":\"" + escape(username) + "\"}";
        HttpRequest finish = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/auth/finish"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> finishResp = http.send(finish, HttpResponse.BodyHandlers.ofString());
        if (finishResp.statusCode() != 200) {
            throw new IOException("auth/finish: " + finishResp.statusCode() + " " + finishResp.body());
        }
        JsonObject finishJson = GSON.fromJson(finishResp.body(), JsonObject.class);
        token.set(finishJson.get("token").getAsString());
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
        if (resp.statusCode() != 200) {
            throw new IOException("session/join: " + resp.statusCode() + " " + resp.body());
        }
        sessionId.set(sid);

        JsonObject obj = GSON.fromJson(resp.body(), JsonObject.class);
        if (obj.has("snapshot")) {
            for (var el : obj.getAsJsonArray("snapshot")) {
                applyPeer(el.getAsJsonObject());
            }
        }
    }

    private void openSseStream() {
        String tk = token.get();
        if (tk == null) return;
        sseThread = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/stream"))
                        .header("Authorization", "Bearer " + tk)
                        .header("Accept", "text/event-stream")
                        .timeout(Duration.ofHours(1))
                        .GET()
                        .build();
                HttpResponse<java.io.InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) {
                    System.err.println("[EssentialPatcher] SSE stream rejected: " + resp.statusCode());
                    return;
                }
                try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while (!closing.get() && (line = r.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            try {
                                handleEvent(GSON.fromJson(line.substring(6), JsonObject.class));
                            } catch (Exception e) {
                                // ignore one malformed event
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (!closing.get()) {
                    System.err.println("[EssentialPatcher] SSE stream ended: " + e.getMessage());
                }
            }
        }, "EssentialPatcher-SSE");
        sseThread.setDaemon(true);
        sseThread.start();
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
            if (joiner != null && uuid.equals(joiner.uuid())) return;
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
            if (joiner != null && uuid.equals(joiner.uuid())) return;
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
                String ki = lastKeyIngredient;
                String lb = lastLabel;
                if (ki != null) {
                    closeSession(false);
                    onServerJoin(ki, lb != null ? lb : "reauth");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void pushCosmetics(SyncedCosmeticOutfit outfit) throws Exception {
        String tk = token.get();
        if (tk == null) return;
        String body = GSON.toJson(Map.of("equipped", outfit.equipped(), "settings", outfit.settings()));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/cosmetics"))
                .header("Authorization", "Bearer " + tk)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
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
