# Attribute Behavior Spec

This document is the functional contract for datapack attribute actions. It separates logical behavior from tooltip presentation so future refactors do not change semantics by accident.

## Action Matrix

| Action | Runtime state | Logical result | Tooltip result |
| :--- | :--- | :--- | :--- |
| `ADD` | Attribute already exists on the item/slot | Adds the datapack modifier without removing existing modifiers. | Vanilla/Forge rendering is trusted. The tooltip service does not merge totals or rewrite existing lines. |
| `ADD` | Attribute does not exist on the item/slot | Adds the datapack modifier as a new attribute for that slot. | If vanilla/Forge does not render the new line, the tooltip service appends a simple vanilla-style `plus/take` line. |
| `MODIFY` | One or more original item modifiers exist for the attribute | Replaces only current modifiers whose UUID matches an original item modifier for that attribute. UUID, name, and relative order are preserved; amount and operation come from the datapack modifier. Extra modifiers from other mods remain untouched and in order. | Vanilla/Forge rendering is trusted. The tooltip service must not rewrite `MODIFY` lines. |
| `MODIFY` | No original item modifier exists for the attribute | No-op. Emits a deferred semantic warning at runtime. | No custom tooltip line is created. |
| `REMOVE` | Attribute exists in the current item/slot modifiers | Removes the whole attribute from the event. | If a stale attribute line remains, the tooltip service hides it. |
| `REMOVE` | Attribute does not exist | No-op. | No custom tooltip line is created. |

## Identity Contract For MODIFY

`MODIFY` is not "remove everything and add one replacement". It reconstructs the final modifier list from the current event state.

For each current modifier of the target attribute:

1. If its UUID is present in `event.getOriginalModifiers().get(attribute)`, create a replacement modifier with the same UUID and name, but with the datapack amount and operation.
2. If its UUID is not original, keep it exactly as-is. This preserves modifiers from other mods.
3. Re-add the reconstructed list in the same iteration order.

This supports items with multiple original modifiers for the same attribute and avoids assuming there is exactly one vanilla "base" modifier.

## Tooltip Contract

The tooltip layer is intentionally minimal:

- It may remove stale lines for `REMOVE`.
- It may add missing lines for genuine new `ADD` attributes.
- It must not calculate player base, item base, totals, or percentage semantics for `MODIFY`.
- It must not aggregate or rewrite vanilla-rendered lines.

Complex number formatting belongs in pure helper logic and unit tests, not in the event handler.

