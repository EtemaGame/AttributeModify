# AttributeModify Tutorial

This guide explains the mod step by step. It is written to be practical rather than formal.

## 1. What the mod does

AttributeModify reads JSON files from your datapack and applies changes to items.

You can use it to:

- add, modify, or remove item attributes
- change mining speed and mining tier
- change durability
- apply conditional changes with NBT
- mark items as decorative
- roll quality tiers

## 2. Where the files go

Put your files here:

```text
data/<namespace>/item_attributes/<file>.json
```

Example:

```text
data/mymod/item_attributes/weapons.json
data/mymod/item_attributes/tools.json
```

You can split your rules across several files. The game merges them.

## 3. The basic structure

There are three common ways to write a rule.

### A. Automatic slot detection

This is the most used form. The mod tries to figure out the slot for you.

```json
{
  "minecraft:diamond_chestplate": {
    "attribute": "minecraft:generic.armor",
    "action": "add",
    "amount": 2.0,
    "operation": "add_value"
  }
}
```

Use this when the item clearly belongs to one slot and you do not want to write the slot manually.

### B. Multiple attributes in one item

Use `attributes` when the same item needs more than one change.

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

Use this when one item needs several effects at once.

### C. Explicit slots

Use `equipment_slots` when you want to pick the slot yourself.

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

Read it like this:

- the key on the left is the item id
- the object inside contains the rule for that item
- the inner arrays hold the actual attribute entries
- the same item can use `attribute`, `attributes`, or `equipment_slots` depending on how precise you want to be

## 4. Adding a simple attribute

If you only want one attribute entry, use `attribute` at the top level.

```json
{
  "minecraft:diamond_sword": {
    "attribute": "minecraft:generic.attack_damage",
    "action": "add",
    "amount": 5.0,
    "operation": "add_value"
  }
}
```

This is the easiest form when the item and slot are obvious.

If the item can be auto-detected, this is usually all you need.

### What the fields mean

- `attribute`: the attribute id
- `action`: what to do with it
- `amount`: the value to apply
- `operation`: how the amount is applied

### Actions

- `add`: add on top of the current value
- `modify`: replace the existing modifier for that entry
- `set`: set the final result more directly
- `remove`: remove the attribute from that slot

### Operations

- `add_value`
- `add_multiplied_base`
- `add_multiplied_total`

Accepted aliases:

- `addition`
- `multiply_base`
- `multiply_total`
- `add_value`
- `add_multiplied_base`
- `add_multiplied_total`

If you leave `operation` out, the mod treats it as `addition`.

## 5. Using multiple attributes

If an item needs more than one change, use `attributes`.

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

Use this when one item needs several effects at once.

## 6. Slot handling

There are three practical ways to handle a rule:

- `attribute` for one change, with slot auto-detection
- `attributes` for several changes, with slot auto-detection
- `equipment_slots` when you want the slot written out explicitly

### Automatic slot detection

This is the easiest path. The mod tries to infer the slot for common items.

Examples:

- armor items go to their armor slot
- swords, tiered tools, and tridents go to `mainhand`
- some Curios items can be detected from their tags

The chestplate example above is one of the common cases this branch handles automatically.

### Explicit slots

If you want full control, use `equipment_slots`.

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

Supported standard slots:

- `mainhand`
- `offhand`
- `head`
- `chest`
- `legs`
- `feet`
- `body`

Curios and Accessories can also be used if those integrations are installed and enabled.

## 7. Matching conditions

You can limit a rule to only work when a piece of NBT exists or matches a value.

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
            "path": "affix_data.rarity",
            "operator": "equals",
            "value": "mythic"
          }
        }
      ]
    }
  }
}
```

### Condition fields

- `path`: the NBT path to check
- `operator`: how the comparison is done
- `value`: the value to compare against

You can also use `key` instead of `path`.

Supported operators:

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

### A couple of notes

- `matches_regex` is useful, but a bad pattern can still fail validation.
- `not_equals` can be useful when you want a rule to apply unless a value matches.
- Keep the path aligned with the data the item actually stores.

## 8. Mining overrides

Mining rules live under `mining`.

```json
{
  "minecraft:iron_pickaxe": {
    "mining": [
      {
        "speed": 8.0,
        "tier": "iron"
      }
    ]
  }
}
```

### What `speed` does

`speed` is the break speed value the mod applies.

If the item already has a break-speed bonus, the mod keeps that proportional when it can. So the value you enter is the base reference, not a random display number.

### What `tier` does

`tier` controls what blocks the item can harvest.

Use a registered tier name or id, for example:

- `wood`
- `stone`
- `iron`
- `diamond`
- `netherite`
- `minecraft:iron`
- `minecraft:diamond`

Notes:

- `wooden` is treated like `wood`
- `golden` is treated like `gold`
- numeric-only tier values are not read by this branch unless the mod or another mod registers them as named tiers

### Mining with conditions

You can also attach an NBT condition to a mining override.

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
      }
    ]
  }
}
```

The first matching mining override wins.

## 9. Durability

Durability is simple:

```json
{
  "minecraft:diamond_pickaxe": {
    "durability": 3000
  }
}
```

### If the item already has durability

The mod can scale it as a virtual durability override. This keeps the item compatible with other systems that still read the base item value.

### If the item does not normally have durability

You can add durability and tell the mod what actions should consume it.

```json
{
  "minecraft:stick": {
    "durability": 100,
    "durability_triggers": ["melee_hit", "block_break"]
  }
}
```

Supported triggers:

- `melee_hit`
- `block_break`
- `right_click`

If you do not add triggers for a custom-durability item, it will not be very useful in practice.

## 10. Quality rolls

Quality rules let you roll values based on weights.

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

### How to read it

- `tag_path` is where the rolled value is stored
- `triggers` says when a roll can happen
- `levels` is the weighted list of possible results

## 11. Decorative items

If you only want an item to count as decorative, mark it like this:

```json
{
  "minecraft:carved_pumpkin": {
    "decorative": true
  }
}
```

## 12. A few things to keep in mind

- If a field is invalid, the game usually warns about it during validation or load.
- Keep item ids valid. A typo in the item id means the rule will not apply.
- Keep attribute ids valid too.
- If a mining tier is not registered, the rule will be ignored.
- If a Curios or Accessories slot does not exist, that slot will not pass validation.
- Use one example at a time when testing. It is easier to debug that way.

## 13. Suggested way to test

1. Start with one item.
2. Add one rule only.
3. Use `/reload`.
4. Check the item in game.
5. Add the next rule only after the first one works.

This saves time because it is much easier to find the broken part.

## 14. Good starting examples

### Simple weapon boost

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

### Faster pickaxe

```json
{
  "minecraft:iron_pickaxe": {
    "mining": [
      {
        "speed": 8.0,
        "tier": "iron"
      }
    ]
  }
}
```

### Custom durability

```json
{
  "minecraft:stick": {
    "durability": 100,
    "durability_triggers": ["right_click"]
  }
}
```

## 15. Support

If you want the version-specific details, examples, or edge cases, keep them in Discord. This guide is the step-by-step base.

- Discord: <https://discord.gg/NXVEtWqsnw>
