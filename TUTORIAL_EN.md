# Step-by-Step Tutorial: AttributeModify 1.20.1 and NeoAttributeModify 1.21.1

This tutorial is based on the current behavior of the parser and handlers in both projects.
The general datapack structure is the same in both versions, but there are real differences that are worth documenting separately.

JSON files always go in:

```text
data/<namespace>/item_attributes/<file>.json
```

You can split your rules across multiple files. The mod merges them when the datapack is loaded.

## 1.20.1 - Forge - AttributeModify

### Step 1. Create the datapack

In singleplayer:

```text
.minecraft/saves/<YourWorld>/datapacks/<your_pack>
```

On a dedicated server:

```text
<server>/world/datapacks/<your_pack>
```

Create this `pack.mcmeta`:

```json
{
  "pack": {
    "description": "AttributeModify 1.20.1 Tutorial",
    "pack_format": 15
  }
}
```

Recommended structure:

```text
<your_pack>/
  pack.mcmeta
  data/
    tutorial/
      item_attributes/
        armor.json
        tools.json
        quality.json
```

### Step 2. Create your first rule

In `data/tutorial/item_attributes/armor.json`:

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

This shorthand format works in `1.20.1`.
The mod tries to detect the slot automatically.

### Step 3. Use explicit slots

If you want full control, use `equipment_slots`:

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

Use `modify` when you want to replace the base attribute for that item in that slot.
Use `remove` when you want to remove that attribute from the item in that slot.

### Step 4. Apply one rule to an entire tag

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

This is useful when you want to affect entire groups of items, including modded items if they are in the tag.

### Step 5. Create a conditional NBT rule

`1.20.1` evaluates the item's NBT.

```json
{
  "minecraft:iron_pickaxe": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 3.0,
          "operation": "add_value",
          "nbt": {
            "path": "quality",
            "operator": "equals",
            "value": "mythic"
          }
        }
      ]
    }
  }
}
```

You can also use the compatibility alias:

```json
{
  "nbt": {
    "path": "component:apotheosis:rarity",
    "operator": "equals",
    "value": "apotheosis:mythic"
  }
}
```

In `1.20.1`, that alias is only implemented to map to `affix_data.rarity`.
It is not a generic component reader.

### Step 6. Use quality_system

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

This writes the quality into the item's NBT at the `quality` path.
You can then reuse that value in other rules through `nbt.path`.

### Step 7. Change mining behavior

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

The first matching entry wins.

### Step 8. Override durability (Virtual Durability)

In `1.20.1`, AttributeModify uses a **Virtual Durability** system for items that already have durability (vanilla or modded). This means we don't change the global Item value, but each individual tool knows its new maximum.

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

### Step 9. Curios

If you have Curios installed, you can declare explicit slots:

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

### Step 10. Reload and test

1. Enter the world or server.
2. Run `/reload`.
3. Pick up the item again or craft it again if you are testing `quality_system`.
4. Check the tooltip, mining speed, or durability depending on the case.

### 1.20.1 limitations

- `equipment_slots` only accepts `mainhand`, `offhand`, `head`, `chest`, `legs`, and `feet`.
- `body` is not implemented in this branch.
- The shorthand form with `attribute` exists and only works here.
- The `nbt` block reads the item's NBT. The `component:` prefix is not generic; there is only special compatibility for `component:apotheosis:rarity`.
- `durability_triggers` only exists in this branch.
- `durability_triggers` only makes sense in custom mode, which in practice points to non-damageable items with stack size 1.
- `modify` does not create new attributes. If the item did not already have that attribute in that slot, nothing happens.
- `tier` only accepts vanilla names: `wood`, `stone`, `iron`, `diamond`, `netherite`, `gold`.
- Slot autodetection only covers armor, `SwordItem`, `TieredItem`, `TridentItem`, and some common Curios tags such as `ring`, `necklace`, `belt`, and `charm`.
- In `mining`, the first matching rule wins.

