# Changelog

## 1.0.8

- Added Mod Menu integration on Fabric. The config screen existed but nothing on Fabric ever exposed it, so the Configure button did nothing — on 1.21.2 and up, which are Fabric-only, there was no way to reach the settings at all. Works on every Fabric version now.
- Added 1.19.2 for Forge and Fabric (#13). YACL has no 1.19.2 build, so the config button opens `config/essentialpatcher.json` instead of a settings screen.
- Fixed: installing from Modrinth failed with a 404 on a dependency (#6). Uploads pointed at a dead Modrinth project for Essential — the CurseForge slug `essential-mod` was being sent to both platforms, but Modrinth's is `essential`.
- Updated to Essential 1.4.1.1.
- Fixed: "Skip Community Rules" made friend requests and joining friends' worlds fail (#12). It only pretended the rules were accepted, so Essential's servers never got the agreement and kept rejecting anything social. It now agrees for you instead of just hiding the popup.
- Retargeted the wiki toast and telemetry patches, which 1.4.1.1 moved out from under them.
- Cosmetic sync now recovers on its own instead of dying silently (#8). A failed join is retried with backoff rather than giving up until you change worlds, the event stream reconnects when it drops, and dead heartbeats now trigger a rejoin instead of leaving the client pushing into an expired session.
