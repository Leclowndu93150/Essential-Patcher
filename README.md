<div align="center">
  <h1>Essential Patcher</h1>
</div>

<div style="border: 2px solid #d1242f; border-radius: 8px; padding: 14px 16px; background: #fff1f1; color: #5f0f14; margin: 16px 0;">
  <strong>WARNING:</strong> Essential Patcher is unofficial and may conflict with Essential's terms of use or other platform rules. It does not include Essential or Minecraft code/assets, does not modify Essential's servers, and does not forge purchases or account entitlements. Read the full legal position before using or redistributing it: <a href="./LEGALITY.md"><strong>LEGALITY.md</strong></a>.
</div>

Essential Patcher is a client-side Minecraft mod for Minecraft 1.21.1 that patches the Essential mod at runtime.

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

## Requirements

- Minecraft: Java Edition 1.21.1
- Essential 1.3.10.9
- NeoForge or Fabric
- Java 21

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
