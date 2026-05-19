package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.editor.EditorClientState;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableCondition;
import com.etema.attributemodify.editor.model.EditableDurabilityModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableMiningOverride;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableQualityLevel;
import com.etema.attributemodify.editor.model.EditableQualitySystem;
import com.etema.attributemodify.editor.model.EditableSlotType;
import com.etema.attributemodify.editor.network.C2SRequestEditorCatalogPacket;
import com.etema.attributemodify.editor.network.C2SRequestItemRulePacket;
import com.etema.attributemodify.editor.network.C2SSaveItemRulePacket;
import com.etema.attributemodify.editor.network.EditorNetwork;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AttributeModifyEditorScreen extends Screen {
    private static final int BG = 0xFF090D12;
    private static final int PANEL = 0xF211171D;
    private static final int PANEL_ALT = 0xEE151D25;
    private static final int PANEL_DEEP = 0xFF0B1015;
    private static final int BORDER = 0xFF2A3542;
    private static final int BORDER_SOFT = 0xFF1D2630;
    private static final int SEL_BG = 0xFF163D30;
    private static final int HEADER_BG = 0xFF0C1218;
    private static final int INPUT_BG = 0xFF070B0F;
    private static final int HOVER_TINT = 0x1FFFFFFF;
    private static final int TEXT = 0xFFE8EEF5;
    private static final int MUTED = 0xFF9EACB8;
    private static final int TEXT_DIM = 0xFF8EACBE;
    private static final int TEXT_MUTED = 0xFF5F7889;
    private static final int ACCENT = 0xFF69C6A4;
    private static final int ACCENT2 = 0xFF3BA87D;
    private static final int WARN = 0xFFFFB74D;
    private static final int ERROR = 0xFFFF8A80;
    private static final String[] STANDARD_SLOTS = {"mainhand", "offhand", "head", "chest", "body", "legs", "feet"};
    private static final int TOP_BAR_H = 26;
    private static final int FOOTER_H = 20;
    private static final int OUTER_MARGIN = 8;
    private static final int PANEL_GAP = 6;
    private static final int ROW_H = 22;
    private static final int CONTROL_H = 17;
    private static final int BUTTON_H = 15;
    private static final int LIST_MIN_W = 184;
    private static final int LIST_MAX_W = 280;
    private static final int EDITOR_X = 260;
    private static final int FORM_Y = 104;
    private static final int DURABILITY_Y = 326;

    private final List<CatalogItem> allItems = new ArrayList<>();
    private final List<CatalogItem> filteredItems = new ArrayList<>();
    private final List<ResourceLocation> attributes = new ArrayList<>();
    private final List<BaseAttribute> baseAttributes = new ArrayList<>();

    private EditBox searchBox;
    private EditBox targetBox;
    private EditBox attributeBox;
    private EditBox amountBox;
    private EditBox slotBox;
    private EditBox conditionPathBox;
    private EditBox conditionValueBox;
    private EditBox durabilityBox;
    private EditBox durabilityTriggersBox;
    private EditBox miningSpeedBox;
    private EditBox qualityPathBox;
    private EditBox qualityLevelsBox;
    private EditBox qualityTriggersBox;
    private Button targetModeButton;
    private Button actionButton;
    private Button operationButton;
    private Button slotTypeButton;
    private Button slotButton;
    private Button conditionOperatorButton;
    private Button miningTierButton;
    private Button qualityButton;
    private Button decorativeButton;
    private Button useBaseButton;
    private Button saveButton;
    private Button closeButton;

    private CatalogItem selectedItem;
    private EditableItemRule currentRule;
    private EditableAttributeAction action = EditableAttributeAction.MODIFY;
    private EditableOperationType operation = EditableOperationType.ADDITION;
    private EditableSlotType slotType = EditableSlotType.AUTO;
    private String slot = "mainhand";
    private String conditionOperator = "equals";
    private String miningTier = "";
    private boolean tagTarget;
    private boolean qualityEnabled;
    private boolean decorative;
    private String lastCatalogJson = "";
    private String lastRuleJson = "";
    private String lastSaveJson = "";
    private String status = "Loading catalog...";
    private boolean externalConflict;
    private int selectedBaseAttributeIndex;
    private int scroll;

    public AttributeModifyEditorScreen() {
        super(Component.literal("AttributeModify Editor"));
    }

    @Override
    protected void init() {
        int margin = 18;
        searchBox = new EditBox(font, margin, 28, 220, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search item id or translated name"));
        searchBox.setBordered(false);
        searchBox.setResponder(value -> {
            scroll = 0;
            refreshFilter();
        });
        addRenderableWidget(searchBox);

        targetBox = new EditBox(font, EDITOR_X, 52, 220, 20, Component.literal("Target"));
        targetBox.setHint(Component.literal("item or tag id"));
        targetBox.setBordered(false);
        addRenderableWidget(targetBox);

        attributeBox = new EditBox(font, EDITOR_X, FORM_Y, 220, 20, Component.literal("Attribute"));
        attributeBox.setHint(Component.literal("attribute id"));
        attributeBox.setBordered(false);
        addRenderableWidget(attributeBox);

        amountBox = new EditBox(font, EDITOR_X, FORM_Y + 48, 88, 20, Component.literal("Amount"));
        amountBox.setHint(Component.literal("amount"));
        amountBox.setBordered(false);
        amountBox.setFilter(this::isNumericInput);
        addRenderableWidget(amountBox);

        slotBox = new EditBox(font, EDITOR_X + 224, FORM_Y + 72, 82, 20, Component.literal("Slot"));
        slotBox.setHint(Component.literal("slot"));
        slotBox.setBordered(false);
        slotBox.setValue(slot);
        slotBox.setResponder(value -> slot = value.trim().isBlank() ? slot : value.trim().toLowerCase(Locale.ROOT));
        addRenderableWidget(slotBox);

        conditionPathBox = new EditBox(font, EDITOR_X, FORM_Y + 96, 136, 20, Component.literal("Condition Path"));
        conditionPathBox.setHint(Component.literal("nbt path"));
        conditionPathBox.setBordered(false);
        addRenderableWidget(conditionPathBox);

        conditionValueBox = new EditBox(font, EDITOR_X + 246, FORM_Y + 96, 100, 20, Component.literal("Condition Value"));
        conditionValueBox.setHint(Component.literal("value"));
        conditionValueBox.setBordered(false);
        addRenderableWidget(conditionValueBox);

        durabilityBox = new EditBox(font, EDITOR_X, DURABILITY_Y, 88, 20, Component.literal("Durability"));
        durabilityBox.setHint(Component.literal("empty"));
        durabilityBox.setBordered(false);
        durabilityBox.setFilter(this::isIntegerInput);
        addRenderableWidget(durabilityBox);

        durabilityTriggersBox = new EditBox(font, EDITOR_X + 96, DURABILITY_Y, 210, 20, Component.literal("Durability Triggers"));
        durabilityTriggersBox.setHint(Component.literal("melee_hit,block_break,right_click"));
        durabilityTriggersBox.setBordered(false);
        addRenderableWidget(durabilityTriggersBox);

        miningSpeedBox = new EditBox(font, EDITOR_X, DURABILITY_Y + 44, 88, 20, Component.literal("Mining Speed"));
        miningSpeedBox.setHint(Component.literal("speed"));
        miningSpeedBox.setBordered(false);
        miningSpeedBox.setFilter(this::isNumericInput);
        addRenderableWidget(miningSpeedBox);

        qualityPathBox = new EditBox(font, EDITOR_X, DURABILITY_Y + 90, 128, 20, Component.literal("Quality Path"));
        qualityPathBox.setHint(Component.literal("quality"));
        qualityPathBox.setBordered(false);
        addRenderableWidget(qualityPathBox);

        qualityLevelsBox = new EditBox(font, EDITOR_X + 136, DURABILITY_Y + 90, 210, 20, Component.literal("Quality Levels"));
        qualityLevelsBox.setHint(Component.literal("common:60,rare:30,mythic:10"));
        qualityLevelsBox.setBordered(false);
        addRenderableWidget(qualityLevelsBox);

        qualityTriggersBox = new EditBox(font, EDITOR_X, DURABILITY_Y + 114, 346, 20, Component.literal("Quality Triggers"));
        qualityTriggersBox.setHint(Component.literal("craft,loot,villager_trade"));
        qualityTriggersBox.setBordered(false);
        addRenderableWidget(qualityTriggersBox);

        targetModeButton = addRenderableWidget(Button.builder(Component.literal("Target: item"), button -> toggleTargetMode())
                .bounds(EDITOR_X + 228, 52, 104, 20)
                .build());
        actionButton = addRenderableWidget(Button.builder(Component.literal("Action: modify"), button -> cycleAction())
                .bounds(EDITOR_X, FORM_Y + 24, 106, 20)
                .build());
        operationButton = addRenderableWidget(Button.builder(Component.literal("Op: addition"), button -> cycleOperation())
                .bounds(EDITOR_X + 114, FORM_Y + 24, 126, 20)
                .build());
        slotTypeButton = addRenderableWidget(Button.builder(Component.literal("Type: auto"), button -> cycleSlotType())
                .bounds(EDITOR_X, FORM_Y + 72, 106, 20)
                .build());
        slotButton = addRenderableWidget(Button.builder(Component.literal("Slot: mainhand"), button -> cycleSlot())
                .bounds(EDITOR_X + 114, FORM_Y + 72, 102, 20)
                .build());
        conditionOperatorButton = addRenderableWidget(Button.builder(Component.literal("Cond: equals"), button -> cycleConditionOperator())
                .bounds(EDITOR_X + 144, FORM_Y + 96, 94, 20)
                .build());
        useBaseButton = addRenderableWidget(Button.builder(Component.literal("Next Base"), button -> useNextBaseAttribute())
                .bounds(EDITOR_X + 224, FORM_Y + 48, 82, 20)
                .build());
        miningTierButton = addRenderableWidget(Button.builder(Component.literal("Tier: none"), button -> cycleMiningTier())
                .bounds(EDITOR_X + 96, DURABILITY_Y + 44, 110, 20)
                .build());
        decorativeButton = addRenderableWidget(Button.builder(Component.literal("Decorative: no"), button -> toggleDecorative())
                .bounds(EDITOR_X + 214, DURABILITY_Y + 44, 132, 20)
                .build());
        qualityButton = addRenderableWidget(Button.builder(Component.literal("Quality: off"), button -> toggleQuality())
                .bounds(EDITOR_X, DURABILITY_Y + 68, 110, 20)
                .build());

        saveButton = addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveCurrentRule())
                .bounds(width - 92, height - 32, 74, 20)
                .build());
        closeButton = addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(width - 174, height - 32, 74, 20)
                .build());

        EditorNetwork.INSTANCE.sendToServer(new C2SRequestEditorCatalogPacket());
        applyWidgetLayout();
        updateButtons();
    }

    @Override
    public void tick() {
        searchBox.tick();
        targetBox.tick();
        attributeBox.tick();
        amountBox.tick();
        slotBox.tick();
        conditionPathBox.tick();
        conditionValueBox.tick();
        durabilityBox.tick();
        durabilityTriggersBox.tick();
        miningSpeedBox.tick();
        qualityPathBox.tick();
        qualityLevelsBox.tick();
        qualityTriggersBox.tick();
        consumeNetworkState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyWidgetLayout();
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, BG);

        renderTitleBar(graphics);
        renderItemList(graphics, mouseX, mouseY);
        renderEditor(graphics);
        renderStatusBar(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickItemList(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && clickBaseAttributeList(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Rect2i listBounds = listBounds();
        if (listBounds.contains((int) mouseX, (int) mouseY)) {
            int listTop = listBounds.getY() + 28 + 1 + 22 + 1;
            int listBottom = listBounds.getY() + listBounds.getHeight() - 18;
            int visible = Math.max(1, (listBottom - listTop) / ROW_H);
            scroll = Math.max(0, Math.min(Math.max(0, filteredItems.size() - visible), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void consumeNetworkState() {
        String catalogJson = EditorClientState.latestCatalogJson();
        if (!catalogJson.equals(lastCatalogJson)) {
            lastCatalogJson = catalogJson;
            parseCatalog(catalogJson);
            refreshFilter();
            status = "Catalog loaded: " + allItems.size() + " items";
        }

        String ruleJson = EditorClientState.latestRuleJson();
        if (!ruleJson.equals(lastRuleJson)) {
            lastRuleJson = ruleJson;
            loadRulePayload(ruleJson);
        }

        String saveJson = EditorClientState.latestSaveResultJson();
        if (!saveJson.equals(lastSaveJson)) {
            lastSaveJson = saveJson;
            parseSaveResult(saveJson);
        }
    }

    private void parseCatalog(String json) {
        allItems.clear();
        attributes.clear();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray itemArray = root.getAsJsonArray("items");
            if (itemArray != null) {
                for (var element : itemArray) {
                    JsonObject item = element.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.tryParse(item.get("id").getAsString());
                    if (id == null) {
                        continue;
                    }
                    String descriptionId = item.get("descriptionId").getAsString();
                    String translated = Component.translatable(descriptionId).getString();
                    int maxDamage = item.get("maxDamage").getAsInt();
                    boolean damageable = item.get("damageable").getAsBoolean();
                    allItems.add(new CatalogItem(id, descriptionId, translated, maxDamage, damageable));
                }
            }

            JsonArray attributeArray = root.getAsJsonArray("attributes");
            if (attributeArray != null) {
                for (var element : attributeArray) {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsJsonObject().get("id").getAsString());
                    if (id != null) {
                        attributes.add(id);
                    }
                }
            }
        } catch (RuntimeException e) {
            status = "Catalog parse failed: " + e.getMessage();
        }
    }

    private void refreshFilter() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredItems.clear();
        for (CatalogItem item : allItems) {
            if (query.isBlank()
                    || item.id().toString().toLowerCase(Locale.ROOT).contains(query)
                    || item.id().getPath().toLowerCase(Locale.ROOT).contains(query)
                    || item.translatedName().toLowerCase(Locale.ROOT).contains(query)
                    || item.descriptionId().toLowerCase(Locale.ROOT).contains(query)) {
                filteredItems.add(item);
            }
        }
        filteredItems.sort(Comparator.comparing(CatalogItem::translatedName));
    }

    private void loadRulePayload(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            ResourceLocation payloadTarget = ResourceLocation.tryParse(root.get("targetId").getAsString());
            boolean payloadTagTarget = root.has("tagTarget") && root.get("tagTarget").getAsBoolean();
            if (selectedItem != null && !tagTarget && payloadTarget != null && !selectedItem.id().equals(payloadTarget)) {
                return;
            }

            resetAttributeForm();
            externalConflict = root.has("externalConflict") && root.get("externalConflict").getAsBoolean();
            parseBaseAttributes(root);
            currentRule = EditorJsonPayloads.ruleFromPayload(json).orElse(null);
            if (currentRule == null) {
                return;
            }
            tagTarget = payloadTagTarget;
            targetBox.setValue((tagTarget ? "#" : "") + currentRule.getTargetId());

            if (!currentRule.getAttributes().isEmpty()) {
                EditableAttributeModifier first = currentRule.getAttributes().get(0);
                loadAttributeIntoForm(first);
            } else if (!baseAttributes.isEmpty()) {
                selectedBaseAttributeIndex = 0;
                loadBaseAttributeIntoForm(baseAttributes.get(selectedBaseAttributeIndex));
            } else {
                resetAttributeForm();
            }

            if (currentRule.getDurability() != null && currentRule.getDurability().getDurability() != null) {
                durabilityBox.setValue(Integer.toString(currentRule.getDurability().getDurability()));
                durabilityTriggersBox.setValue(String.join(",", currentRule.getDurability().getTriggers()));
            } else {
                durabilityBox.setValue("");
                durabilityTriggersBox.setValue("");
            }
            loadMiningIntoForm(currentRule);
            loadQualityIntoForm(currentRule);
            decorative = currentRule.isDecorative();

            status = externalConflict ? "External rule detected for this item." : "Rule loaded.";
            updateButtons();
        } catch (RuntimeException e) {
            status = "Rule parse failed: " + e.getMessage();
        }
    }

    private void resetAttributeForm() {
        attributeBox.setValue("");
        amountBox.setValue("");
        conditionPathBox.setValue("");
        conditionValueBox.setValue("");
        action = EditableAttributeAction.MODIFY;
        operation = EditableOperationType.ADDITION;
        slotType = EditableSlotType.AUTO;
        slot = "mainhand";
        slotBox.setValue(slot);
        conditionOperator = "equals";
        selectedBaseAttributeIndex = -1;
        updateButtons();
    }

    private void parseBaseAttributes(JsonObject root) {
        baseAttributes.clear();
        JsonArray array = root.getAsJsonArray("baseAttributes");
        if (array == null) {
            return;
        }

        for (var element : array) {
            JsonObject object = element.getAsJsonObject();
            ResourceLocation attributeId = ResourceLocation.tryParse(object.get("attribute").getAsString());
            EditableOperationType operation = EditableOperationType.fromString(object.get("operation").getAsString());
            if (attributeId == null || operation == null) {
                continue;
            }
            baseAttributes.add(new BaseAttribute(
                    attributeId,
                    object.get("amount").getAsDouble(),
                    operation,
                    object.get("slot").getAsString()));
        }
    }

    private void loadAttributeIntoForm(EditableAttributeModifier attribute) {
        attributeBox.setValue(attribute.getAttributeId() == null ? "" : attribute.getAttributeId().toString());
        amountBox.setValue(attribute.getAmount() == null ? "" : Double.toString(attribute.getAmount()));
        action = attribute.getAction();
        operation = attribute.getOperation();
        slotType = attribute.getSlotType();
        slot = attribute.getSlot() == null || attribute.getSlot().isBlank() ? "mainhand" : attribute.getSlot();
        slotBox.setValue(slot);
        if (attribute.getCondition() != null) {
            conditionPathBox.setValue(attribute.getCondition().getPath() == null ? "" : attribute.getCondition().getPath());
            conditionOperator = attribute.getCondition().getOperator();
            conditionValueBox.setValue(attribute.getCondition().getValue() == null ? "" : attribute.getCondition().getValue().toString());
        } else {
            conditionPathBox.setValue("");
            conditionOperator = "equals";
            conditionValueBox.setValue("");
        }
    }

    private void loadBaseAttributeIntoForm(BaseAttribute attribute) {
        attributeBox.setValue(attribute.attributeId().toString());
        amountBox.setValue(Double.toString(attribute.amount()));
        action = EditableAttributeAction.MODIFY;
        operation = attribute.operation();
        slotType = EditableSlotType.STANDARD;
        slot = attribute.slot();
        slotBox.setValue(slot);
    }

    private void loadMiningIntoForm(EditableItemRule rule) {
        if (rule.getMiningOverrides().isEmpty()) {
            miningSpeedBox.setValue("");
            miningTier = "";
            return;
        }

        EditableMiningOverride miningOverride = rule.getMiningOverrides().get(0);
        miningSpeedBox.setValue(miningOverride.getSpeed() == null ? "" : Float.toString(miningOverride.getSpeed()));
        miningTier = miningOverride.getTier() == null ? "" : miningOverride.getTier();
    }

    private void loadQualityIntoForm(EditableItemRule rule) {
        EditableQualitySystem quality = rule.getQualitySystem();
        qualityEnabled = quality != null;
        if (quality == null) {
            qualityPathBox.setValue("");
            qualityLevelsBox.setValue("");
            qualityTriggersBox.setValue("");
            return;
        }

        qualityPathBox.setValue(quality.getTagPath());
        qualityTriggersBox.setValue(String.join(",", quality.getTriggers()));
        List<String> levels = new ArrayList<>();
        for (EditableQualityLevel level : quality.getLevels()) {
            String value = level.getValue() == null ? "" : stripJsonQuotes(level.getValue().toString());
            levels.add(value + ":" + level.getWeight());
        }
        qualityLevelsBox.setValue(String.join(",", levels));
    }

    private void parseSaveResult(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean success = root.has("success") && root.get("success").getAsBoolean();
            String message = root.has("message") ? root.get("message").getAsString() : "";
            status = (success ? "Saved. " : "Save failed. ") + message;
        } catch (RuntimeException e) {
            status = "Save result parse failed: " + e.getMessage();
        }
    }

    private void renderItemList(GuiGraphics graphics, int mouseX, int mouseY) {
        Rect2i bounds = listBounds();
        graphics.fill(bounds.getX(), bounds.getY(), bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(), PANEL);
        drawBorder(graphics, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), BORDER);

        int headerBottom = bounds.getY() + 28;
        int searchTop = headerBottom + 1;
        int searchBottom = searchTop + 22;
        int listTop = searchBottom + 1;
        int listBottom = bounds.getY() + bounds.getHeight() - 18;

        graphics.fill(bounds.getX(), bounds.getY(), bounds.getX() + bounds.getWidth(), headerBottom, HEADER_BG);
        graphics.fill(bounds.getX(), headerBottom, bounds.getX() + bounds.getWidth(), headerBottom + 1, BORDER_SOFT);
        graphics.drawString(font, "ITEM CATALOG", bounds.getX() + 8, centeredTextY(bounds.getY(), 28), TEXT_DIM, false);
        String total = Integer.toString(allItems.size());
        graphics.drawString(font, total, bounds.getX() + bounds.getWidth() - font.width(total) - 8,
                centeredTextY(bounds.getY(), 28), TEXT_MUTED, false);

        renderInputBg(graphics, bounds.getX() + 6, searchTop + 3, bounds.getWidth() - 12, 16, true);

        int y = listTop;
        int visible = Math.max(1, (listBottom - listTop) / ROW_H);
        scroll = Math.max(0, Math.min(Math.max(0, filteredItems.size() - visible), scroll));
        graphics.enableScissor(bounds.getX() + 1, listTop, bounds.getX() + bounds.getWidth() - 1, listBottom);
        for (int index = scroll; index < Math.min(filteredItems.size(), scroll + visible); index++) {
            CatalogItem item = filteredItems.get(index);
            boolean selected = selectedItem != null && selectedItem.id().equals(item.id());
            boolean hover = mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                    && mouseY >= y && mouseY < y + ROW_H;
            int rowColor = selected ? SEL_BG : (index % 2 == 0 ? PANEL_ALT : PANEL_DEEP);
            graphics.fill(bounds.getX() + 1, y, bounds.getX() + bounds.getWidth() - 1, y + ROW_H, rowColor);
            if (hover && !selected) {
                graphics.fill(bounds.getX() + 1, y, bounds.getX() + bounds.getWidth() - 1, y + ROW_H, HOVER_TINT);
            }
            if (selected) {
                graphics.fill(bounds.getX() + 1, y, bounds.getX() + 4, y + ROW_H, ACCENT);
            }

            Item registryItem = ForgeRegistries.ITEMS.getValue(item.id());
            if (registryItem != null) {
                graphics.renderItem(new ItemStack(registryItem), bounds.getX() + 8, y + 2);
            }
            int textW = bounds.getWidth() - 38;
            graphics.drawString(font, truncatePx(item.translatedName(), textW), bounds.getX() + 30, y + 3, selected ? ACCENT : TEXT, false);
            graphics.drawString(font, truncatePx(item.id().toString(), textW), bounds.getX() + 30, y + 13, TEXT_DIM, false);
            y += ROW_H;
        }
        graphics.disableScissor();

        if (filteredItems.size() > visible) {
            int trackTop = listTop + 2;
            int trackBottom = listBottom - 2;
            int trackH = Math.max(1, trackBottom - trackTop);
            int thumbH = Math.max(14, trackH * visible / Math.max(visible, filteredItems.size()));
            int maxScroll = Math.max(1, filteredItems.size() - visible);
            int thumbY = trackTop + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(bounds.getX() + bounds.getWidth() - 4, trackTop, bounds.getX() + bounds.getWidth() - 2, trackBottom, BORDER_SOFT);
            graphics.fill(bounds.getX() + bounds.getWidth() - 4, thumbY, bounds.getX() + bounds.getWidth() - 2, thumbY + thumbH, TEXT_DIM);
        }

        graphics.fill(bounds.getX(), listBottom, bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(), HEADER_BG);
        graphics.fill(bounds.getX(), listBottom - 1, bounds.getX() + bounds.getWidth(), listBottom, BORDER_SOFT);
        String count = filteredItems.size() + " / " + allItems.size();
        graphics.drawString(font, count, bounds.getX() + bounds.getWidth() - font.width(count) - 7, listBottom + 5, TEXT_DIM, false);
    }

    private void renderEditor(GuiGraphics graphics) {
        Rect2i panel = editorBounds();
        int x = panel.getX();
        int y = panel.getY();
        int w = panel.getWidth();
        int h = panel.getHeight();
        graphics.fill(x, y, x + w, y + h, PANEL);
        drawBorder(graphics, x, y, w, h, BORDER);
        graphics.fill(x, y, x + w, y + 28, HEADER_BG);
        graphics.fill(x, y + 28, x + w, y + 29, BORDER_SOFT);

        if (selectedItem == null) {
            graphics.drawString(font, "Select an item from the catalog", x + (w - font.width("Select an item from the catalog")) / 2,
                    y + h / 2 - font.lineHeight / 2, MUTED, false);
            return;
        }

        Item registryItem = ForgeRegistries.ITEMS.getValue(selectedItem.id());
        if (registryItem != null) {
            graphics.renderItem(new ItemStack(registryItem), x + 8, y + 6);
        }
        int titleX = x + 30;
        graphics.drawString(font, truncatePx(selectedItem.translatedName(), w - 180), titleX, y + 5, TEXT, false);
        graphics.drawString(font, truncatePx(selectedItem.id().toString(), w - 180), titleX, y + 15, TEXT_DIM, false);
        if (externalConflict) {
            String warn = "External conflict";
            graphics.drawString(font, warn, x + w - font.width(warn) - 8, centeredTextY(y, 28), WARN, false);
        }

        int contentX = x + 10;
        int contentY = y + 38;
        int contentW = w - 20;
        int rightW = Math.max(210, Math.min(310, contentW / 3));
        int leftW = Math.max(280, contentW - rightW - 8);
        int rightX = contentX + leftW + 8;

        renderSectionHeader(graphics, contentX, contentY, leftW, "TARGET");
        drawFieldLabel(graphics, "Item or tag id", targetBox);
        renderEditBoxFrame(graphics, targetBox, true);

        int attrY = contentY + 52;
        renderSectionHeader(graphics, contentX, attrY, leftW, "ATTRIBUTE CHANGE");
        drawFieldLabel(graphics, "Attribute", attributeBox);
        renderEditBoxFrame(graphics, attributeBox, true);
        drawFieldLabel(graphics, "Value", amountBox);
        renderEditBoxFrame(graphics, amountBox, action != EditableAttributeAction.REMOVE);
        drawFieldLabel(graphics, "Explicit slot", slotBox);
        renderEditBoxFrame(graphics, slotBox, slotType != EditableSlotType.AUTO);
        drawFieldLabel(graphics, "NBT/component path", conditionPathBox);
        renderEditBoxFrame(graphics, conditionPathBox, true);
        drawFieldLabel(graphics, "Condition value", conditionValueBox);
        renderEditBoxFrame(graphics, conditionValueBox, true);

        int systemY = contentY + 184;
        renderSectionHeader(graphics, contentX, systemY, leftW, "DURABILITY, MINING, QUALITY");
        drawFieldLabel(graphics, "Max durability", durabilityBox);
        renderEditBoxFrame(graphics, durabilityBox, true);
        drawFieldLabel(graphics, "Durability triggers", durabilityTriggersBox);
        renderEditBoxFrame(graphics, durabilityTriggersBox, true);
        drawFieldLabel(graphics, "Mining speed", miningSpeedBox);
        renderEditBoxFrame(graphics, miningSpeedBox, true);
        drawFieldLabel(graphics, "Quality tag", qualityPathBox);
        renderEditBoxFrame(graphics, qualityPathBox, qualityEnabled);
        drawFieldLabel(graphics, "Levels", qualityLevelsBox);
        renderEditBoxFrame(graphics, qualityLevelsBox, qualityEnabled);
        drawFieldLabel(graphics, "Quality triggers", qualityTriggersBox);
        renderEditBoxFrame(graphics, qualityTriggersBox, qualityEnabled);

        String vanillaDurability = selectedItem.damageable() ? Integer.toString(selectedItem.maxDamage()) : "not damageable";
        graphics.drawString(font, "Vanilla durability: " + vanillaDurability, contentX + Math.min(leftW - 150, 308),
                durabilityBox.getY() + 4, TEXT_MUTED, false);

        renderSideSummary(graphics, rightX, contentY, rightW, panel.getY() + panel.getHeight() - contentY - 8);
    }

    private void renderSideSummary(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_ALT);
        drawBorder(graphics, x, y, w, h, BORDER);

        int cy = y + 8;
        cy = renderSectionHeader(graphics, x + 6, cy, w - 12, "DETECTED ATTRIBUTES");
        if (baseAttributes.isEmpty()) {
            graphics.drawString(font, "No vanilla attributes found.", x + 10, cy + 3, TEXT_DIM, false);
            cy += 18;
        } else {
            for (int i = 0; i < Math.min(6, baseAttributes.size()); i++) {
                BaseAttribute attribute = baseAttributes.get(i);
                int rowY = cy + (i * 20);
                boolean selected = i == selectedBaseAttributeIndex;
                drawRow(graphics, x + 6, rowY, w - 12, selected, i);
                graphics.drawString(font, truncatePx(attributeDisplayName(attribute.attributeId()), w - 76),
                        x + 12, rowY + 3, selected ? ACCENT : TEXT, false);
                String effect = formatAmount(attribute.amount()) + " " + attribute.slot();
                graphics.drawString(font, truncatePx(effect, 70), x + w - font.width(truncatePx(effect, 70)) - 12,
                        rowY + 3, TEXT_DIM, false);
            }
            cy += Math.min(6, baseAttributes.size()) * 20;
            if (baseAttributes.size() > 6) {
                graphics.drawString(font, "+" + (baseAttributes.size() - 6) + " more", x + 10, cy + 3, TEXT_MUTED, false);
                cy += 16;
            }
        }

        cy += 8;
        cy = renderSectionHeader(graphics, x + 6, cy, w - 12, "SAVED RULE");
        int saved = currentRule == null ? 0 : currentRule.getAttributes().size();
        if (saved == 0) {
            graphics.drawString(font, "No attribute changes yet.", x + 10, cy + 3, TEXT_DIM, false);
            cy += 18;
        } else {
            for (int i = 0; i < Math.min(5, saved); i++) {
                EditableAttributeModifier attribute = currentRule.getAttributes().get(i);
                int rowY = cy + (i * 20);
                drawRow(graphics, x + 6, rowY, w - 12, false, i);
                String name = attribute.getAttributeId() == null ? "unknown" : attributeDisplayName(attribute.getAttributeId());
                graphics.drawString(font, truncatePx(name, w - 94), x + 12, rowY + 3, TEXT, false);
                String actionText = attribute.getAction().serializedName() + " " + slotText(attribute);
                graphics.drawString(font, truncatePx(actionText, 86), x + w - font.width(truncatePx(actionText, 86)) - 12,
                        rowY + 3, TEXT_DIM, false);
            }
            cy += Math.min(5, saved) * 20;
        }

        if (currentRule != null && currentRule.getDurability() != null && currentRule.getDurability().getDurability() != null) {
            graphics.drawString(font, "Durability: " + currentRule.getDurability().getDurability(), x + 10, cy + 8, ACCENT2, false);
            cy += 20;
        }
        if (currentRule != null && !currentRule.getMiningOverrides().isEmpty()) {
            graphics.drawString(font, "Mining override active", x + 10, cy + 8, ACCENT2, false);
            cy += 20;
        }
        if (qualityEnabled) {
            graphics.drawString(font, "Quality system active", x + 10, cy + 8, ACCENT2, false);
            cy += 20;
        }
        if (decorative) {
            graphics.drawString(font, "Decorative item", x + 10, cy + 8, ACCENT2, false);
        }
    }

    private void renderStatusBar(GuiGraphics graphics) {
        int y = height - FOOTER_H;
        graphics.fill(0, y, width, height, HEADER_BG);
        graphics.fill(0, y, width, y + 1, BORDER_SOFT);

        boolean error = status.startsWith("Save failed") || status.contains("failed") || status.contains("Invalid")
                || status.contains("must") || status.contains("needs");
        int color = error ? ERROR : (status.startsWith("Saved") || status.contains("loaded") ? ACCENT : TEXT_DIM);
        String pill = error ? "ERR" : (color == ACCENT ? "OK" : "...");
        int pillW = error ? 34 : (color == ACCENT ? 30 : 22);
        graphics.fill(8, y + 4, 8 + pillW, y + 16, INPUT_BG);
        drawBorder(graphics, 8, y + 4, pillW, 12, error ? ERROR : (color == ACCENT ? ACCENT2 : BORDER_SOFT));
        graphics.drawString(font, pill, 13, y + 6, color, false);
        graphics.drawString(font, truncatePx(status, Math.max(40, width - 180)),
                8 + pillW + 7, centeredTextY(y, FOOTER_H), color, false);
    }

    private boolean clickItemList(double mouseX, double mouseY) {
        Rect2i bounds = listBounds();
        if (!bounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }

        int listTop = bounds.getY() + 28 + 1 + 22 + 1;
        int row = ((int) mouseY - listTop) / ROW_H;
        int index = scroll + row;
        if (row >= 0 && index >= 0 && index < filteredItems.size()) {
            selectedItem = filteredItems.get(index);
            tagTarget = false;
            targetBox.setValue(selectedItem.id().toString());
            currentRule = new EditableItemRule(selectedItem.id(), false);
            baseAttributes.clear();
            resetAttributeForm();
            status = "Loading rule...";
            lastRuleJson = "";
            EditorNetwork.INSTANCE.sendToServer(new C2SRequestItemRulePacket(selectedItem.id(), false));
            updateButtons();
            return true;
        }
        return false;
    }

    private void saveCurrentRule() {
        TargetSelection target = readTargetSelection();
        if (target == null) {
            status = "Select or type a valid item/tag target.";
            return;
        }

        String rawAttribute = attributeBox.getValue().trim();
        EditableItemRule rule = currentRule != null
                && target.id().equals(currentRule.getTargetId())
                && target.tagTarget() == currentRule.isTagTarget()
                ? currentRule.copy()
                : new EditableItemRule(target.id(), target.tagTarget());

        if (!rawAttribute.isBlank()) {
            ResourceLocation attributeId = ResourceLocation.tryParse(rawAttribute);
            if (attributeId == null) {
                status = "Invalid attribute id.";
                return;
            }

            Double amount = null;
            if (action != EditableAttributeAction.REMOVE) {
                try {
                    amount = Double.parseDouble(amountBox.getValue().trim());
                } catch (NumberFormatException e) {
                    status = "Amount must be numeric.";
                    return;
                }
            }

            EditableAttributeModifier editedAttribute = new EditableAttributeModifier(attributeId, action, amount,
                    operation, slotType, effectiveSlot());
            EditableCondition condition = readConditionFromForm();
            if (condition != null) {
                editedAttribute.setCondition(condition);
            }
            rule.getAttributes().removeIf(existing -> editedAttribute.duplicateKey().equalsIgnoreCase(existing.duplicateKey()));
            rule.getAttributes().add(editedAttribute);
        }

        if (!durabilityBox.getValue().isBlank()) {
            try {
                EditableDurabilityModifier durability = new EditableDurabilityModifier(Integer.parseInt(durabilityBox.getValue().trim()));
                durability.getTriggers().addAll(parseCsv(durabilityTriggersBox.getValue()));
                rule.setDurability(durability);
            } catch (NumberFormatException e) {
                status = "Durability must be an integer.";
                return;
            }
        } else {
            rule.setDurability(null);
        }

        rule.getMiningOverrides().clear();
        if (!miningSpeedBox.getValue().isBlank() || !miningTier.isBlank()) {
            Float speed = null;
            if (!miningSpeedBox.getValue().isBlank()) {
                try {
                    speed = Float.parseFloat(miningSpeedBox.getValue().trim());
                } catch (NumberFormatException e) {
                    status = "Mining speed must be numeric.";
                    return;
                }
            }
            EditableMiningOverride miningOverride = new EditableMiningOverride(speed, miningTier.isBlank() ? null : miningTier);
            miningOverride.setCondition(readConditionFromForm());
            rule.getMiningOverrides().add(miningOverride);
        }

        rule.setQualitySystem(qualityEnabled ? buildQualityFromForm() : null);
        if (qualityEnabled && rule.getQualitySystem() == null) {
            return;
        }
        rule.setDecorative(decorative);

        currentRule = rule.copy();
        status = "Saving...";
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
    }

    private Rect2i listBounds() {
        int listW = Math.max(LIST_MIN_W, Math.min(LIST_MAX_W, (int) Math.round(width * 0.235D)));
        int y = TOP_BAR_H + 6;
        return new Rect2i(OUTER_MARGIN, y, listW, Math.max(180, height - y - FOOTER_H - 6));
    }

    private Rect2i editorBounds() {
        Rect2i list = listBounds();
        int x = list.getX() + list.getWidth() + PANEL_GAP;
        int y = list.getY();
        return new Rect2i(x, y, Math.max(260, width - OUTER_MARGIN - x), list.getHeight());
    }

    private void applyWidgetLayout() {
        Rect2i list = listBounds();
        Rect2i editor = editorBounds();
        int contentX = editor.getX() + 10;
        int contentY = editor.getY() + 38;
        int contentW = editor.getWidth() - 20;
        int rightW = Math.max(210, Math.min(310, contentW / 3));
        int leftW = Math.max(280, contentW - rightW - 8);
        int fieldW = Math.max(140, leftW - 126);

        setEditBoxBounds(searchBox, list.getX() + 11, list.getY() + 36, list.getWidth() - 22, CONTROL_H);
        setEditBoxBounds(targetBox, contentX, contentY + 20, fieldW, CONTROL_H);
        setButtonBounds(targetModeButton, contentX + fieldW + 8, contentY + 20, 104, BUTTON_H);

        int attrY = contentY + 72;
        setEditBoxBounds(attributeBox, contentX, attrY + 20, Math.min(360, leftW), CONTROL_H);
        setButtonBounds(actionButton, contentX, attrY + 46, 102, BUTTON_H);
        setButtonBounds(operationButton, contentX + 110, attrY + 46, 128, BUTTON_H);
        setEditBoxBounds(amountBox, contentX + 246, attrY + 46, 82, CONTROL_H);
        setButtonBounds(useBaseButton, contentX + 336, attrY + 46, 78, BUTTON_H);
        setButtonBounds(slotTypeButton, contentX, attrY + 72, 102, BUTTON_H);
        setButtonBounds(slotButton, contentX + 110, attrY + 72, 104, BUTTON_H);
        setEditBoxBounds(slotBox, contentX + 222, attrY + 72, 96, CONTROL_H);
        setEditBoxBounds(conditionPathBox, contentX, attrY + 98, Math.min(210, leftW / 2), CONTROL_H);
        setButtonBounds(conditionOperatorButton, contentX + Math.min(218, leftW / 2 + 8), attrY + 98, 104, BUTTON_H);
        setEditBoxBounds(conditionValueBox, contentX + Math.min(330, leftW - 110), attrY + 98, Math.max(96, leftW - Math.min(330, leftW - 110)), CONTROL_H);

        int systemY = contentY + 204;
        setEditBoxBounds(durabilityBox, contentX, systemY + 20, 88, CONTROL_H);
        setEditBoxBounds(durabilityTriggersBox, contentX + 96, systemY + 20, Math.min(230, leftW - 96), CONTROL_H);
        setEditBoxBounds(miningSpeedBox, contentX, systemY + 46, 88, CONTROL_H);
        setButtonBounds(miningTierButton, contentX + 96, systemY + 46, 108, BUTTON_H);
        setButtonBounds(decorativeButton, contentX + 212, systemY + 46, 132, BUTTON_H);
        setButtonBounds(qualityButton, contentX, systemY + 72, 108, BUTTON_H);
        setEditBoxBounds(qualityPathBox, contentX, systemY + 98, 128, CONTROL_H);
        setEditBoxBounds(qualityLevelsBox, contentX + 136, systemY + 98, Math.max(154, leftW - 136), CONTROL_H);
        setEditBoxBounds(qualityTriggersBox, contentX, systemY + 124, Math.min(360, leftW), CONTROL_H);

        setButtonBounds(saveButton, width - OUTER_MARGIN - 64, height - FOOTER_H + 3, 64, BUTTON_H);
        setButtonBounds(closeButton, width - OUTER_MARGIN - 132, height - FOOTER_H + 3, 64, BUTTON_H);
    }

    private void setEditBoxBounds(EditBox box, int x, int y, int w, int h) {
        if (box == null) {
            return;
        }
        box.setX(x);
        box.setY(y);
        box.setWidth(Math.max(20, w));
        box.setHeight(h);
    }

    private void setButtonBounds(Button button, int x, int y, int w, int h) {
        if (button == null) {
            return;
        }
        button.setX(x);
        button.setY(y);
        button.setWidth(Math.max(20, w));
        button.setHeight(h);
    }

    private void renderTitleBar(GuiGraphics graphics) {
        graphics.fill(0, 0, width, TOP_BAR_H, HEADER_BG);
        graphics.fill(0, 0, width, 1, BORDER_SOFT);
        graphics.fill(0, TOP_BAR_H - 1, width, TOP_BAR_H, BORDER_SOFT);
        graphics.fill(0, 0, 3, TOP_BAR_H, ACCENT);

        String titleText = "AttributeModify Editor";
        int titleY = centeredTextY(0, TOP_BAR_H);
        graphics.drawString(font, titleText, 9, titleY, ACCENT, false);

        int x = 9 + font.width(titleText);
        if (selectedItem != null) {
            String crumb = "  >  " + selectedItem.translatedName();
            graphics.drawString(font, truncatePx(crumb, Math.max(40, width - x - 150)), x, titleY, TEXT_DIM, false);
        }

        String meta = allItems.size() + " items  |  " + attributes.size() + " attrs";
        int metaX = width - font.width(meta) - 10;
        if (metaX > x + 16) {
            graphics.drawString(font, meta, metaX, titleY, TEXT_MUTED, false);
        }
    }

    private int renderSectionHeader(GuiGraphics graphics, int x, int y, int w, String label) {
        graphics.fill(x, y, x + w, y + 1, BORDER_SOFT);
        graphics.fill(x, y + 2, x + 3, y + 13, ACCENT);
        graphics.drawString(font, label, x + 8, y + 3, TEXT_DIM, false);
        return y + 16;
    }

    private void drawFieldLabel(GuiGraphics graphics, String label, EditBox box) {
        if (box == null || !box.visible) {
            return;
        }
        graphics.drawString(font, label, box.getX() + 4, box.getY() - 12, TEXT_DIM, false);
    }

    private void renderEditBoxFrame(GuiGraphics graphics, EditBox box, boolean enabled) {
        if (box == null || !box.visible) {
            return;
        }
        renderInputBg(graphics, box.getX() - 1, box.getY() - 1, box.getWidth() + 2, box.getHeight() + 2, enabled);
    }

    private void renderInputBg(GuiGraphics graphics, int x, int y, int w, int h, boolean enabled) {
        graphics.fill(x, y, x + w, y + h, enabled ? INPUT_BG : PANEL_DEEP);
        drawBorder(graphics, x, y, w, h, enabled ? BORDER : BORDER_SOFT);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawRow(GuiGraphics graphics, int x, int y, int w, boolean selected, int index) {
        graphics.fill(x, y, x + w, y + 20, selected ? SEL_BG : (index % 2 == 0 ? PANEL_DEEP : PANEL));
        if (selected) {
            graphics.fill(x, y, x + 3, y + 20, ACCENT);
        }
    }

    private int centeredTextY(int y, int h) {
        return y + Math.max(0, (h - font.lineHeight) / 2);
    }

    private String truncatePx(String value, int maxPx) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (font.width(value) <= maxPx) {
            return value;
        }
        String ellipsis = "...";
        int max = Math.max(0, maxPx - font.width(ellipsis));
        String result = value;
        while (!result.isEmpty() && font.width(result) > max) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    private String attributeDisplayName(ResourceLocation id) {
        if (id == null) {
            return "Unknown";
        }
        String path = id.getPath()
                .replace("generic.", "")
                .replace("player.", "")
                .replace('.', ' ')
                .replace('_', ' ')
                .trim();
        if (path.isEmpty()) {
            return id.toString();
        }
        StringBuilder out = new StringBuilder(path.length());
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(' ');
                upper = true;
            } else if (upper) {
                out.append(Character.toUpperCase(c));
                upper = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private String formatAmount(double amount) {
        if (amount == (long) amount) {
            return Long.toString((long) amount);
        }
        return String.format(Locale.ROOT, "%.3f", amount).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String slotText(EditableAttributeModifier attribute) {
        if (attribute.getSlotType() == EditableSlotType.AUTO) {
            return "auto";
        }
        if (attribute.getSlot() == null || attribute.getSlot().isBlank()) {
            return attribute.getSlotType().name().toLowerCase(Locale.ROOT);
        }
        return attribute.getSlot();
    }

    private void cycleAction() {
        action = switch (action) {
            case ADD -> EditableAttributeAction.MODIFY;
            case MODIFY -> EditableAttributeAction.REMOVE;
            case REMOVE -> EditableAttributeAction.ADD;
        };
        updateButtons();
    }

    private void cycleOperation() {
        operation = switch (operation) {
            case ADDITION -> EditableOperationType.MULTIPLY_BASE;
            case MULTIPLY_BASE -> EditableOperationType.MULTIPLY_TOTAL;
            case MULTIPLY_TOTAL -> EditableOperationType.ADDITION;
        };
        updateButtons();
    }

    private void cycleSlot() {
        int index = 0;
        for (int i = 0; i < STANDARD_SLOTS.length; i++) {
            if (STANDARD_SLOTS[i].equals(slot)) {
                index = i;
                break;
            }
        }
        slot = STANDARD_SLOTS[(index + 1) % STANDARD_SLOTS.length];
        slotBox.setValue(slot);
        updateButtons();
    }

    private void toggleTargetMode() {
        tagTarget = !tagTarget;
        String value = targetBox.getValue().trim();
        if (tagTarget && !value.startsWith("#")) {
            targetBox.setValue("#" + value);
        } else if (!tagTarget && value.startsWith("#")) {
            targetBox.setValue(value.substring(1));
        }
        updateButtons();
    }

    private void cycleSlotType() {
        slotType = switch (slotType) {
            case STANDARD -> EditableSlotType.CURIOS;
            case CURIOS -> EditableSlotType.AUTO;
            case AUTO -> EditableSlotType.STANDARD;
        };
        updateButtons();
    }

    private void cycleConditionOperator() {
        String[] operators = {"equals", "not_equals", "greater", "less", "exists", "not_exists", "contains"};
        int index = 0;
        for (int i = 0; i < operators.length; i++) {
            if (operators[i].equals(conditionOperator)) {
                index = i;
                break;
            }
        }
        conditionOperator = operators[(index + 1) % operators.length];
        updateButtons();
    }

    private void cycleMiningTier() {
        String[] tiers = {"", "wood", "stone", "iron", "diamond", "netherite", "gold"};
        int index = 0;
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i].equals(miningTier)) {
                index = i;
                break;
            }
        }
        miningTier = tiers[(index + 1) % tiers.length];
        updateButtons();
    }

    private void toggleDecorative() {
        decorative = !decorative;
        updateButtons();
    }

    private void toggleQuality() {
        qualityEnabled = !qualityEnabled;
        if (qualityEnabled && qualityPathBox.getValue().isBlank()) {
            qualityPathBox.setValue("quality");
        }
        if (qualityEnabled && qualityTriggersBox.getValue().isBlank()) {
            qualityTriggersBox.setValue("craft");
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean hasTarget = selectedItem != null || (targetBox != null && !targetBox.getValue().trim().isBlank());
        if (targetModeButton != null) {
            targetModeButton.setMessage(Component.literal("Target: " + (tagTarget ? "tag" : "item")));
        }
        if (actionButton != null) {
            actionButton.setMessage(Component.literal("Action: " + action.serializedName()));
        }
        if (operationButton != null) {
            operationButton.setMessage(Component.literal("Op: " + operation.serializedName()));
            operationButton.active = hasTarget && action != EditableAttributeAction.REMOVE;
            operationButton.visible = hasTarget;
        }
        if (slotTypeButton != null) {
            slotTypeButton.setMessage(Component.literal("Type: " + slotType.name().toLowerCase(Locale.ROOT)));
            slotTypeButton.active = hasTarget;
            slotTypeButton.visible = hasTarget;
        }
        if (slotButton != null) {
            slotButton.setMessage(Component.literal("Slot: " + slot));
            slotButton.active = hasTarget && slotType == EditableSlotType.STANDARD;
            slotButton.visible = hasTarget;
        }
        if (conditionOperatorButton != null) {
            conditionOperatorButton.setMessage(Component.literal("Cond: " + conditionOperator));
            conditionOperatorButton.active = hasTarget;
            conditionOperatorButton.visible = hasTarget;
        }
        if (miningTierButton != null) {
            miningTierButton.setMessage(Component.literal("Tier: " + (miningTier.isBlank() ? "none" : miningTier)));
            miningTierButton.active = hasTarget;
            miningTierButton.visible = hasTarget;
        }
        if (decorativeButton != null) {
            decorativeButton.setMessage(Component.literal("Decorative: " + (decorative ? "yes" : "no")));
            decorativeButton.active = hasTarget;
            decorativeButton.visible = hasTarget;
        }
        if (qualityButton != null) {
            qualityButton.setMessage(Component.literal("Quality: " + (qualityEnabled ? "on" : "off")));
            qualityButton.active = hasTarget;
            qualityButton.visible = hasTarget;
        }
        if (useBaseButton != null) {
            useBaseButton.active = selectedItem != null && !baseAttributes.isEmpty();
            useBaseButton.visible = selectedItem != null;
        }
        if (saveButton != null) {
            saveButton.active = hasTarget;
        }
        if (amountBox != null) {
            amountBox.active = hasTarget && action != EditableAttributeAction.REMOVE;
            amountBox.visible = hasTarget;
        }
        if (attributeBox != null) {
            attributeBox.active = hasTarget;
            attributeBox.visible = hasTarget;
        }
        if (durabilityBox != null) {
            durabilityBox.active = hasTarget;
            durabilityBox.visible = hasTarget;
        }
        if (actionButton != null) {
            actionButton.active = hasTarget;
            actionButton.visible = hasTarget;
        }
        setVisibleActive(targetBox, true, true);
        setVisibleActive(slotBox, hasTarget && slotType != EditableSlotType.AUTO, hasTarget);
        setVisibleActive(conditionPathBox, hasTarget, hasTarget);
        setVisibleActive(conditionValueBox, hasTarget, hasTarget);
        setVisibleActive(durabilityTriggersBox, hasTarget, hasTarget);
        setVisibleActive(miningSpeedBox, hasTarget, hasTarget);
        setVisibleActive(qualityPathBox, hasTarget && qualityEnabled, hasTarget && qualityEnabled);
        setVisibleActive(qualityLevelsBox, hasTarget && qualityEnabled, hasTarget && qualityEnabled);
        setVisibleActive(qualityTriggersBox, hasTarget && qualityEnabled, hasTarget && qualityEnabled);
    }

    private boolean isNumericInput(String value) {
        return value.isBlank() || value.matches("-?\\d*(\\.\\d*)?");
    }

    private boolean isIntegerInput(String value) {
        return value.isBlank() || value.matches("\\d*");
    }

    private void useNextBaseAttribute() {
        if (baseAttributes.isEmpty()) {
            status = "This item has no base attributes.";
            return;
        }
        selectedBaseAttributeIndex = Math.floorMod(selectedBaseAttributeIndex + 1, baseAttributes.size());
        loadBaseAttributeIntoForm(baseAttributes.get(selectedBaseAttributeIndex));
        status = "Selected base attribute " + (selectedBaseAttributeIndex + 1) + "/" + baseAttributes.size();
        updateButtons();
    }

    private boolean clickBaseAttributeList(double mouseX, double mouseY) {
        if (selectedItem == null || baseAttributes.isEmpty()) {
            return false;
        }

        Rect2i editor = editorBounds();
        int contentX = editor.getX() + 10;
        int contentY = editor.getY() + 38;
        int contentW = editor.getWidth() - 20;
        int rightW = Math.max(210, Math.min(310, contentW / 3));
        int leftW = Math.max(280, contentW - rightW - 8);
        int rightX = contentX + leftW + 8;
        int rowsTop = contentY + 8 + 16;
        int row = ((int) mouseY - rowsTop) / 20;
        if (mouseX < rightX + 6 || mouseX > rightX + rightW - 6 || row < 0 || row >= Math.min(6, baseAttributes.size())) {
            return false;
        }

        selectedBaseAttributeIndex = row;
        loadBaseAttributeIntoForm(baseAttributes.get(row));
        status = "Selected base attribute " + (row + 1) + "/" + baseAttributes.size();
        updateButtons();
        return true;
    }

    private void setVisibleActive(EditBox box, boolean active, boolean visible) {
        if (box == null) {
            return;
        }
        box.active = active;
        box.visible = visible;
    }

    private TargetSelection readTargetSelection() {
        String raw = targetBox == null ? "" : targetBox.getValue().trim();
        if (raw.isBlank() && selectedItem != null) {
            raw = selectedItem.id().toString();
        }
        boolean isTag = tagTarget || raw.startsWith("#");
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null ? null : new TargetSelection(id, isTag);
    }

    private String effectiveSlot() {
        if (slotType == EditableSlotType.AUTO) {
            return null;
        }
        String typed = slotBox == null ? "" : slotBox.getValue().trim();
        if (!typed.isBlank()) {
            return typed.toLowerCase(Locale.ROOT);
        }
        return slot == null || slot.isBlank() ? "mainhand" : slot;
    }

    private EditableCondition readConditionFromForm() {
        String path = conditionPathBox == null ? "" : conditionPathBox.getValue().trim();
        if (path.isBlank()) {
            return null;
        }
        String rawValue = conditionValueBox == null ? "" : conditionValueBox.getValue().trim();
        JsonElement value = rawValue.isBlank() ? null : parseJsonValue(rawValue);
        return new EditableCondition(path, conditionOperator, value);
    }

    private JsonElement parseJsonValue(String rawValue) {
        try {
            return JsonParser.parseString(rawValue);
        } catch (RuntimeException ignored) {
            return JsonParser.parseString("\"" + rawValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        }
    }

    private EditableQualitySystem buildQualityFromForm() {
        String levelsText = qualityLevelsBox.getValue().trim();
        if (levelsText.isBlank()) {
            status = "Quality needs levels like common:60,rare:30.";
            return null;
        }

        EditableQualitySystem quality = new EditableQualitySystem();
        quality.setTagPath(qualityPathBox.getValue().trim());
        quality.getTriggers().addAll(parseCsv(qualityTriggersBox.getValue()));

        for (String token : parseCsv(levelsText)) {
            int split = token.lastIndexOf(':');
            if (split <= 0 || split >= token.length() - 1) {
                status = "Quality level must use value:weight.";
                return null;
            }
            String value = token.substring(0, split).trim();
            String weightText = token.substring(split + 1).trim();
            try {
                quality.getLevels().add(new EditableQualityLevel(parseJsonValue(value), Integer.parseInt(weightText)));
            } catch (NumberFormatException e) {
                status = "Quality weight must be an integer.";
                return null;
            }
        }

        return quality;
    }

    private List<String> parseCsv(String value) {
        List<String> values = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return values;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private String stripJsonQuotes(String value) {
        if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CatalogItem(ResourceLocation id, String descriptionId, String translatedName, int maxDamage, boolean damageable) {
    }

    private record BaseAttribute(ResourceLocation attributeId, double amount, EditableOperationType operation, String slot) {
    }

    private record TargetSelection(ResourceLocation id, boolean tagTarget) {
    }
}
