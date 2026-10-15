# Changelog

## 1.0.9

- Updated to Essential 1.5.0.1 (#18). 1.5.0 rebuilt sale, banner, promo-toast and "new cosmetic" notices on top of a shared notice state and deleted the methods the patcher hooked, so those four toggles silently stopped doing anything. They now hook the new state getters and the toast push instead.
- Added 26.3 (Fabric).
- Fixed: the 1.21.2 jar only showed up for 1.21.2 and the 1.21.9 jar only for 1.21.9 (#7). They already ran on 1.21.2–1.21.8 and 1.21.9–1.21.10; uploads are now tagged with every version they support. The 26.1.2 jar is also tagged for 26.1 and 26.1.1.
- Shader glint corruption with particle cosmetics on 1.19.2, 1.20.1 and 1.21.1 (#19): the old fix wrapped the step where Essential queues particles, but the particles are drawn later, outside it. The Iris/Oculus state isolation now wraps the actual draw, and the shader texture Essential's particle renderer overwrites on its way out (it restores slot 0 from whatever texture is bound on the active unit) is put back. Not yet confirmed in-game with Oculus.
- Probable fix for skins reverting to a default skin (#15): restoring your saved cosmetics, and applying another patcher user's synced cosmetics, rebuilt the outfit with no skin, which drops Essential's skin override. The current skin is now kept. Not yet confirmed by the reporter.
- Cosmetic sync now treats `server.net` and `server.net:25565` as the same server, so players who saved the address differently end up in the same sync session (possible cause of #17, not confirmed).
- Added "Hide Menu Paperdoll" (#16): removes the player model Essential draws on the title and pause screens. Off by default.
- Added "Hide Essential Cosmetics" (#3): strips the wardrobe down to Outfits, Skins and Capes. Off by default.
- Added "Show Delisted Cosmetics" (#14): asks Essential for the cosmetic IDs listed by the sync server's new `GET /api/catalog/cosmetics` endpoint, so cosmetics that are no longer sold load and get unlocked like the rest. The list is whatever the server operator puts in `COSMETIC_CATALOG_FILE`.
