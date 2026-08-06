# AttributeModify

AttributeModify is a Forge 1.20.1 mod that changes item behavior through datapacks without requiring custom item classes.

Current mod version: `1.2.3`

## Features

- Add, modify, set, or remove item attributes by equipment slot
- Remove a complete attribute or only modifiers using one operation
- Support vanilla slots plus optional Curios and Accessories slots
- Override mining speed while preserving proportional bonuses and penalties
- Use vanilla or modded registered mining tiers
- Override vanilla durability or add custom durability to normally unbreakable items
- Roll weighted quality values on craft, loot, or villager trade events
- Integrate quality rarity paths with Apotheosis when it is installed
- Mark items as decorative and configure tooltip, combat, use, and effect restrictions
- Apply rules conditionally using NBT paths
- Edit rules in game and inspect final attribute origins
- Synchronize datapack rules between dedicated server and clients
- Reload rules with `/reload`

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.0` or a compatible Forge 47 release
- Java `17`

Optional integrations:

- Curios `5.14.1+`
- Accessories `1.0.0-beta.47+`
- Apotheosis `7.4.8` with its required Placebo dependency

AttributeModify starts normally when these optional mods are absent.

## Datapack Location

Place JSON files in:

```text
data/<namespace>/item_attributes/<file>.json
```

Minimal example:

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

Run `/reload` after adding or changing datapack files.

## Client Commands

- `/attributemodify_editor` opens the rule editor. Saving requires the singleplayer owner or permission level 2 on a server.
- `/attributemodify_inspect` inspects the main-hand item.
- `/attributemodify_inspect mainhand` inspects the main-hand item explicitly.
- `/attributemodify_inspect offhand` inspects the off-hand item.

The inspector reports default modifiers, modifiers injected by other events or mods, matching AttributeModify rules, and the effective final result. The complete report is also written to `latest.log`.

## Documentation

- [Complete tutorial](Tutorial.md)
- [Manual Forge regression datapack](test-packs/forge-regression/README.md)

## Build

```text
gradlew clean test jar
```

The reobfuscated mod is generated under `build/libs/`.

## Support

- Discord: <https://discord.gg/NXVEtWqsnw>
