# CLAUDE.md

Essential Patcher: a client-side Minecraft mod that patches the Essential mod at runtime with Mixins
(cosmetic unlock, telemetry/ads/purchase blocking), plus a small Python cosmetic-sync server.
User-facing docs: `README.md`. Legal position: `LEGALITY.md`. Release notes: `CHANGELOG.md`.

## Layout

```
build.gradle.kts        per-version deps, loader versions, publishing (dev.prism plugin)
settings.gradle.kts     which Minecraft versions / loaders exist
versions/<mc>/common    shared Java code + mixins for that MC version
versions/<mc>/fabric    Fabric entrypoints, Mod Menu, fabric.mod.json
versions/<mc>/forge     Forge entrypoint + META-INF/mods.toml        (1.19.2, 1.20.1)
versions/<mc>/neoforge  NeoForge entrypoint + neoforge.mods.toml     (1.21.1)
server/                 cosmetic-sync backend (FastAPI + SQLite + SSE), see server/README.md
```

Versions: 1.19.2, 1.20.1, 1.21.1, 1.21.2, 1.21.9, 1.21.11, 26.1.2, 26.3. Java package root:
`com.leclowndu93150.essentialpatcher` (`config/`, `cosmetics/`, `httpsync/`, `network/`, `compat/`,
`mixin/{cosmetics,gui,network,notices,render,telemetry}`).

## Per-version source is copied, not shared

Each `versions/<mc>/common` tree is a full copy of the same 38 Java files. They are near-identical: for
example 1.21.1 vs 1.21.11 differ only in `mixin/render/HumanoidArmorLayer*`, and 1.19.2 vs 1.20.1 only
in `config/PatcherConfig*` (1.19.2 has no YACL, so its config button opens
`config/essentialpatcher.json`).

- A bug fix or feature in shared logic (sync, config, a mixin) must be applied to every version
  tree that has the file. Diff the trees (`diff -rq versions/A/common/src versions/B/common/src`)
  before and after to confirm only the intended version-specific differences remain.
- Adding a config option means editing `PatcherConfig.java` and `PatcherConfigScreen.java` in
  every version.

## Mixins

- Targets are Essential internals (`gg.essential.*`), not Minecraft. Essential updates move or rename
  them and break mixins; retarget against the current Essential jar.
- Every mixin must be listed in `versions/<mc>/common/src/main/resources/essentialpatcher.mixins.json`
  (no auto-detect). `compatibilityLevel` is `JAVA_17` for 1.19.2/1.20.1, `JAVA_21` for the rest.
- Fabric uses `mixins { disableAutoDetect() }` and registers the config in
  `EssentialpatcherPreLaunch` (`Mixins.addConfiguration`), so it applies after Essential's own
  preLaunch. Forge/NeoForge register it in `mods.toml` / `neoforge.mods.toml`.
- Each patch is gated by a boolean in `PatcherConfig` (persisted to `config/essentialpatcher.json`).

## Build

```bash
./gradlew build
```

- Essential jars are compile-time inputs that are NOT in the repo (`libs/essential*.jar` is
  gitignored). Put them in `libs/` before building: `essential-1.19.2.jar`, `essential-1.20.1.jar`,
  `essential.jar` (1.21.1, 1.21.2), `essential-1.21.9.jar` (1.21.9, Essential 1.4.1.1),
  `essential-1.21.11.jar` (1.21.11, Essential 1.5.1),
  `essential-26.1.2.jar`, `essential-26.3.jar` (Essential 1.5.1).
- Required Essential version is in `README.md` (1.5.1 for 1.21.11 and 26.3, 1.4.1.1 elsewhere).
- Other deps (YACL, Mod Menu, Essential runtime) come from CurseForge Maven via `curse.maven:*`.
- Mod `version` and the publish `changelog` string live in `build.gradle.kts`; keep `CHANGELOG.md`
  in sync. Publishing reads `CURSEFORGE_TOKEN` / `MODRINTH_TOKEN` from the environment.
- There are no automated tests and no CI. Verify by building and running the game.

## Cosmetic sync (client side)

`httpsync/CosmeticHttpSync` is a singleton talking to `httpCosmeticSyncBaseUrl`
(default `https://cosmetics.leclowndu93150.dev`):
login -> JWT, `POST /api/session/join` with a locally hashed session id (`SessionKey`), SSE
`GET /api/stream`, heartbeat every ~30 s, `PUT /api/cosmetics` on wardrobe change. 401 clears the
token and rejoins; repeated heartbeat failures trigger a rejoin with backoff.
`EssentialSpsSyncListener` hooks Essential SPS hosting into the same flow. Received outfits are
applied via `network/CosmeticSyncData`.

## Server (`server/`)

Python FastAPI app in `server/cosmeticsync/` (`app.py` routes, `auth.py` JWT, `sessions.py` SSE hub,
`storage.py` SQLite, `ratelimit.py`, `metrics_client.py`). Config is env vars read in `config.py`;
`JWT_SECRET` is required. Run locally:

```bash
cd server && pip install -r requirements.txt
JWT_SECRET=dev-secret-dev-secret-dev-secret COSMETICSYNC_DB=/tmp/cosmetics.db \
  python -m uvicorn cosmeticsync.app:app --reload --port 7100
```

Deploy is `server/ops/deploy.sh` to a production VPS: do not run it without explicit approval.
The API contract (endpoints, SSE event shapes) is documented in `server/README.md`; client and
server change together.

## Constraints

- Privacy promises in `README.md` are user-facing commitments: the client must never read or send
  Minecraft/Microsoft tokens, and the server stores only UUID, equipped cosmetics, settings and
  timestamps. Changes that add stored or transmitted data must update the README privacy section.
- Never bundle Essential or Minecraft code/assets, and never forge purchases or entitlements
  (see `LEGALITY.md`).
- Server auth is not verified against Mojang yet (known gap, documented in both READMEs).
