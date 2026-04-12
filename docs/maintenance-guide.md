# Maintenance Guide

This guide records the current "behavior first" architecture for AttributeModify.

## Layers

### Resolution: `service.AttributeResolutionService`

Resolves which datapack entries apply to an `ItemStack` and `EquipmentSlot`.

Responsibilities:
- Filter by item and slot.
- Apply NBT conditions.
- Keep handlers thin.

### Application: `service.AttributeApplicationService`

Applies resolved rules to `ItemAttributeModifierEvent`.

Responsibilities:
- `ADD`: add the datapack modifier.
- `MODIFY`: replace only original item modifiers for the target attribute while preserving UUID, name, and relative order.
- `REMOVE`: remove the target attribute from the event.
- Emit runtime semantic warnings when `MODIFY` has no valid original target.

### Presentation: `service.TooltipPresentationService`

Keeps tooltip intervention small.

Responsibilities:
- Hide stale lines for `REMOVE`.
- Add missing vanilla-style lines for genuinely new `ADD` attributes.
- Leave `MODIFY` to vanilla/Forge rendering.

Do not put player-base math, item-base math, or total aggregation in this event layer.

### Pure Helpers: `util.AttributeTooltipHelper`

Contains deterministic formatting and translation-key helpers that can be unit-tested without a Minecraft integration test.

## Logging

There is no hardcoded `DEBUG_MODE` gate. Use normal logger levels:

- `debug`: detailed tracing during loading, matching, or non-critical skips.
- `info`: lifecycle summaries.
- `warn`: invalid datapack values or runtime semantic mismatches.
- `error`: parse failures or runtime exceptions that prevent an operation.

## Rules For Future Changes

1. Update `docs/attribute-behavior-spec.md` before changing behavior.
2. Keep event handlers thin; place behavior in services or helpers.
3. Preserve modifier identity on `MODIFY` whenever an original target exists.
4. Prefer static validation at datapack load time for malformed JSON and registry IDs.
5. Use runtime warnings for semantic misses that only become knowable with an actual item/slot context.
6. Add focused unit tests for pure helper logic when changing number formatting or tooltip key selection.

