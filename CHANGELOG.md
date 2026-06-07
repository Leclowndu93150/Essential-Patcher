# Changelog

## 1.0.4

- Switched HTTP cosmetic sync auth from Mojang `joinServer` to the Minecraft access-token verify endpoint. The old flow hit Mojang's session server hard enough to get user accounts temp-suspended for "high API requests"; the new flow auths once per launcher session, caches the JWT, and never calls `joinServer`.
- Backend kept the old `/api/auth/begin` and `/api/auth/finish` endpoints for backwards compatibility, so older patcher versions still work.
- JWT TTL bumped from 15 minutes to 20 hours so heartbeats stop forcing re-auth mid-session.
- Fixed: Essential's "Show only cosmetic / Show only armor / Show both" settings now work with patcher enabled.
- Fixed: "Hide cosmetics in inventory" toggle is now respected.
