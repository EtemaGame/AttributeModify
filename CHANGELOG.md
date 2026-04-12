# Changelog

## [1.0.9] - Forge 1.20.1

### New Features
- **Automatic Slot Inference**: Skip specifying slots for armor, tools/weapons, and Curios. The system now detects them automatically based on item type and tags.
- **Shorthand JSON Syntax**: Support for defining single attributes or `attributes` arrays at the top level of the item object, simplifying configuration files.
- **Apotheosis Parity**: Added `apotheosis_sync` trigger and improved API integration for better compatibility with Apotheosis rarity systems.
- **Cross-Version Compatibility**: Added a bridge for `component:apotheosis:rarity` in NBT conditions, allowing the same JSON files to work on both 1.20.1 (NBT) and 1.21.1 (Components).

## [1.0.8] - Forge 1.20.1

## [1.0.7] - Forge 1.20.1 / [1.0.3] - NeoForge 1.21.1

### New Features
- **Multi-File Accumulation**: Multiple datapack files can now contribute attributes to the same item. Previously, the last file loaded would overwrite all others. Now entries from all files are merged together.
- **ListTag Index Navigation**: NBT condition paths now support numeric indices to access elements inside list tags (e.g., `enchantments.0.id`).
- **`not_equals` returns true on missing paths**: The `not_equals` operator now correctly returns `true` when the NBT path doesn't exist on the item, matching the intuitive "this value is not X" behavior.

### Bug Fixes
- **Villager Trade Quality Template Mutation**: Fixed a critical bug where applying quality to a villager trade would permanently modify the trade offer template, preventing re-rolls on future trades. Quality is now only applied via deferred inventory scan.
- **Network Buffer Desync (Forge)**: Fixed packet encode/decode that could desync when items with null registry IDs were included, causing all subsequent data in the packet to be corrupted.
- **Container Close Inventory Scan**: The container close event now correctly skips the player's own inventory menu, preventing unnecessary quality re-processing when simply closing the inventory screen.
- **Regex Pattern Safety**: Added `PatternSyntaxException` catch for the `matches_regex` NBT operator, preventing server crashes from malformed regex patterns in datapacks.
- **Long Precision in Quality Values**: Fixed quality system NBT writing to properly distinguish between int, long, and double values, preventing precision loss for large numbers.
- **Null NBT Condition Serialization (Forge)**: Fixed network packet encoding to correctly handle null NBT condition values using a sentinel, preventing deserialization errors on the client.

### Performance & Stability
- **Thread Safety (Forge)**: All attribute maps now use `ConcurrentHashMap` and `volatile` instance fields, preventing potential race conditions during reload.
- **Debug Logging Optimization**: Changed all hot-path logging from `info` to `debug` level across both versions, reducing log spam during normal gameplay.
- **Removed Dead Code (Forge)**: Cleaned up unused `handleCuriosAttributes()` method and related imports.

### Internal
- Unified both Forge and NeoForge codebases to feature parity.
- Extracted NbtCondition encode/decode into reusable helper methods (Forge).

---

## [1.0.5] - Forge / [1.0.0] - NeoForge (Previous Release)

- Initial multi-platform release with all core features.
