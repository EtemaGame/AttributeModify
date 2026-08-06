# Forge 1.20.1 manual regression pack

Copy this folder into a test world's `datapacks` directory and run `/reload`.

- Hold a stick and verify that it grants 3 attack damage.
- Check that a wooden sword reports 96 durability.
- Compare an iron pickaxe with and without Efficiency, Haste, and Mining Fatigue.
- Craft or pick up a golden apple and inspect its `attributemodify.quality` tag.
- Equip or hold a carved pumpkin: attacks and use must be blocked, unrelated effects must remain, and non-attribute tooltip metadata must be preserved.
- Repeat the checks after a second `/reload` and after reconnecting to a dedicated server.
