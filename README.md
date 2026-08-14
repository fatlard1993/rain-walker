# Rain Walker

A Fabric mod that adds the **Rain Walker** boot enchantment: conjure a fleeting ice platform under your feet whenever you're caught running or falling in the rain, so you can sprint across open ground without slowing down or taking fall damage.

## Features

- New treasure enchantment for boots: found in loot, not obtainable at the enchanting table
- Creates a temporary ice platform under your feet when you're exposed to open sky and it's raining
- Prevents fall damage while the ice platform is placed
- Ice disappears after roughly 1-2 seconds and leaves no water behind
- Mutually exclusive with Frost Walker and Depth Strider

## Requirements

Targets the Minecraft, Fabric Loader, and Fabric API versions declared in this mod's `gradle.properties`; check there for the exact currently-supported version.

## Pandorical

Rain Walker is a server-side mod. If Pandorical is installed on the server, the mod uses `PandoricalApi.content().registerModAssets()` to sync its translations and other client assets (such as the enchantment's display name) to Pandorical clients. This is the only Pandorical usage; Rain Walker has no custom UI. Pandorical is not required for the enchantment's gameplay effects to work; it only affects whether a client sees the proper localized name for the enchantment.

## Installation

**Server-side**: install alongside its declared dependencies (see `fabric.mod.json`). Clients do not need any mod installed to experience the enchantment's effects.

## License

MIT License - see [LICENSE](LICENSE)
