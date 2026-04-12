# Regression Baseline

This file captures the manual scenarios that should be checked before and after behavior changes. It is not a substitute for in-game screenshots, but it fixes the expected acceptance criteria.

## Manual Scenarios

### Case 1: Vanilla Sword - MODIFY `minecraft:generic.attack_damage`
- Config: `MODIFY`, `ADDITION`, `amount: 10`.
- Expected: the original attack damage modifier keeps UUID and name; only amount/operation change.
- Tooltip: rendered by vanilla/Forge; the tooltip service must not rewrite the line.

### Case 2: Vanilla Armor - MODIFY `minecraft:generic.armor`
- Config: `MODIFY`, `ADDITION`, `amount: 8`.
- Expected: armor modifier identity is preserved and slot order stays stable.
- Tooltip: rendered by vanilla/Forge.

### Case 3: New ADD Attribute - `minecraft:generic.luck`
- Config: `ADD`, `ADDITION`, `amount: 2`.
- Expected: adds a modifier without deleting vanilla modifiers.
- Tooltip: if vanilla/Forge does not render the new attribute, the tooltip service adds one simple vanilla-style line.

### Case 4: REMOVE Base Attribute
- Config: `REMOVE`.
- Expected: removes the target attribute from the event.
- Tooltip: no stale line remains.

### Case 5: Coexistence With Other Mods
- Config: item has extra modifiers from another mod plus a `MODIFY` rule from AttributeModify.
- Expected: only original item modifiers with matching UUIDs are replaced; extra modifiers stay unchanged and in relative order.

### Case 6: MULTIPLY_BASE / MULTIPLY_TOTAL
- Config: `ADD` and `MODIFY` with multiplicative operations.
- Expected: logical operation is applied exactly as configured; tooltip service does not recalculate `MODIFY` totals.

## Asset Slots

Expected screenshot paths when running a manual pass:

- `docs/assets/baseline/sword_modify.png`
- `docs/assets/baseline/armor_modify.png`
- `docs/assets/baseline/add_new_attribute.png`
- `docs/assets/baseline/remove_attribute.png`
- `docs/assets/baseline/multi_mod_conflict.png`

## Acceptance Checklist

- [ ] Manual screenshot captured for sword `MODIFY`.
- [ ] Manual screenshot captured for armor `MODIFY`.
- [ ] Manual screenshot captured for new `ADD`.
- [ ] Manual screenshot captured for `REMOVE`.
- [ ] Manual coexistence check with another modifier source.
- [x] `compileJava` passes after the semantic changes.
- [x] Unit tests for pure tooltip helper logic pass.

