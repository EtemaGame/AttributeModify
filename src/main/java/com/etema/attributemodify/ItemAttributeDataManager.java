package com.etema.attributemodify;

import com.etema.attributemodify.durability.DurabilityMode;
import com.etema.attributemodify.durability.DurabilityRule;
import com.etema.attributemodify.durability.DurabilityHelper;
import com.etema.attributemodify.handler.MiningTierHandler;
import com.etema.attributemodify.integration.CuriosIntegration;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.slf4j.Logger;

import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ItemAttributeDataManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile ItemAttributeDataManager instance;

    private final Map<Item, Map<EquipmentSlot, List<AttributeEntry>>> itemAttributes = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Item, Map<String, List<AttributeEntry>>> curiosAttributes = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Item, DurabilityRule> durabilityRules = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Item, Integer> originalDurabilityValues = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Item, QualityConfig> qualityConfigs = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Item, List<MiningOverride>> miningOverrides = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<Item> decorativeItems = java.util.Collections.synchronizedSet(new HashSet<>());
    private final List<Map.Entry<String, JsonObject>> deferredTagEntries = java.util.Collections
            .synchronizedList(new ArrayList<>());

    public record QualityLevel(JsonElement value, int weight) {
    }

    public record QualityConfig(String tagPath, String[] pathParts, Set<String> triggers, List<QualityLevel> levels, int totalWeight) {
    }

    public record MiningOverride(Float speed, String tierName, net.minecraft.world.item.Tier tier, NbtCondition nbtCondition) {
        public boolean matches(ItemStack stack) {
            if (nbtCondition == null)
                return true;
            return nbtCondition.matches(stack);
        }

        public boolean matches(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
            if (nbtCondition == null)
                return true;
            return nbtCondition.matches(stack, tag);
        }

        public net.minecraft.world.item.Tier tier() {
            return tier;
        }
    }


    public enum AttributeAction {
        ADD, // Añade modificadores encima de los vanilla (no los borra)
        MODIFY, // Reemplaza modificadores originales preservando UUID, nombre y orden
        REMOVE; // Elimina el atributo por completo

        public static AttributeAction fromString(String actionName) {
            if (actionName == null) return ADD;
            switch (actionName.toLowerCase()) {
                case "add":
                    return ADD;
                case "modify":
                    return MODIFY;
                case "remove":
                    return REMOVE;
                default:
                    LOGGER.warn("Unknown attribute action '{}' - defaulting to ADD", actionName);
                    return ADD;
            }
        }
    }

    public static final class AttributeEntry {
        private final Attribute attribute;
        private final AttributeModifier modifier;
        private final AttributeAction action;
        private final NbtCondition nbtCondition;

        public AttributeEntry(Attribute attribute, AttributeModifier modifier, AttributeAction action,
                NbtCondition nbtCondition) {
            this.attribute = attribute;
            this.modifier = modifier;
            this.action = action;
            this.nbtCondition = nbtCondition;
        }

        public Attribute attribute() {
            return attribute;
        }

        public AttributeModifier modifier() {
            return modifier;
        }

        public AttributeAction action() {
            return action;
        }

        public NbtCondition nbtCondition() {
            return nbtCondition;
        }

        public boolean matches(ItemStack stack) {
            if (nbtCondition == null) {
                return true;
            }
            return nbtCondition.matches(stack);
        }

        public boolean matches(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
            if (nbtCondition == null) {
                return true;
            }
            return nbtCondition.matches(stack, tag);
        }
    }

    public static final class NbtCondition {
        private final String path;
        private final String operator;
        private final JsonElement value;
        private final String[] pathParts;

        public NbtCondition(String path, String operator, JsonElement value) {
            this.path = path;
            this.operator = operator != null ? operator : "equals";
            this.value = value;
            this.pathParts = path.split("\\.");
        }

        public String getPath() {
            return path;
        }

        public String getOperator() {
            return operator;
        }

        public JsonElement getValue() {
            return value;
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }

            // Compatibility layer for 1.21.1 'component:' prefix
            if (path.startsWith("component:")) {
                if (path.equals("component:apotheosis:rarity")) {
                    // Map to 1.20.1 Apotheosis NBT path
                    return matchesNbt(stack, stack.getTag(), "affix_data.rarity".split("\\."), operator, value);
                }
                return false;
            }

            if (!stack.hasTag()) {
                return "not_exists".equals(operator) || ("not_equals".equals(operator) && value != null);
            }

            return matches(stack, stack.getTag());
        }

        private boolean matchesNbt(ItemStack stack, net.minecraft.nbt.CompoundTag tag, String[] parts, String op, JsonElement val) {
            if (tag == null) return "not_exists".equals(op) || "not_equals".equals(op);
            net.minecraft.nbt.Tag currentTag = tag;
            for (String part : parts) {
                if (currentTag instanceof net.minecraft.nbt.CompoundTag compound) {
                    if (compound.contains(part)) {
                        currentTag = compound.get(part);
                    } else {
                        return "not_exists".equals(op) || "not_equals".equals(op);
                    }
                } else {
                    return "not_exists".equals(op) || "not_equals".equals(op);
                }
            }
            return compareTags(currentTag, val, op);
        }

        public boolean matches(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
            if (tag == null) {
                return "not_exists".equals(operator) || ("not_equals".equals(operator) && value != null);
            }

            net.minecraft.nbt.Tag currentTag = tag;

            // Navigate path
            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                if (currentTag instanceof net.minecraft.nbt.CompoundTag compound) {
                    if (compound.contains(part)) {
                        currentTag = compound.get(part);
                    } else {
                        return "not_exists".equals(operator) || "not_equals".equals(operator);
                    }
                } else if (currentTag instanceof net.minecraft.nbt.ListTag list) {
                    try {
                        int index = Integer.parseInt(part);
                        if (index >= 0 && index < list.size()) {
                            currentTag = list.get(index);
                        } else {
                            return "not_exists".equals(operator) || "not_equals".equals(operator);
                        }
                    } catch (NumberFormatException e) {
                        return "not_exists".equals(operator) || "not_equals".equals(operator);
                    }
                } else {
                    return "not_exists".equals(operator) || "not_equals".equals(operator);
                }
            }

            // Compare value
            return compareTags(currentTag, value, operator);
        }

        private boolean compareTags(net.minecraft.nbt.Tag tag, JsonElement expectedValue, String op) {
            if (tag == null) {
                return "not_exists".equals(op) || "not_equals".equals(op);
            }

            // "exists" operator
            if ("exists".equals(op)) {
                return true;
            }

            if (expectedValue == null || expectedValue.isJsonNull()) {
                return false;
            }

            try {
                // Numeric comparisons: try if tag is numeric AND expected can be parsed as
                // number
                if (tag instanceof net.minecraft.nbt.NumericTag numericTag) {
                    Double expectedVal = null;

                    if (expectedValue.isJsonPrimitive()) {
                        if (expectedValue.getAsJsonPrimitive().isNumber()) {
                            expectedVal = expectedValue.getAsDouble();
                        } else {
                            // Try parsing string value as number (e.g. "5" -> 5.0)
                            try {
                                expectedVal = Double.parseDouble(expectedValue.getAsString());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    if (expectedVal != null) {
                        double tagVal = numericTag.getAsDouble();

                        switch (op) {
                            case "equals":
                            case "==":
                                return Math.abs(tagVal - expectedVal) < 0.00001;
                            case "not_equals":
                            case "!=":
                                return Math.abs(tagVal - expectedVal) >= 0.00001;
                            case "greater":
                            case ">":
                                return tagVal > expectedVal;
                            case "greater_or_equal":
                            case ">=":
                                return tagVal >= expectedVal;
                            case "less":
                            case "<":
                                return tagVal < expectedVal;
                            case "less_or_equal":
                            case "<=":
                                return tagVal <= expectedVal;
                            default:
                                return false;
                        }
                    }
                }

                // String comparisons
                String tagStr = tag.getAsString();
                String expectedStr = expectedValue.isJsonPrimitive() ? expectedValue.getAsString()
                        : expectedValue.toString();

                switch (op) {
                    case "equals":
                    case "==":
                        return tagStr.equals(expectedStr);
                    case "not_equals":
                    case "!=":
                        return !tagStr.equals(expectedStr);
                    case "contains":
                        return tagStr.contains(expectedStr);
                    case "starts_with":
                        return tagStr.startsWith(expectedStr);
                    case "ends_with":
                        return tagStr.endsWith(expectedStr);
                    case "matches_regex":
                        try {
                            return tagStr.matches(expectedStr);
                        } catch (java.util.regex.PatternSyntaxException e) {
                            LOGGER.error("Invalid regex pattern '{}': {}", expectedStr, e.getMessage());
                            return false;
                        }
                    case "greater":
                    case ">":
                        return tagStr.compareTo(expectedStr) > 0;
                    case "greater_or_equal":
                    case ">=":
                        return tagStr.compareTo(expectedStr) >= 0;
                    case "less":
                    case "<":
                        return tagStr.compareTo(expectedStr) < 0;
                    case "less_or_equal":
                    case "<=":
                        return tagStr.compareTo(expectedStr) <= 0;
                    default:
                        return false;
                }
            } catch (Exception e) {
                LOGGER.debug("Error comparing NBT tag (op={}, path={}): {}", op, path, e.getMessage());
                return false;
            }
        }
    }

    public ItemAttributeDataManager() {
        super(GSON, "item_attributes");
        instance = this;
    }


    private String getItemName(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.toString() : String.valueOf(item);
    }


    private void applyDurabilityOverride(Item item, int durability) {
        applyDurabilityOverride(item, durability, Set.of());
    }

    private void applyDurabilityOverride(Item item, int durability, Set<String> triggers) {
        if (durability <= 0) {
            LOGGER.warn("Ignoring invalid durability {} for item {}", durability, getItemName(item));
            return;
        }

        com.etema.attributemodify.durability.DurabilityMode mode = com.etema.attributemodify.durability.DurabilityHelper.resolveMode(item, durability);
        Set<String> effectiveTriggers = mode == com.etema.attributemodify.durability.DurabilityMode.CUSTOM
                ? (triggers != null ? triggers : Set.of())
                : Set.of();
        com.etema.attributemodify.durability.DurabilityRule rule = new com.etema.attributemodify.durability.DurabilityRule(
                durability,
                mode,
                effectiveTriggers);
        applyDurabilityRule(item, rule);
    }

    private void applyDurabilityRule(Item item, com.etema.attributemodify.durability.DurabilityRule rule) {
        if (rule.mode() == com.etema.attributemodify.durability.DurabilityMode.CUSTOM) {
            if (com.etema.attributemodify.durability.DurabilityHelper.isVanillaDamageableTarget(item)) {
                durabilityRules.remove(item);
                LOGGER.debug("Skipping durability {} for {} -> mode CUSTOM because the item is already vanilla damageable",
                        rule.maxDurability(), getItemName(item));
                return;
            }

            if (!com.etema.attributemodify.durability.DurabilityHelper.isCustomDurabilitySupported(item)) {
                durabilityRules.remove(item);
                LOGGER.debug("Skipping durability {} for {} -> mode CUSTOM because the item stacks to {}. Max stack size 1 required.",
                            rule.maxDurability(), getItemName(item), item.getDefaultInstance().getMaxStackSize());
                return;
            }
            
            durabilityRules.put(item, rule);
            LOGGER.debug("Registered durability {} for {} -> mode CUSTOM (triggers: {})",
                        rule.maxDurability(), getItemName(item), rule.hasTriggers() ? rule.triggers() : "none");
        } else if (rule.mode() == com.etema.attributemodify.durability.DurabilityMode.VANILLA_OVERRIDE) {
            int original = item.getMaxDamage(item.getDefaultInstance());
            if (original <= 0) {
                LOGGER.debug("Skipping durability override for {} because it is not damageable", getItemName(item));
                durabilityRules.remove(item);
                return;
            }

            originalDurabilityValues.putIfAbsent(item, original);
            durabilityRules.put(item, rule);

            LOGGER.debug("Registered virtual durability override {} for {} (Original: {})", 
                rule.maxDurability(), getItemName(item), original);
        }
    }

    public static ItemAttributeDataManager getInstance() {
        if (instance == null) {
            instance = new ItemAttributeDataManager();
        }
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager,
            ProfilerFiller profiler) {

        itemAttributes.clear();
        curiosAttributes.clear();
        qualityConfigs.clear();
        miningOverrides.clear();
        decorativeItems.clear();
        deferredTagEntries.clear();
        durabilityRules.clear();

        int loadedFiles = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            ResourceLocation location = entry.getKey();

            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                processAttributeFile(json, location);
                loadedFiles++;
            } catch (Exception e) {
                LOGGER.error("Error loading attribute file {}: {}", location, e.getMessage());
            }
        }
        // Create summary line
        LOGGER.info("AttributeModify - Loaded {} attribute files.", loadedFiles);

        int totalEntries = 0;
        for (Map.Entry<Item, Map<EquipmentSlot, List<AttributeEntry>>> itemEntry : itemAttributes.entrySet()) {
            for (List<AttributeEntry> entries : itemEntry.getValue().values()) {
                totalEntries += entries.size();
            }
        }
        int totalCuriosEntries = 0;
        for (Map.Entry<Item, Map<String, List<AttributeEntry>>> itemEntry : curiosAttributes.entrySet()) {
            for (List<AttributeEntry> entries : itemEntry.getValue().values()) {
                totalCuriosEntries += entries.size();
            }
        }

        LOGGER.info(
                "  -> Details: {} items ({} attr), {} curios ({} attr), {} durability, {} quality, {} mining, {} decorative, {} deferred tags",
                itemAttributes.size(), totalEntries, curiosAttributes.size(), totalCuriosEntries,
                durabilityRules.size(), qualityConfigs.size(), miningOverrides.size(), decorativeItems.size(),
                deferredTagEntries.size());
    }

    /**
     * Resolves tag entries that were deferred during initial load because tags
     * weren't available yet.
     * Called from TagsUpdatedEvent handler.
     */
    public void resolveDeferredTags() {
        if (deferredTagEntries.isEmpty()) {
            return;
        }

        LOGGER.info("Resolving {} deferred tag entries...", deferredTagEntries.size());
        int resolved = 0;

        for (Map.Entry<String, JsonObject> deferred : deferredTagEntries) {
            String entryKey = deferred.getKey();
            JsonObject tagData = deferred.getValue();

            try {
                ResourceLocation tagLocation = ResourceLocation.tryParse(entryKey.substring(1));
                TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagLocation);
                ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);

                if (tag.isEmpty()) {
                    LOGGER.warn("Tag {} still not found or empty after deferred resolution", entryKey);
                    continue;
                }

                for (Item item : tag) {
                    processItemAttributes(item, tagData);
                }
                resolved++;
            } catch (Exception e) {
                LOGGER.error("Error resolving deferred tag {}: {}", entryKey, e.getMessage());
            }
        }

        if (resolved > 0) {
            LOGGER.info("Resolved {}/{} deferred tag entries", resolved, deferredTagEntries.size());
        }
        deferredTagEntries.clear();
    }

    private void processAttributeFile(JsonObject json, ResourceLocation location) {
        for (Map.Entry<String, JsonElement> itemEntry : json.entrySet()) {
            String entryKey = itemEntry.getKey();

            if (entryKey.startsWith("#")) {
                try {
                    ResourceLocation tagLocation = ResourceLocation.tryParse(entryKey.substring(1));
                    TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagLocation);
                    ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);

                    JsonObject tagData = itemEntry.getValue().getAsJsonObject();

                    if (tag.isEmpty()) {
                        // Tags may not be loaded yet on first world load; defer for later resolution
                        LOGGER.debug("Tag {} not yet available, deferring for resolution (from {})", entryKey, location);
                        deferredTagEntries.add(Map.entry(entryKey, tagData));
                        continue;
                    }

                    for (Item item : tag) {
                        processItemAttributes(item, tagData);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing tag {} in {}: {}", entryKey, location, e.getMessage());
                }
            } else {
                ResourceLocation itemLocation = ResourceLocation.tryParse(entryKey);
                Item item = ForgeRegistries.ITEMS.getValue(itemLocation);

                if (item == null || item == net.minecraft.world.item.Items.AIR) {
                    LOGGER.warn(
                            "Item '{}' not found in registry (from {}). Check if the mod providing this item is installed and the ID is correct.",
                            entryKey, location);
                    continue;
                }

                LOGGER.debug("Processing item '{}' from {}", entryKey, location);
                JsonObject itemData = itemEntry.getValue().getAsJsonObject();
                processItemAttributes(item, itemData);
            }
        }
    }

    private void processItemAttributes(Item item, JsonObject itemData) {
        // Removed legacy processItemAttributes and version_overrides logic to save resources
        // The itemData passed here is already the final, potentially version-overridden data
        // if version_overrides were handled upstream (e.g., in a pre-processing step
        // or if they are no longer supported).
        // For this instruction, we assume itemData is ready to be processed.

        if (itemData == null || itemData.size() == 0) {
            LOGGER.debug("No attribute data found for item {}", ForgeRegistries.ITEMS.getKey(item));
            return;
        }

        applyAttributesFromJson(item, itemData);
    }

    private void applyAttributesFromJson(Item item, JsonObject itemData) {
        Map<EquipmentSlot, List<AttributeEntry>> standardData = new EnumMap<>(EquipmentSlot.class);
        Map<String, List<AttributeEntry>> curiosData = new HashMap<>();
        String itemKey = ForgeRegistries.ITEMS.getKey(item).toString();

        // 1. Specific standard slots
        if (itemData.has("equipment_slots") && itemData.get("equipment_slots").isJsonObject()) {
            JsonObject slotsData = itemData.getAsJsonObject("equipment_slots");
            for (Map.Entry<String, JsonElement> slotEntry : slotsData.entrySet()) {
                String slotName = slotEntry.getKey().toLowerCase();
                EquipmentSlot slot = parseEquipmentSlot(slotName);
                if (slot != null && slotEntry.getValue().isJsonArray()) {
                    standardData.put(slot, parseAttributeEntries(item, slotEntry.getValue().getAsJsonArray(), itemKey, slotName));
                }
            }
        }

        // 2. Specific curios slots
        if (itemData.has("curios_slots") && itemData.get("curios_slots").isJsonObject()) {
            if (CuriosIntegration.shouldProcessCuriosSlots()) {
                JsonObject curiosJson = itemData.getAsJsonObject("curios_slots");
                for (Map.Entry<String, JsonElement> curiosEntry : curiosJson.entrySet()) {
                    String curiosSlot = curiosEntry.getKey();
                    if (curiosEntry.getValue().isJsonArray()) {
                        curiosData.put(curiosSlot, parseAttributeEntries(item, curiosEntry.getValue().getAsJsonArray(), itemKey, curiosSlot));
                    }
                }
            }
        }

        // 3. Shorthand top-level 'attributes' array (Auto-detect slot)
        if (itemData.has("attributes") && itemData.get("attributes").isJsonArray()) {
            processAutoSlotAttributes(item, itemData.getAsJsonArray("attributes"), standardData, curiosData);
        }

        // 4. Shorthand top-level single attribute (Auto-detect slot)
        if (itemData.has("attribute") && itemData.has("amount")) {
            JsonArray singleAttrArray = new JsonArray();
            singleAttrArray.add(itemData); // Use the itemData itself as the attribute object
            processAutoSlotAttributes(item, singleAttrArray, standardData, curiosData);
        }

        // 5. Merge collected data into global maps
        if (!standardData.isEmpty()) {
            Map<EquipmentSlot, List<AttributeEntry>> existing = itemAttributes.computeIfAbsent(item, k -> new EnumMap<>(EquipmentSlot.class));
            for (Map.Entry<EquipmentSlot, List<AttributeEntry>> entry : standardData.entrySet()) {
                existing.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
            }
        }

        if (!curiosData.isEmpty()) {
            Map<String, List<AttributeEntry>> existing = curiosAttributes.computeIfAbsent(item, k -> new HashMap<>());
            for (Map.Entry<String, List<AttributeEntry>> entry : curiosData.entrySet()) {
                existing.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
            }
        }

        if (itemData.has("durability") && !itemData.get("durability").isJsonNull()) {
            try {
                JsonElement durabilityElement = itemData.get("durability");
                if (!durabilityElement.isJsonPrimitive() || !durabilityElement.getAsJsonPrimitive().isNumber()) {
                    LOGGER.warn("Durability value for item {} is not a number", getItemName(item));
                } else {
                    int durability = durabilityElement.getAsInt();
                    Set<String> durabilityTriggers = parseDurabilityTriggers(item, itemData);
                    applyDurabilityOverride(item, durability, durabilityTriggers);
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing durability for item {}: {}", getItemName(item), e.getMessage());
            }
        }
    }

    private void processAutoSlotAttributes(Item item, JsonArray attributesArray,
            Map<EquipmentSlot, List<AttributeEntry>> standardData,
            Map<String, List<AttributeEntry>> curiosData) {
        if (attributesArray == null || attributesArray.size() == 0) {
            return;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        String itemKey = itemId != null ? itemId.toString() : "unknown_item";

        EquipmentSlot autoStandard = autoDetectSlot(item);
        if (autoStandard != null) {
            List<AttributeEntry> attributes = parseAttributeEntries(item, attributesArray, itemKey, autoStandard.getName());
            standardData.computeIfAbsent(autoStandard, k -> new ArrayList<>()).addAll(attributes);
                LOGGER.debug("Auto-detected standard slot '{}' for item {}", autoStandard.getName(), itemKey);
            return;
        }

        String autoCurio = autoDetectCuriosSlot(item);
        if (autoCurio != null) {
            List<AttributeEntry> attributes = parseAttributeEntries(item, attributesArray, itemKey, autoCurio);
            curiosData.computeIfAbsent(autoCurio, k -> new ArrayList<>()).addAll(attributes);
                LOGGER.debug("Auto-detected Curios slot '{}' for item {}", autoCurio, itemKey);
            return;
        }

        LOGGER.warn("Could not auto-detect slot for item {} with top-level 'attributes'", itemKey);
    }

    private EquipmentSlot autoDetectSlot(Item item) {
        if (item instanceof net.minecraft.world.item.ArmorItem armor) {
            return armor.getEquipmentSlot();
        }
        if (item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.TridentItem) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    private String autoDetectCuriosSlot(Item item) {
        if (!CuriosIntegration.shouldProcessCuriosSlots()) {
            return null;
        }

        if (hasCuriosTag(item, "ring")) {
            return "ring";
        }
        if (hasCuriosTag(item, "necklace")) {
            return "necklace";
        }
        if (hasCuriosTag(item, "belt")) {
            return "belt";
        }
        if (hasCuriosTag(item, "charm")) {
            return "charm";
        }

        return null;
    }

    private boolean hasCuriosTag(Item item, String slotName) {
        ResourceLocation tagLocation = ResourceLocation.tryParse("curios:" + slotName);
        if (tagLocation == null) {
            return false;
        }

        TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagLocation);
        ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);
        if (tag.isEmpty()) {
            return false;
        }

        for (Item taggedItem : tag) {
            if (taggedItem == item) {
                return true;
            }
        }

        return false;
    }

    private List<AttributeEntry> parseAttributeEntries(Item item, JsonArray attributesArray, String itemKey, String slotKey) {
        List<AttributeEntry> entries = new ArrayList<>();

        String itemSafe = (itemKey == null ? "unknown_item" : itemKey)
                .replace(":", "_")
                .replace(".", "_")
                .toLowerCase();

        String slotSafe = (slotKey == null ? "unknown_slot" : slotKey)
                .replace(":", "_")
                .replace(".", "_")
                .toLowerCase();

        for (JsonElement element : attributesArray) {
            if (!element.isJsonObject()) continue;
            JsonObject attrObj = element.getAsJsonObject();

            try {
                if (attrObj.has("durability") && !attrObj.has("attribute")) {
                    try {
                        int durability = attrObj.get("durability").getAsInt();
                        Set<String> durabilityTriggers = parseDurabilityTriggers(item, attrObj);
                        applyDurabilityOverride(item, durability, durabilityTriggers);
                        LOGGER.debug("  -> Parsed durability override {} for item '{}' (from slot '{}')", 
                            durability, itemKey, slotKey);
                    } catch (Exception e) {
                        LOGGER.error("Error parsing durability in attribute list (item {} slot {}): {}", 
                            itemKey, slotKey, e.getMessage());
                    }
                    continue;
                }

                if (!attrObj.has("attribute")) {
                    LOGGER.error("Missing 'attribute' field in entry for item {} slot {}", itemKey, slotKey);
                    continue;
                }

                String attributeId = attrObj.get("attribute").getAsString();
                AttributeAction action = attrObj.has("action") ? AttributeAction.fromString(attrObj.get("action").getAsString()) : AttributeAction.ADD;

                ResourceLocation attrLocation = ResourceLocation.tryParse(attributeId);
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrLocation);

                if (attribute == null) {
                    LOGGER.warn("Attribute '{}' not found in registry (item '{}', slot '{}'). Check if the mod providing this attribute is installed.",
                            attributeId, itemKey, slotKey);
                    continue;
                }

                LOGGER.debug("  -> Parsed attribute '{}' action={} for item '{}' slot '{}'", attributeId, action, itemKey, slotKey);

                NbtCondition nbtCondition = null;
                if (attrObj.has("nbt") && attrObj.get("nbt").isJsonObject()) {
                    JsonObject nbtObj = attrObj.get("nbt").getAsJsonObject();
                    String path = nbtObj.has("path") ? nbtObj.get("path").getAsString()
                            : (nbtObj.has("key") ? nbtObj.get("key").getAsString() : null);
                    String operator = nbtObj.has("operator") ? nbtObj.get("operator").getAsString() : "equals";
                    JsonElement value = nbtObj.get("value");

                    if (path != null) {
                        nbtCondition = new NbtCondition(path, operator, value);
                    }
                }

                if (action == AttributeAction.REMOVE) {
                    entries.add(new AttributeEntry(attribute, null, action, nbtCondition));
                } else {
                    if (!attrObj.has("amount") && action != AttributeAction.REMOVE) {
                        LOGGER.warn("Missing 'amount' for action {} on attribute {} (item {}, slot {})", action, attributeId, itemKey, slotKey);
                    }

                    String attrSafe = attributeId
                            .replace("minecraft:", "")
                            .replace(":", "_")
                            .replace(".", "_")
                            .toLowerCase();

                    String modifierIdString = attrObj.has("modifier_id") ? attrObj.get("modifier_id").getAsString()
                            : AttributeModify.MODID + ":modifier_" + itemSafe + "_" + slotSafe + "_" + attrSafe;

                    double amount = attrObj.has("amount") ? attrObj.get("amount").getAsDouble() : 0.0;
                    String operationStr = attrObj.has("operation") ? attrObj.get("operation").getAsString() : "addition";
                    AttributeModifier.Operation operation = parseOperation(operationStr);

                    UUID uuid;
                    if (attrObj.has("uuid")) {
                        try {
                            uuid = UUID.fromString(attrObj.get("uuid").getAsString());
                        } catch (Exception e) {
                            uuid = UUID.nameUUIDFromBytes(modifierIdString.getBytes());
                            LOGGER.error("Invalid UUID format '{}' for attribute {} (item {}). Using generated UUID.", attrObj.get("uuid").getAsString(), attributeId, itemKey);
                        }
                    } else {
                        uuid = UUID.nameUUIDFromBytes(modifierIdString.getBytes());
                    }

                    if (nbtCondition != null) {
                        String nbtDisc = nbtCondition.path + "_" + nbtCondition.operator + "_"
                                + (nbtCondition.value != null ? nbtCondition.value.toString() : "null");
                        uuid = UUID.nameUUIDFromBytes((modifierIdString + "_" + nbtDisc).getBytes());
                    }

                    AttributeModifier modifier = new AttributeModifier(uuid, modifierIdString, amount, operation);
                    entries.add(new AttributeEntry(attribute, modifier, action, nbtCondition));
                }

            } catch (Exception e) {
                LOGGER.error("Error parsing attribute entry (item {} slot {}): {}", itemKey, slotKey, e.getMessage());
            }
        }

        return entries;
    }

    private EquipmentSlot parseEquipmentSlot(String slotName) {
        switch (slotName) {
            case "mainhand":
                return EquipmentSlot.MAINHAND;
            case "offhand":
                return EquipmentSlot.OFFHAND;
            case "feet":
                return EquipmentSlot.FEET;
            case "legs":
                return EquipmentSlot.LEGS;
            case "chest":
                return EquipmentSlot.CHEST;
            case "head":
                return EquipmentSlot.HEAD;
            default:
                return null;
        }
    }

    private AttributeModifier.Operation parseOperation(String operationName) {
        switch (operationName.toLowerCase()) {
            case "addition":
            case "add":
            case "add_value":
                return AttributeModifier.Operation.ADDITION;
            case "multiply_base":
            case "multiply":
            case "add_multiplied_base":
                return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total":
            case "add_multiplied_total":
                return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
                LOGGER.warn("Unknown operation '{}', defaulting to ADDITION", operationName);
                return AttributeModifier.Operation.ADDITION;
        }
    }

    public List<AttributeEntry> getEntriesForSlot(Item item, EquipmentSlot slot) {
        Map<EquipmentSlot, List<AttributeEntry>> itemData = itemAttributes.get(item);
        if (itemData == null) {
            return java.util.Collections.emptyList();
        }

        List<AttributeEntry> entries = itemData.get(slot);
        return entries != null ? entries : java.util.Collections.emptyList();
    }

    public List<AttributeEntry> getEntriesForCuriosSlot(Item item, String curiosSlot) {
        Map<String, List<AttributeEntry>> itemData = curiosAttributes.get(item);
        if (itemData == null) {
            return java.util.Collections.emptyList();
        }

        List<AttributeEntry> entries = itemData.get(curiosSlot);
        return entries != null ? entries : java.util.Collections.emptyList();
    }

    public boolean hasCustomAttributes(Item item) {
        return itemAttributes.containsKey(item) || curiosAttributes.containsKey(item) || isDecorative(item);
    }

    public boolean isDecorative(Item item) {
        return decorativeItems.contains(item);
    }
    public Map<Item, Map<EquipmentSlot, List<AttributeEntry>>> getStandardAttributesForSync() {
        Map<Item, Map<EquipmentSlot, List<AttributeEntry>>> copy = new HashMap<>();

        for (Map.Entry<Item, Map<EquipmentSlot, List<AttributeEntry>>> itemEntry : itemAttributes.entrySet()) {
            Map<EquipmentSlot, List<AttributeEntry>> slotCopy = new EnumMap<>(EquipmentSlot.class);
            for (Map.Entry<EquipmentSlot, List<AttributeEntry>> slotEntry : itemEntry.getValue().entrySet()) {
                slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
            }
            copy.put(itemEntry.getKey(), slotCopy);
        }

        return copy;
    }

    public Map<Item, Map<String, List<AttributeEntry>>> getCuriosAttributesForSync() {
        Map<Item, Map<String, List<AttributeEntry>>> copy = new HashMap<>();
        for (Map.Entry<Item, Map<String, List<AttributeEntry>>> itemEntry : curiosAttributes.entrySet()) {
            Map<String, List<AttributeEntry>> slotCopy = new HashMap<>();
            for (Map.Entry<String, List<AttributeEntry>> slotEntry : itemEntry.getValue().entrySet()) {
                slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
            }
            copy.put(itemEntry.getKey(), slotCopy);
        }
        return copy;
    }

    public Map<Item, com.etema.attributemodify.durability.DurabilityRule> getDurabilityRulesForSync() {
        return new HashMap<>(durabilityRules);
    }

    public com.etema.attributemodify.durability.DurabilityRule getDurabilityRule(Item item) {
        return durabilityRules.get(item);
    }

    public int getOriginalVanillaDurability(Item item) {
        if (item == null) return 0;
        return originalDurabilityValues.getOrDefault(item, 0);
    }

    public Set<Item> getDecorativeItemsForSync() {
        return new HashSet<>(decorativeItems);
    }

    public void updateFromServer(Map<Item, Map<EquipmentSlot, List<AttributeEntry>>> standardData,
            Map<Item, Map<String, List<AttributeEntry>>> curiosData,
            Map<Item, com.etema.attributemodify.durability.DurabilityRule> durabilityData,
            Map<Item, List<MiningOverride>> miningData,
            Set<Item> decorativeData) {
        itemAttributes.clear();
        curiosAttributes.clear();
        miningOverrides.clear();
        decorativeItems.clear();
        durabilityRules.clear();

        if (standardData != null) {
            for (Map.Entry<Item, Map<EquipmentSlot, List<AttributeEntry>>> itemEntry : standardData.entrySet()) {
                Map<EquipmentSlot, List<AttributeEntry>> slotCopy = new EnumMap<>(EquipmentSlot.class);
                for (Map.Entry<EquipmentSlot, List<AttributeEntry>> slotEntry : itemEntry.getValue().entrySet()) {
                    slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
                }
                itemAttributes.put(itemEntry.getKey(), slotCopy);
            }
        }

        if (curiosData != null) {
            for (Map.Entry<Item, Map<String, List<AttributeEntry>>> itemEntry : curiosData.entrySet()) {
                Map<String, List<AttributeEntry>> slotCopy = new HashMap<>();
                for (Map.Entry<String, List<AttributeEntry>> slotEntry : itemEntry.getValue().entrySet()) {
                    slotCopy.put(slotEntry.getKey(), copyEntries(slotEntry.getValue()));
                }
                curiosAttributes.put(itemEntry.getKey(), slotCopy);
            }
        }

        if (durabilityData != null) {
            for (Map.Entry<Item, com.etema.attributemodify.durability.DurabilityRule> entry : durabilityData.entrySet()) {
                Item item = entry.getKey();
                com.etema.attributemodify.durability.DurabilityRule rule = entry.getValue();
                if (item != null && rule != null) {
                    applyDurabilityRule(item, rule);
                }
            }
        }

        if (miningData != null) {
            for (Map.Entry<Item, List<MiningOverride>> entry : miningData.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    miningOverrides.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
            }
        }

        if (decorativeData != null) {
            decorativeItems.addAll(decorativeData);
        }
    }

    private List<AttributeEntry> copyEntries(List<AttributeEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return List.copyOf(entries);
    }

    public QualityConfig getQualityConfig(Item item) {
        return qualityConfigs.get(item);
    }

    private QualityConfig parseQualityConfig(Item item, JsonObject qualityJson) {
        String tagPath = qualityJson.has("tag_path") ? qualityJson.get("tag_path").getAsString() : "quality";

        Set<String> triggers = new HashSet<>();
        if (qualityJson.has("triggers") && qualityJson.get("triggers").isJsonArray()) {
            for (JsonElement triggerEl : qualityJson.getAsJsonArray("triggers")) {
                triggers.add(triggerEl.getAsString().toLowerCase());
            }
        } else {
            triggers.add("craft");
        }

        List<QualityLevel> levels = new ArrayList<>();
        int totalWeight = 0;

        if (qualityJson.has("levels") && qualityJson.get("levels").isJsonArray()) {
            for (JsonElement levelEl : qualityJson.getAsJsonArray("levels")) {
                JsonObject levelObj = levelEl.getAsJsonObject();
                JsonElement value = levelObj.get("value");
                int weight = levelObj.has("weight") ? levelObj.get("weight").getAsInt() : 1;

                if (value == null) {
                    LOGGER.warn("Quality level without value for item {}", getItemName(item));
                    continue;
                }
                if (weight <= 0) {
                    LOGGER.warn("Quality level with non-positive weight {} for item {}", weight, getItemName(item));
                    continue;
                }

                levels.add(new QualityLevel(value, weight));
                totalWeight += weight;
            }
        }

        if (levels.isEmpty()) {
            LOGGER.warn("Quality system for item {} has no valid levels", getItemName(item));
            return null;
        }

        return new QualityConfig(tagPath, tagPath.split("\\."), triggers, levels, totalWeight);
    }

    private List<MiningOverride> parseMiningOverrides(Item item, JsonArray miningArray) {
        List<MiningOverride> overrides = new ArrayList<>();

        for (JsonElement element : miningArray) {
            if (!element.isJsonObject()) {
                LOGGER.warn("Mining override entry for item {} is not an object", getItemName(item));
                continue;
            }

            JsonObject obj = element.getAsJsonObject();

            Float speed = null;
            if (obj.has("speed") && obj.get("speed").isJsonPrimitive()) {
                speed = obj.get("speed").getAsFloat();
            }

            String tier = null;
            if (obj.has("tier") && obj.get("tier").isJsonPrimitive()) {
                tier = obj.get("tier").getAsString().toLowerCase();
            }

            if (speed == null && tier == null) {
                LOGGER.warn("Mining override for item {} has neither speed nor tier", getItemName(item));
                continue;
            }

            NbtCondition nbtCondition = null;
            if (obj.has("nbt") && obj.get("nbt").isJsonObject()) {
                JsonObject nbtObj = obj.getAsJsonObject("nbt");
                String path = nbtObj.has("path") ? nbtObj.get("path").getAsString()
                        : (nbtObj.has("key") ? nbtObj.get("key").getAsString() : null);
                String operator = nbtObj.has("operator") ? nbtObj.get("operator").getAsString() : "equals";
                JsonElement value = nbtObj.get("value");

                if (path != null) {
                    nbtCondition = new NbtCondition(path, operator, value);
                }
            }

            overrides.add(new MiningOverride(speed, tier, tier != null ? MiningTierHandler.parseTier(tier) : null, nbtCondition));
        }

        return overrides;
    }

    private Set<String> parseDurabilityTriggers(Item item, JsonObject itemData) {
        if (!itemData.has("durability_triggers") || itemData.get("durability_triggers").isJsonNull()) {
            return Set.of();
        }

        JsonElement triggersElement = itemData.get("durability_triggers");
        if (!triggersElement.isJsonArray()) {
            LOGGER.warn("durability_triggers for item {} must be an array", getItemName(item));
            return Set.of();
        }

        Set<String> triggers = new LinkedHashSet<>();
        for (JsonElement triggerElement : triggersElement.getAsJsonArray()) {
            if (!triggerElement.isJsonPrimitive() || !triggerElement.getAsJsonPrimitive().isString()) {
                LOGGER.warn("Ignoring non-string durability trigger for item {}", getItemName(item));
                continue;
            }

            String trigger = triggerElement.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!DurabilityRule.isSupportedTrigger(trigger)) {
                LOGGER.warn("Ignoring unsupported durability trigger '{}' for item {}. Supported triggers: {}",
                        trigger, getItemName(item), DurabilityRule.supportedTriggers());
                continue;
            }

            triggers.add(trigger);
        }

        if (triggers.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(triggers);
    }

    public List<MiningOverride> getMiningOverrides(Item item) {
        List<MiningOverride> overrides = miningOverrides.get(item);
        return overrides != null ? List.copyOf(overrides) : List.of();
    }

    public Map<Item, List<MiningOverride>> getMiningOverridesForSync() {
        Map<Item, List<MiningOverride>> copy = new HashMap<>();
        for (Map.Entry<Item, List<MiningOverride>> entry : miningOverrides.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public void overrideItem(Item item, Map<EquipmentSlot, List<AttributeEntry>> overrides) {
        if (item == null || overrides == null || overrides.isEmpty()) {
            return;
        }

        Map<EquipmentSlot, List<AttributeEntry>> slots = itemAttributes.computeIfAbsent(item,
                key -> new EnumMap<>(EquipmentSlot.class));

        for (Map.Entry<EquipmentSlot, List<AttributeEntry>> entry : overrides.entrySet()) {
            EquipmentSlot slot = entry.getKey();
            if (slot == null) {
                continue;
            }

            List<AttributeEntry> values = entry.getValue();
            if (values == null) {
                continue;
            }

            slots.put(slot, copyEntries(values));
        }
    }

    public void overrideItemDurability(Item item, Integer durability) {
        if (item == null || durability == null) {
            return;
        }
        applyDurabilityOverride(item, durability);
    }

    public void overrideItemDurability(Item item, Integer durability, Set<String> triggers) {
        if (item == null || durability == null) {
            return;
        }
        applyDurabilityOverride(item, durability, triggers);
    }

}