## 1.21.1 - NeoForge - NeoAttributeModify

### Step 1. Create the datapack

The datapack location is the same, but `pack_format` changes:

```json
{
  "pack": {
    "description": "NeoAttributeModify 1.21.1 Tutorial",
    "pack_format": 34
  }
}
```

### Step 2. Create your first rule

In `1.21.1`, use `attributes` or `equipment_slots`.
The shorthand form with a single `attribute` key is no longer parsed in this branch.

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

### Step 3. Use explicit slots

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
        }
      ]
    }
  }
}
```

This branch also supports the `body` slot.

### Step 4. Create a conditional rule

`1.21.1` works with `minecraft:custom_data` for conditional checks.
The safest approach is to write a value there first and then read it through `nbt.path`.

```json
{
  "minecraft:iron_sword": {
    "quality_system": {
      "tag_path": "quality",
      "triggers": ["craft", "loot", "villager_trade"],
      "levels": [
        { "value": "common", "weight": 60 },
        { "value": "rare", "weight": 30 },
        { "value": "mythic", "weight": 10 }
      ]
    },
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value",
          "nbt": {
            "path": "quality",
            "operator": "equals",
            "value": "mythic"
          }
        }
      ]
    }
  }
}
```

Important recommendation:
always define `tag_path` explicitly if you want packs that are easier to port.

### Step 5. Change mining behavior

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

Just like in `1.20.1`, the first valid rule is the one that gets applied.

### Step 6. Change durability

```json
{
  "minecraft:diamond_pickaxe": {
    "durability": 3000
  }
}
```

In this branch, there is no `durability_triggers` support in the datapack parser.
If you define `durability`, the mod handles the internal wear logic through its own events.

### Step 7. Curios

Curios is still optional. If it is installed:

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

### Step 8. Reload and test

1. Run `/reload`.
2. Test the item again.
3. If you use `quality_system`, craft or obtain a new item to force the roll.
4. If you use `nbt.path`, make sure the value actually lives inside `custom_data`.

### 1.21.1 limitations

- The shorthand form with a single `attribute` key is not supported. Use `attributes` or `equipment_slots`.
- `equipment_slots` accepts `body` in addition to the classic slots.
- The `nbt` block compares against `minecraft:custom_data`, not against every vanilla item data component.
- `durability_triggers` is not implemented in the datapack for this branch.
- `quality_system.tag_path` uses `Quality` as its default value if you do not define it. To avoid differences with `1.20.1`, it is better to always declare it manually.
- There is no equivalent integration for `apotheosis_sync` and no special alias like `component:apotheosis:rarity`.
- `modify` does not create new attributes. It only replaces an attribute that already exists in that slot.
- `tier` only accepts vanilla names: `wood`, `stone`, `iron`, `diamond`, `netherite`, `gold`.
- Slot autodetection is still limited to armor, `SwordItem`, `TieredItem`, `TridentItem`, and some common Curios tags.
- In `mining`, the first matching rule wins.

## Quick migration from 1.20.1 to 1.21.1

1. Change `pack_format` from `15` to `34`.
2. Replace the shorthand `attribute` form with `attributes` or `equipment_slots`.
3. Always define `quality_system.tag_path` so you do not depend on different defaults.
4. If a condition depended on arbitrary NBT, make sure that value now lives inside `minecraft:custom_data`.
5. If you used `durability_triggers`, rethink that config because `1.21.1` does not parse it.
6. If you depended on `apotheosis_sync` or `component:apotheosis:rarity`, keep a separate `1.20.1` version.

## Practical tips

- If two mods touch the same attribute, use `modifier_id` to keep a stable modifier identity.
- If a rule does not seem to work, try `equipment_slots` first instead of relying on autodetection.
- If a `modify` rule does nothing, it usually means the original item did not already have that attribute in that slot.
- If a `tier` does not work, make sure it is one of the supported vanilla names.
