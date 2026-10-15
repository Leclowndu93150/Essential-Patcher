# Changelog

## 1.0.9

- Added Minecraft 26.3 (Fabric), built against Essential 1.5.1.
- 1.21.11 Fabric: updated to Essential 1.5.1. Other Minecraft versions are unchanged and still target Essential 1.4.1.1.
- Fixed: the game crashed on startup with Essential 1.5.1 (`NoClassDefFoundError: gg/essential/event/sps/SPSStartEvent`). Essential removed its SPS event and manager; cosmetic sync for hosted worlds now keys off the world being opened to guests instead.
- Retargeted the telemetry patches: Essential 1.5.1 moved hardware, display and auto-update telemetry into `TelemetryGatherer`.
- Notice blocking (sales, banners, toasts, new-cosmetic dots) now relies on the single notice filter. Essential 1.5 removed the per-type hooks the other patches used.
- Retargeted the shader glint fix to the new particle render signature.
- "Disable auto-update" wrote to a file the Essential loader never reads. It now sets `autoUpdate=false` in `essential/loader/stage1/fabric/stage2.fabric_<version>.properties` and in `essential/essential-loader.properties`, which controls Essential mod updates. It takes effect from the next launch.
- Fixed: cosmetic sync could stop pushing and heartbeating after a quick rejoin, when the previous session thread cleared the joined flag of the new one. Applies to every version.

## 1.0.8

- Added Mod Menu integration on Fabric. The config screen existed but nothing on Fabric ever exposed it, so the Configure button did nothing — on 1.21.2 and up, which are Fabric-only, there was no way to reach the settings at all. Works on every Fabric version now.
- Added 1.19.2 for Forge and Fabric (#13). YACL has no 1.19.2 build, so the config button opens `config/essentialpatcher.json` instead of a settings screen.
- Fixed: installing from Modrinth failed with a 404 on a dependency (#6). Uploads pointed at a dead Modrinth project for Essential — the CurseForge slug `essential-mod` was being sent to both platforms, but Modrinth's is `essential`.
- Updated to Essential 1.4.1.1.
- Fixed: "Skip Community Rules" made friend requests and joining friends' worlds fail (#12). It only pretended the rules were accepted, so Essential's servers never got the agreement and kept rejecting anything social. It now agrees for you instead of just hiding the popup.
- Retargeted the wiki toast and telemetry patches, which 1.4.1.1 moved out from under them.
- Cosmetic sync now recovers on its own instead of dying silently (#8). A failed join is retried with backoff rather than giving up until you change worlds, the event stream reconnects when it drops, and dead heartbeats now trigger a rejoin instead of leaving the client pushing into an expired session.
