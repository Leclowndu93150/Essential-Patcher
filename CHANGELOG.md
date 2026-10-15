# Changelog

## 1.0.9

- Added Minecraft 26.3 (Fabric), built against Essential 1.5.1.
- 1.21.11 Fabric: updated to Essential 1.5.1. Other Minecraft versions are unchanged and still target Essential 1.4.1.1.
- Fixed: the game crashed on startup with Essential 1.5.1 (`NoClassDefFoundError: gg/essential/event/sps/SPSStartEvent`). Essential removed its SPS event and manager; cosmetic sync for hosted worlds now keys off the world being opened to guests instead.
- Retargeted the telemetry patches: Essential 1.5.1 moved hardware, display and auto-update telemetry into `TelemetryGatherer`.
- Retargeted the shader glint fix to the new particle render signature.
- "Disable auto-update" wrote to a file the Essential loader never reads. It now sets `autoUpdate=false` in `essential/loader/stage1/fabric/stage2.fabric_<version>.properties` and in `essential/essential-loader.properties`, which controls Essential mod updates. It takes effect from the next launch.
- Fixed: cosmetic sync could stop pushing and heartbeating after a quick rejoin, when the previous session thread cleared the joined flag of the new one. Applies to every version.
- Sale, banner, promo-toast and "new cosmetic" notice toggles (#18) were broken by Essential 1.5: it rebuilt those notices on a shared notice state and removed the methods the patcher hooked. They now hook the new state getters and the toast push, alongside the single notice filter.
- Fixed: the 1.21.2 jar only showed up for 1.21.2 and the 1.21.9 jar only for 1.21.9 (#7). They already ran on 1.21.2–1.21.8 and 1.21.9–1.21.10; uploads are now tagged with every version they support. The 26.1.2 jar is also tagged for 26.1 and 26.1.1.
- Shader glint corruption with particle cosmetics on 1.19.2, 1.20.1 and 1.21.1 (#19): the old fix wrapped the step where Essential queues particles, but the particles are drawn later, outside it. The Iris/Oculus state isolation now wraps the actual draw, and the shader texture Essential's particle renderer overwrites on its way out (it restores slot 0 from whatever texture is bound on the active unit) is put back. Not yet confirmed in-game with Oculus.
- Probable fix for skins reverting to a default skin (#15): restoring your saved cosmetics, and applying another patcher user's synced cosmetics, rebuilt the outfit with no skin, which drops Essential's skin override. The current skin is now kept. Not yet confirmed by the reporter.
- Cosmetic sync now treats `server.net` and `server.net:25565` as the same server, so players who saved the address differently end up in the same sync session (possible cause of #17, not confirmed).
- Added "Hide Menu Paperdoll" (#16): removes the player model Essential draws on the title and pause screens. Off by default.
- Added "Hide Essential Cosmetics" (#3): strips the wardrobe down to Outfits, Skins and Capes. Off by default.
- Added "Show Delisted Cosmetics" (#14): asks Essential for the cosmetic IDs listed by the sync server's new `GET /api/catalog/cosmetics` endpoint, so cosmetics that are no longer sold load and get unlocked like the rest. The list is whatever the server operator puts in `COSMETIC_CATALOG_FILE`.

## 1.0.8

- Added Mod Menu integration on Fabric. The config screen existed but nothing on Fabric ever exposed it, so the Configure button did nothing — on 1.21.2 and up, which are Fabric-only, there was no way to reach the settings at all. Works on every Fabric version now.
- Added 1.19.2 for Forge and Fabric (#13). YACL has no 1.19.2 build, so the config button opens `config/essentialpatcher.json` instead of a settings screen.
- Fixed: installing from Modrinth failed with a 404 on a dependency (#6). Uploads pointed at a dead Modrinth project for Essential — the CurseForge slug `essential-mod` was being sent to both platforms, but Modrinth's is `essential`.
- Updated to Essential 1.4.1.1.
- Fixed: "Skip Community Rules" made friend requests and joining friends' worlds fail (#12). It only pretended the rules were accepted, so Essential's servers never got the agreement and kept rejecting anything social. It now agrees for you instead of just hiding the popup.
- Retargeted the wiki toast and telemetry patches, which 1.4.1.1 moved out from under them.
- Cosmetic sync now recovers on its own instead of dying silently (#8). A failed join is retried with backoff rather than giving up until you change worlds, the event stream reconnects when it drops, and dead heartbeats now trigger a rejoin instead of leaving the client pushing into an expired session.
