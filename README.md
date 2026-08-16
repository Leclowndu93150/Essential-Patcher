<div align="center">
  <img src="icon.png" alt="Essential Patcher" width="128">
  <h1>Essential Patcher</h1>
  <a href="https://www.curseforge.com/minecraft/mc-mods/essential-patcher">Download on CurseForge</a>
</div>

<div style="border: 2px solid #d1242f; border-radius: 8px; padding: 14px 16px; background: #fff1f1; color: #5f0f14; margin: 16px 0;">
  <strong>WARNING:</strong> Essential Patcher is unofficial and may conflict with Essential's terms of use or other platform rules. It does not include Essential or Minecraft code/assets, does not modify Essential's servers, and does not forge purchases or account entitlements. Read the full legal position before using or redistributing it: <a href="./LEGALITY.md"><strong>LEGALITY.md</strong></a>.
</div>

Essential Patcher is a client-side Minecraft mod that patches the Essential mod at runtime.

It is built for users who want more control over Essential's cosmetics, telemetry, ads, purchase prompts, and client-side behavior.

## What It Does

- Unlocks cosmetics locally on your client.
- Saves your equipped cosmetics locally.
- Syncs cosmetics with other Essential Patcher users through an independent HTTPS sync service.
- Blocks Essential telemetry and analytics calls.
- Removes ads, promos, notices, purchase prompts, and checkout flows.
- Fixes some Essential cosmetic sync crashes.
- Keeps the patcher separate from Essential's servers and account entitlements.

## How It Works

Essential Patcher uses Mixins to change Essential while Minecraft is running.

The mod does not edit Essential's JAR. It loads alongside Essential, intercepts selected client-side methods, and changes the result locally.

For cosmetics, the patcher stores your selected cosmetic IDs and settings, sends them to the patcher sync service, and receives cosmetic data from other patcher users in the same session. Only users running Essential Patcher can see this synced cosmetic data.

For privacy, the patcher blocks known telemetry, analytics, and tracking paths before they leave the client.

For ads and purchases, the patcher removes or blocks local UI and packet paths related to promos, coin purchases, checkout, and shop prompts.

## What It Does Not Do

- It does not include Essential's code.
- It does not include Essential's assets, cosmetics, models, textures, or icons.
- It does not include Minecraft's code or assets.
- It does not distribute a modified Essential JAR.
- It does not distribute a modified Minecraft JAR.
- It does not create Coins.
- It does not submit fake purchases.
- It does not modify Essential account ownership records.
- It does not make non-patcher users see patched cosmetics.

## Cosmetic Sync Server -- Privacy (GDPR)

Essential Patcher runs an independent cosmetic sync service at `cosmetics.leclowndu93150.dev` so patcher users can see each other's cosmetics. Here is exactly what is stored and what is not.

### What is stored

| Data | Purpose | Retention |
|------|---------|-----------|
| Minecraft UUID | Identify which player owns which cosmetic loadout | Until 90 days of inactivity, then auto-pruned |
| Equipped cosmetic slot IDs and cosmetic IDs | Sync your cosmetic outfit to other patcher users | Same as above |
| Cosmetic settings (per-cosmetic JSON) | Sync cosmetic customization (colors, variants) | Same as above |
| Last-seen timestamp | Prune inactive records | Same as above |

### What is NOT stored

- No IP addresses are logged or stored.
- No Minecraft access tokens or session tokens are stored. The Mojang session auth is a one-time handshake to verify you own the account; the token is never saved.
- No email addresses, passwords, or account credentials.
- No playtime, server history, or gameplay data.
- No chat messages, friend lists, or social data.
- No hardware info, OS info, or device fingerprints.
- No analytics, telemetry, or tracking of any kind.

### How authentication works

1. The client requests a challenge (random server ID) from the sync server.
2. The client calls Mojang's `joinServer` with that challenge to prove account ownership.
3. The sync server calls Mojang's `hasJoined` to verify.
4. On success, the server issues a short-lived JWT containing only your UUID. No Mojang tokens are retained.

### Data deletion

All player data is automatically deleted after 90 days of inactivity. To request immediate deletion, open an issue on this repository with your Minecraft UUID.

### Data location

The sync server runs on an OVH VPS located in France (EU). All data is subject to EU/GDPR regulations.

## Supported Versions

| Minecraft | Loaders |
|-----------|---------|
| 26.1.2 | Fabric |
| 1.21.11 | Fabric |
| 1.21.9 | Fabric |
| 1.21.2 | Fabric |
| 1.21.1 | NeoForge, Fabric |
| 1.20.1 | Forge, Fabric |
| 1.19.2 | Forge, Fabric |

## Requirements

- Essential 1.4.1.1
- YACL (Yet Another Config Lib) — not required on 1.19.2, which has no YACL build; there the config
  button opens `config/essentialpatcher.json` instead of a settings screen.

## Building

```bash
./gradlew build
```

Build outputs are generated under the Gradle build directories for the supported loaders.

## Status

This project is experimental and depends on Essential internals. Essential updates can break Mixins, cosmetic sync, telemetry blocking, or UI patches.

## Disclaimer

Essential Patcher is not affiliated with, endorsed by, approved by, or associated with Essential, ModCore Inc., Spark Universe, Mojang, Microsoft, or Minecraft.

See [LEGALITY.md](./LEGALITY.md) for the full legal and technical position.
