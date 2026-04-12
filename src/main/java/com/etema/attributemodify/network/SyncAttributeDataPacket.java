package com.etema.attributemodify.network;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncAttributeDataPacket {
    private final Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> standardAttributes;
    private final Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> curiosAttributes;
    private final Map<Item, com.etema.attributemodify.durability.DurabilityRule> durabilityRules;
    private final Map<Item, List<ItemAttributeDataManager.MiningOverride>> miningOverrides;
    private final java.util.Set<Item> decorativeItems;

    public SyncAttributeDataPacket(
            Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> standard,
            Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> curios,
            Map<Item, com.etema.attributemodify.durability.DurabilityRule> durabilityRules,
            Map<Item, List<ItemAttributeDataManager.MiningOverride>> mining,
            java.util.Set<Item> decorative) {
        this.standardAttributes = deepCopyStandard(standard);
        this.curiosAttributes = deepCopyCurios(curios);
        this.durabilityRules = copyDurabilityMap(durabilityRules);
        this.miningOverrides = copyMiningMap(mining);
        this.decorativeItems = decorative != null ? new java.util.HashSet<>(decorative) : new java.util.HashSet<>();
    }

    public static void encode(SyncAttributeDataPacket packet, FriendlyByteBuf buf) {
        // Pre-filter standard attributes to get accurate count
        List<Map.Entry<ResourceLocation, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> > validStandard = new ArrayList<>();
        for (var itemEntry : packet.standardAttributes.entrySet()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemEntry.getKey());
            if (itemId != null) {
                validStandard.add(Map.entry(itemId, itemEntry.getValue()));
            }
        }

        buf.writeInt(validStandard.size());
        for (var itemEntry : validStandard) {
            buf.writeResourceLocation(itemEntry.getKey());

            buf.writeInt(itemEntry.getValue().size());
            for (var slotEntry : itemEntry.getValue().entrySet()) {
                buf.writeEnum(slotEntry.getKey());

                List<ItemAttributeDataManager.AttributeEntry> filtered = new ArrayList<>();
                List<ResourceLocation> attributeIds = new ArrayList<>();
                for (var attr : slotEntry.getValue()) {
                    ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr.attribute());
                    if (attrId == null) {
                        continue;
                    }
                    filtered.add(attr);
                    attributeIds.add(attrId);
                }

                buf.writeInt(filtered.size());
                for (int i = 0; i < filtered.size(); i++) {
                    var attr = filtered.get(i);
                    ResourceLocation attrId = attributeIds.get(i);
                    buf.writeEnum(attr.action());
                    buf.writeResourceLocation(attrId);

                    if (attr.modifier() != null) {
                        buf.writeBoolean(true);
                        buf.writeUUID(attr.modifier().getId());
                        // OPTIMIZED: Name field removed from packet
                        buf.writeDouble(attr.modifier().getAmount());
                        buf.writeEnum(attr.modifier().getOperation());
                    } else {
                        buf.writeBoolean(false);
                    }

                    encodeNbtCondition(buf, attr.nbtCondition());
                }
            }
        }

        // Pre-filter curios attributes to get accurate count
        List<Map.Entry<ResourceLocation, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> > validCurios = new ArrayList<>();
        for (var itemEntry : packet.curiosAttributes.entrySet()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemEntry.getKey());
            if (itemId != null) {
                validCurios.add(Map.entry(itemId, itemEntry.getValue()));
            }
        }

        buf.writeInt(validCurios.size());
        for (var itemEntry : validCurios) {
            buf.writeResourceLocation(itemEntry.getKey());

            buf.writeInt(itemEntry.getValue().size());
            for (var slotEntry : itemEntry.getValue().entrySet()) {
                buf.writeUtf(slotEntry.getKey());

                List<ItemAttributeDataManager.AttributeEntry> filtered = new ArrayList<>();
                List<ResourceLocation> attributeIds = new ArrayList<>();
                for (var attr : slotEntry.getValue()) {
                    ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr.attribute());
                    if (attrId == null) {
                        continue;
                    }
                    filtered.add(attr);
                    attributeIds.add(attrId);
                }

                buf.writeInt(filtered.size());
                for (int i = 0; i < filtered.size(); i++) {
                    var attr = filtered.get(i);
                    ResourceLocation attrId = attributeIds.get(i);
                    buf.writeEnum(attr.action());
                    buf.writeResourceLocation(attrId);

                    if (attr.modifier() != null) {
                        buf.writeBoolean(true);
                        buf.writeUUID(attr.modifier().getId());
                        // OPTIMIZED: Name field removed from packet
                        buf.writeDouble(attr.modifier().getAmount());
                        buf.writeEnum(attr.modifier().getOperation());
                    } else {
                        buf.writeBoolean(false);
                    }

                    encodeNbtCondition(buf, attr.nbtCondition());
                }
            }
        }

        // Pre-filter durability rules to get accurate count
        List<Map.Entry<ResourceLocation, com.etema.attributemodify.durability.DurabilityRule>> validDurability = new ArrayList<>();
        for (var entry : packet.durabilityRules.entrySet()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (itemId != null) {
                validDurability.add(Map.entry(itemId, entry.getValue()));
            }
        }

        buf.writeInt(validDurability.size());
        for (var entry : validDurability) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeInt(entry.getValue().maxDurability());
            buf.writeUtf(entry.getValue().mode().name());
            buf.writeInt(entry.getValue().triggers().size());
            for (String trigger : entry.getValue().triggers()) {
                buf.writeUtf(trigger);
            }
        }

        // Pre-filter mining overrides to get accurate count
        List<Map.Entry<ResourceLocation, List<ItemAttributeDataManager.MiningOverride>>> validMining = new ArrayList<>();
        for (var entry : packet.miningOverrides.entrySet()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (itemId != null) {
                validMining.add(Map.entry(itemId, entry.getValue()));
            }
        }

        buf.writeInt(validMining.size());
        for (var entry : validMining) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (var override : entry.getValue()) {
                buf.writeBoolean(override.speed() != null);
                if (override.speed() != null) {
                    buf.writeFloat(override.speed());
                }
                buf.writeBoolean(override.tierName() != null);
                if (override.tierName() != null) {
                    buf.writeUtf(override.tierName());
                }
                encodeNbtCondition(buf, override.nbtCondition());
            }
        }

        // Filter decorative items
        List<ResourceLocation> validDecorative = new ArrayList<>();
        for (Item item : packet.decorativeItems) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null) {
                validDecorative.add(itemId);
            }
        }
        buf.writeInt(validDecorative.size());
        for (ResourceLocation loc : validDecorative) {
            buf.writeResourceLocation(loc);
        }
    }

    private static void encodeNbtCondition(FriendlyByteBuf buf, ItemAttributeDataManager.NbtCondition nbtCondition) {
        if (nbtCondition != null) {
            buf.writeBoolean(true);
            buf.writeUtf(nbtCondition.getPath());
            buf.writeUtf(nbtCondition.getOperator());
            // Use "null" sentinel instead of empty string to distinguish from actual empty
            // values
            String valueStr = nbtCondition.getValue() != null ? nbtCondition.getValue().toString() : "null";
            buf.writeUtf(valueStr);
        } else {
            buf.writeBoolean(false);
        }
    }

    private static ItemAttributeDataManager.NbtCondition decodeNbtCondition(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        String path = buf.readUtf();
        String operator = buf.readUtf();
        String valueStr = buf.readUtf();
        if (valueStr.isEmpty() || "null".equals(valueStr)) {
            return new ItemAttributeDataManager.NbtCondition(path, operator, null);
        }
        try {
            com.google.gson.JsonElement value = com.google.gson.JsonParser.parseString(valueStr);
            return new ItemAttributeDataManager.NbtCondition(path, operator, value);
        } catch (Exception e) {
            if (AttributeModify.DEBUG_MODE) {
                AttributeModify.LOGGER.error("Failed to parse NBT condition value '{}': {}", valueStr, e.getMessage());
            }
            return new ItemAttributeDataManager.NbtCondition(path, operator, null);
        }
    }

    public static SyncAttributeDataPacket decode(FriendlyByteBuf buf) {
        Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> standard = new HashMap<>();
        Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> curios = new HashMap<>();
        Map<Item, com.etema.attributemodify.durability.DurabilityRule> durability = new HashMap<>();

        int standardSize = buf.readInt();
        for (int i = 0; i < standardSize; i++) {
            ResourceLocation itemLoc = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
            if (item == null) {
                if (AttributeModify.DEBUG_MODE) {
                    AttributeModify.LOGGER.warn("Skipping unknown item {} while decoding standard attribute data", itemLoc);
                }
            }

            int slotsSize = buf.readInt();
            Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>> slots = new EnumMap<>(
                    EquipmentSlot.class);

            for (int j = 0; j < slotsSize; j++) {
                EquipmentSlot slot = buf.readEnum(EquipmentSlot.class);

                int attrsSize = buf.readInt();
                List<ItemAttributeDataManager.AttributeEntry> entries = new ArrayList<>();

                for (int k = 0; k < attrsSize; k++) {
                    ItemAttributeDataManager.AttributeAction action = buf
                            .readEnum(ItemAttributeDataManager.AttributeAction.class);
                    ResourceLocation attrLoc = buf.readResourceLocation();
                    Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(attrLoc);

                    AttributeModifier modifier = null;
                    if (buf.readBoolean()) {
                        UUID id = buf.readUUID();
                        // OPTIMIZED: Name field removed from packet, using stable placeholder
                        String name = "attributemodify:modifier";
                        double amount = buf.readDouble();
                        AttributeModifier.Operation operation = buf.readEnum(AttributeModifier.Operation.class);
                        modifier = new AttributeModifier(id, name, amount, operation);
                    }

                    ItemAttributeDataManager.NbtCondition nbtCondition = decodeNbtCondition(buf);

                    if (attr == null) {
                        if (AttributeModify.DEBUG_MODE) {
                            AttributeModify.LOGGER.warn("Skipping unknown attribute {} while decoding standard slot {}",
                                    attrLoc, slot);
                        }
                        continue;
                    }

                    entries.add(new ItemAttributeDataManager.AttributeEntry(attr, modifier, action, nbtCondition));
                }
                slots.put(slot, entries);
            }
            if (item != null) {
                standard.put(item, slots);
            }
        }

        int curiosSize = buf.readInt();
        for (int i = 0; i < curiosSize; i++) {
            ResourceLocation itemLoc = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
            if (item == null) {
                if (AttributeModify.DEBUG_MODE) {
                    AttributeModify.LOGGER.warn("Skipping unknown item {} while decoding Curios attribute data", itemLoc);
                }
            }

            int slotsSize = buf.readInt();
            Map<String, List<ItemAttributeDataManager.AttributeEntry>> slots = new HashMap<>();

            for (int j = 0; j < slotsSize; j++) {
                String slotName = buf.readUtf();

                int attrsSize = buf.readInt();
                List<ItemAttributeDataManager.AttributeEntry> entries = new ArrayList<>();

                for (int k = 0; k < attrsSize; k++) {
                    ItemAttributeDataManager.AttributeAction action = buf
                            .readEnum(ItemAttributeDataManager.AttributeAction.class);
                    ResourceLocation attrLoc = buf.readResourceLocation();
                    Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(attrLoc);

                    AttributeModifier modifier = null;
                    if (buf.readBoolean()) {
                        UUID id = buf.readUUID();
                        // OPTIMIZED: Name field removed from packet, using stable placeholder
                        String name = "attributemodify:curios_modifier";
                        double amount = buf.readDouble();
                        AttributeModifier.Operation operation = buf.readEnum(AttributeModifier.Operation.class);
                        modifier = new AttributeModifier(id, name, amount, operation);
                    }

                    ItemAttributeDataManager.NbtCondition nbtCondition = decodeNbtCondition(buf);

                    if (attr == null) {
                        if (AttributeModify.DEBUG_MODE) {
                            AttributeModify.LOGGER.warn("Skipping unknown attribute {} while decoding Curios slot {}",
                                    attrLoc, slotName);
                        }
                        continue;
                    }

                    entries.add(new ItemAttributeDataManager.AttributeEntry(attr, modifier, action, nbtCondition));
                }
                slots.put(slotName, entries);
            }
            if (item != null) {
                curios.put(item, slots);
            }
        }

        int durabilitySize = buf.readInt();
        for (int i = 0; i < durabilitySize; i++) {
            ResourceLocation itemLoc = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
            int value = buf.readInt();
            String modeStr = buf.readUtf();
            com.etema.attributemodify.durability.DurabilityMode mode = com.etema.attributemodify.durability.DurabilityMode.valueOf(modeStr);
            int triggerCount = buf.readInt();
            Set<String> triggers = new LinkedHashSet<>();
            for (int triggerIndex = 0; triggerIndex < triggerCount; triggerIndex++) {
                triggers.add(buf.readUtf());
            }
            if (item == null) {
                if (AttributeModify.DEBUG_MODE) {
                    AttributeModify.LOGGER.warn("Skipping unknown item {} while decoding durability rules", itemLoc);
                }
                continue;
            }
            durability.put(item, new com.etema.attributemodify.durability.DurabilityRule(value, mode, triggers));
        }

        Map<Item, List<ItemAttributeDataManager.MiningOverride>> mining = new HashMap<>();
        if (buf.isReadable()) {
            int miningSize = buf.readInt();
            for (int i = 0; i < miningSize; i++) {
                ResourceLocation itemLoc = buf.readResourceLocation();
                Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                int overrideCount = buf.readInt();
                List<ItemAttributeDataManager.MiningOverride> overrides = new ArrayList<>();

                for (int j = 0; j < overrideCount; j++) {
                    Float speed = null;
                    if (buf.readBoolean()) {
                        speed = buf.readFloat();
                    }
                    String tier = null;
                    if (buf.readBoolean()) {
                        tier = buf.readUtf();
                    }
                    ItemAttributeDataManager.NbtCondition nbtCondition = decodeNbtCondition(buf);
                    overrides.add(new ItemAttributeDataManager.MiningOverride(speed, tier, tier != null ? com.etema.attributemodify.MiningTierHandler.parseTier(tier) : null, nbtCondition));
                }

                if (item != null) {
                    mining.put(item, overrides);
                }
            }
        }

        java.util.Set<Item> decorative = new java.util.HashSet<>();
        if (buf.isReadable()) {
            int decorativeSize = buf.readInt();
            for (int i = 0; i < decorativeSize; i++) {
                ResourceLocation itemLoc = buf.readResourceLocation();
                Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                if (item != null) {
                    decorative.add(item);
                }
            }
        }

        return new SyncAttributeDataPacket(standard, curios, durability, mining, decorative);
    }

    public static void handle(SyncAttributeDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ItemAttributeDataManager.getInstance().updateFromServer(
                    packet.standardAttributes,
                    packet.curiosAttributes,
                    packet.durabilityRules,
                    packet.miningOverrides,
                    packet.decorativeItems);
            // AttributeModify.LOGGER.info("Client synchronized attribute data from
            // server");
        });
        context.setPacketHandled(true);
    }

    private static Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> deepCopyStandard(
            Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> source) {
        Map<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> copy = new HashMap<>();
        for (Map.Entry<Item, Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>>> itemEntry : source
                .entrySet()) {
            Map<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>> slotCopy = new EnumMap<>(
                    EquipmentSlot.class);
            for (Map.Entry<EquipmentSlot, List<ItemAttributeDataManager.AttributeEntry>> slotEntry : itemEntry
                    .getValue().entrySet()) {
                slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
            }
            copy.put(itemEntry.getKey(), slotCopy);
        }
        return copy;
    }

    private static Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> deepCopyCurios(
            Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> source) {
        Map<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> copy = new HashMap<>();
        for (Map.Entry<Item, Map<String, List<ItemAttributeDataManager.AttributeEntry>>> itemEntry : source
                .entrySet()) {
            Map<String, List<ItemAttributeDataManager.AttributeEntry>> slotCopy = new HashMap<>();
            for (Map.Entry<String, List<ItemAttributeDataManager.AttributeEntry>> slotEntry : itemEntry.getValue()
                    .entrySet()) {
                slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
            }
            copy.put(itemEntry.getKey(), slotCopy);
        }
        return copy;
    }

    private static List<ItemAttributeDataManager.AttributeEntry> copyEntries(
            List<ItemAttributeDataManager.AttributeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return List.copyOf(entries);
    }

    private static Map<Item, com.etema.attributemodify.durability.DurabilityRule> copyDurabilityMap(Map<Item, com.etema.attributemodify.durability.DurabilityRule> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return new HashMap<>(source);
    }

    private static Map<Item, List<ItemAttributeDataManager.MiningOverride>> copyMiningMap(
            Map<Item, List<ItemAttributeDataManager.MiningOverride>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<Item, List<ItemAttributeDataManager.MiningOverride>> copy = new HashMap<>();
        for (Map.Entry<Item, List<ItemAttributeDataManager.MiningOverride>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }
}
