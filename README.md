# AttributeModify

**Version 1.2.4 | Minecraft 1.20.1 | Forge**

Modify item attributes and related item behavior through datapacks. The mod supports vanilla and modded attributes, mining overrides, durability, decorative profiles, weighted quality data, and optional Curios or Accessories slots.

AttributeModify and NeoAttributeModify are published on the same CurseForge and Modrinth project pages. Install only the file that matches your Minecraft version and mod loader.

## Available Versions

| Mod | Minecraft | Loader |
| --- | --- | --- |
| AttributeModify | 1.20.1 | Forge |
| NeoAttributeModify | 1.21.1 | NeoForge |

The Forge and NeoForge files are not interchangeable.

## Features

- Add, replace, set, or remove item attribute modifiers
- Remove one modifier operation without removing the others
- Override mining speed while preserving external speed multipliers
- Raise harvest capability to vanilla or modded mining tier requirements
- Override durability and custom durability triggers
- Disable functional behavior for decorative items without deleting unrelated player effects
- Roll weighted quality data on craft, loot, or villager trade events
- Generate rules through the in-game editor
- Apply rules conditionally using NBT paths
- Works with vanilla and modded items
- Multiplayer friendly and `/reload` compatible
- Keeps Curios and Accessories rule storage isolated

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.0` or a compatible Forge 47 release
- Java `17`

Optional integrations:

- Curios `5.14.1+`
- Accessories `1.0.0-beta.47+`
- Apotheosis `7.4.8` with its required Placebo dependency

AttributeModify starts normally when these optional mods are absent.

## Datapack Layout

Place the datapack inside `<world>/datapacks/`. Attribute files belong under `data/<namespace>/item_attributes/`:

```text
MyAttributePack/
|-- pack.mcmeta
`-- data/
    `-- my_namespace/
        `-- item_attributes/
            `-- tools.json
```

Minecraft 1.20.1 uses datapack format `15`:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "My AttributeModify rules"
  }
}
```

Run `/reload` after changing a datapack.

## Client Command

- `/attributemodify_editor` opens the rule editor.

The editor writes `AttributeModify_Editor` into the current world's datapack directory. Saving requires the singleplayer owner or permission level `2` on a server.

## Support

- Discord: <https://discord.gg/NXVEtWqsnw>
