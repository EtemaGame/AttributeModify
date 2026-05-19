# Changelog

## 1.1.2 - 2026-05-19

- Enabled datapack loading for `quality_system`, `mining`, and `decorative`.
- Kept shorthand `attribute` and `attributes` compatible with automatic slot detection.
- Added `body` as a supported editor/datapack slot alias for Forge 1.20.1, mapped internally to chest behavior.
- Expanded the in-game editor so rules can include target mode, auto/standard/Curios slot type, conditions, durability triggers, mining overrides, quality rolls, and decorative flags.
- Reworked the editor UI toward the previous cleaner panel layout while preserving the new save behavior.
- Reduced repeated warning spam when `modify` rules cannot find an original attribute to replace.
- Cleaned custom durability session tracking when players disconnect.
- Added serialization and validation coverage for mining, quality, decorative, and `body` slot handling.
