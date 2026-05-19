# AttributeModify / NeoAttributeModify

Customize item attributes, mining behavior, durability, and item progression with JSON datapacks. No Java required.

Latest Forge release: `1.1.2` for Minecraft `1.20.1`.

`AttributeModify` is the Forge edition for Minecraft 1.20.1.
`NeoAttributeModify` is the NeoForge edition for Minecraft 1.21.1.

Both projects use the same datapack folder layout and the same general JSON style. The main difference is how item data is read internally:

- 1.20.1 evaluates NBT-style data
- 1.21.1 evaluates data components
- For cross-version packs, component-like paths such as `component:apotheosis:rarity` can be used to keep configs easier to maintain

## What You Can Do

- Add, modify, or remove attributes on vanilla and modded items
- Target standard equipment slots, `body`, and optional Curios slots
- Use shorthand syntax with automatic slot detection
- Apply rules to single items or whole item tags
- Make rules conditional with NBT or component matching
- Override mining speed and mining tier
- Override durability and optional durability triggers
- Roll randomized quality tiers on item creation
- Mark items as decorative
- Split configs across multiple files and hot-reload them with `/reload`
- Sync everything automatically in multiplayer
- Edit rules in-game with `/attributemodify_editor`

## Datapack Location

Place your files in:

```text
data/<namespace>/item_attributes/<file>.json
```

Entries from multiple files are merged, so you can organize your datapack however you want.

## Examples

### 1. Single shorthand attribute with automatic slot detection

```json
{
  "minecraft:diamond_chestplate": {
    "attribute": "minecraft:generic.luck",
    "action": "add",
    "amount": 1.0,
    "operation": "add_value"
  }
}
```

Useful for quick changes. Armor and common tools can infer the correct slot automatically.

### 2. Multiple shorthand attributes on the same item

```json
{
  "minecraft:diamond_boots": {
    "attributes": [
      {
        "attribute": "minecraft:generic.armor",
        "action": "add",
        "amount": 1.0,
        "operation": "add_value"
      },
      {
        "attribute": "minecraft:generic.movement_speed",
        "action": "add",
        "amount": 0.03,
        "operation": "add_multiplied_total"
      }
    ]
  }
}
```

### 3. Explicit equipment slots with `add`, `modify`, and `remove`

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "modify",
          "amount": 10.0,
          "operation": "add_value"
        },
        {
          "attribute": "minecraft:generic.attack_speed",
          "action": "remove"
        }
      ]
    }
  }
}
```

### 4. Curios slot example

Requires Curios to be installed.

```json
{
  "mymod:magic_ring": {
    "curios_slots": {
      "ring": [
        {
          "attribute": "minecraft:generic.max_health",
          "action": "add",
          "amount": 4.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

### 5. Apply one rule to a whole item tag

```json
{
  "#minecraft:swords": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_knockback",
          "action": "add",
          "amount": 0.5,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

### 6. Conditional attributes with NBT or component matching

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value",
          "nbt": {
            "path": "component:apotheosis:rarity",
            "operator": "equals",
            "value": "apotheosis:mythic"
          }
        }
      ]
    }
  }
}
```

You can use `path` or `key` inside the `nbt` block.

### 7. Quality system

```json
{
  "minecraft:iron_sword": {
    "quality_system": {
      "tag_path": "quality",
      "triggers": ["craft", "loot", "villager_trade", "apotheosis_sync"],
      "levels": [
        { "value": "common", "weight": 60 },
        { "value": "rare", "weight": 30 },
        { "value": "mythic", "weight": 10 }
      ]
    }
  }
}
```

### 8. Mining speed and tier overrides

```json
{
  "minecraft:iron_pickaxe": {
    "mining": [
      {
        "speed": 12.0,
        "tier": "diamond",
        "nbt": {
          "path": "quality",
          "operator": "equals",
          "value": "mythic"
        }
      },
      {
        "speed": 8.0,
        "tier": "iron"
      }
    ]
  }
}
```

### 9. Durability override (Virtual Durability)

```json
{
  "minecraft:diamond_pickaxe": {
    "durability": 3000
  }
}
```

**Advantages of the virtual system:**

- **Compatibility**: It doesn't interfere with other mods that read the base Item value.
- **Automatic scaling**: If you change the limit from 3000 to 5000, tools that already exist in your world will automatically scale their wear to maintain the proportion.
- **Persistence**: The applied maximum is saved in the item's NBT (`attributemodify.vdur.applied_max`).

#### Triggers for items WITHOUT durability

If you want an item that normally does NOT have durability (like a Stick) to have it, AttributeModify will use **Custom Durability**. For these cases, you must define which actions break the item:

```json
{
  "minecraft:stick": {
    "durability": 100,
    "durability_triggers": ["melee_hit", "block_break"]
  }
}
```

Supported triggers:

- `melee_hit`: When hitting an entity.
- `block_break`: When breaking a block.
- `right_click`: When using the item with right click.

### 10. Decorative items

```json
{
  "minecraft:carved_pumpkin": {
    "decorative": true
  }
}
```

## Quick Reference

### Top-level blocks

- `attribute`: single shorthand attribute entry
- `attributes`: shorthand array of attribute entries
- `equipment_slots`: explicit vanilla equipment slots
- `curios_slots`: explicit Curios slots
- `quality_system`: weighted quality roll rules
- `mining`: mining speed and tier overrides
- `durability`: max durability override
- `durability_triggers`: optional triggers for custom durability
- `decorative`: marks an item as decorative

### Standard equipment slots

`mainhand`, `offhand`, `head`, `chest`, `legs`, `feet`, `body`

If no slot is provided, AttributeModify uses automatic slot detection. This keeps older and simpler datapacks valid while still allowing explicit slots for special cases like offhand, Curios, or forced equipment behavior.

## In-Game Editor

Open the editor with:

```text
/attributemodify_editor
```

The editor can select item targets, switch item/tag mode, edit attribute changes, use automatic or explicit slots, add NBT/component conditions, configure durability, mining overrides, quality rolls, and decorative flags.

Rules saved by the editor use the same datapack format shown above.

### Actions

- `add`: add modifiers on top of existing stats
- `modify`: replace the base modifiers for that attribute
- `remove`: remove that attribute from the item for the target slot

### Operations

- `add_value`
- `add_multiplied_base`
- `add_multiplied_total`

Accepted aliases:

- `addition`
- `multiply_base`
- `multiply_total`

### NBT or component operators

- `equals`
- `not_equals`
- `greater`
- `greater_or_equal`
- `less`
- `less_or_equal`
- `exists`
- `not_exists`
- `contains`
- `starts_with`
- `ends_with`
- `matches_regex`

### Quality triggers

- `craft`
- `loot`
- `villager_trade`
- `apotheosis_sync`

## Multiplayer and Reloading

- `/reload` re-reads datapack files without restarting the game
- Servers sync attribute, mining, durability, and decorative data to clients
- The logic is designed for multiplayer use

## Compatibility Notes

- Works with vanilla items and modded items
- Curios support is optional
- Apotheosis-style data checks are supported through conditional matching
- Use `modifier_id` if you want a fixed custom modifier identity for compatibility with other mods

## Links

- Discord: <https://discord.gg/NXVEtWqsnw>

Author: Etema
