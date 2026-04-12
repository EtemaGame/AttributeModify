# Datapack Schema

Datapack files are loaded from:

```text
data/<namespace>/item_attributes/<file>.json
```

Each file is a JSON object. Top-level keys are item IDs or item tag IDs prefixed with `#`.

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "modify",
          "amount": 10,
          "operation": "addition"
        }
      ]
    }
  },
  "#minecraft:swords": {
    "attributes": [
      {
        "attribute": "minecraft:generic.luck",
        "action": "add",
        "amount": 2
      }
    ]
  }
}
```

## Item Object Fields

| Field | Type | Notes |
| :--- | :--- | :--- |
| `equipment_slots` | object | Explicit vanilla slot map. Valid slots: `mainhand`, `offhand`, `feet`, `legs`, `chest`, `head`. |
| `curios_slots` | object | Explicit Curios slot map. Only processed when Curios is loaded. |
| `attributes` | array | Shorthand entries using auto slot detection. |
| `attribute` + `amount` | fields | Single shorthand entry using auto slot detection. |
| `durability` | number | Max durability override. |
| `durability_triggers` | string array | Optional triggers for custom durability. |

The codebase also has runtime structures for quality, mining, and decorative sync. Keep this schema aligned with the parser before documenting those fields as stable datapack inputs.

## Attribute Entry

| Field | Type | Required | Notes |
| :--- | :--- | :--- | :--- |
| `attribute` | string | yes | Registry ID, for example `minecraft:generic.attack_damage`. |
| `action` | string | no | `add`, `modify`, or `remove`. Defaults to `add`. |
| `amount` | number | for `add`/`modify` | Defaults to `0.0` if omitted, but omission should be treated as a warning. |
| `operation` | string | no | `addition`, `multiply_base`, or `multiply_total`. Defaults to `addition`. Aliases are supported for legacy names. |
| `uuid` | string | no | Explicit modifier UUID. Invalid UUIDs fall back to a deterministic generated UUID and log an error. |
| `modifier_id` | string | no | Stable name/seed for generated UUIDs. |
| `nbt` | object | no | Runtime condition for matching an `ItemStack`. |

For `remove`, `amount`, `operation`, `uuid`, and `modifier_id` are ignored.

## NBT Condition

```json
{
  "nbt": {
    "path": "quality",
    "operator": "equals",
    "value": "legendary"
  }
}
```

`key` is accepted as an alias for `path`. Operators include:

- `exists`
- `equals` / `==`
- `not_equals` / `!=`
- `greater` / `>`
- `greater_or_equal` / `>=`
- `less` / `<`
- `less_or_equal` / `<=`
- `contains`
- `starts_with`
- `ends_with`
- `matches_regex`

