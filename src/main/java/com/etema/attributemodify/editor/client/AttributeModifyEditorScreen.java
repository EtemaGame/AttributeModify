package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.editor.EditorClientState;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableDurabilityModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import com.etema.attributemodify.editor.network.C2SRequestEditorCatalogPacket;
import com.etema.attributemodify.editor.network.C2SRequestItemRulePacket;
import com.etema.attributemodify.editor.network.C2SSaveItemRulePacket;
import com.etema.attributemodify.editor.network.EditorNetwork;
import com.google.gson.JsonArray;
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
    private static final int PANEL = 0xCC101418;
    private static final int PANEL_ALT = 0xCC1A2128;
    private static final int TEXT = 0xFFE8EEF5;
    private static final int MUTED = 0xFF9EACB8;
    private static final int ACCENT = 0xFF69C6A4;
    private static final int ERROR = 0xFFFF8A80;
    private static final String[] STANDARD_SLOTS = {"mainhand", "offhand", "head", "chest", "legs", "feet"};
    private static final int EDITOR_X = 260;
    private static final int FORM_Y = 104;
    private static final int DURABILITY_Y = 326;

    private final List<CatalogItem> allItems = new ArrayList<>();
    private final List<CatalogItem> filteredItems = new ArrayList<>();
    private final List<ResourceLocation> attributes = new ArrayList<>();
    private final List<BaseAttribute> baseAttributes = new ArrayList<>();

    private EditBox searchBox;
    private EditBox attributeBox;
    private EditBox amountBox;
    private EditBox durabilityBox;
    private Button actionButton;
    private Button operationButton;
    private Button slotButton;
    private Button useBaseButton;
    private Button saveButton;

    private CatalogItem selectedItem;
    private EditableItemRule currentRule;
    private EditableAttributeAction action = EditableAttributeAction.MODIFY;
    private EditableOperationType operation = EditableOperationType.ADDITION;
    private String slot = "mainhand";
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
        searchBox.setResponder(value -> {
            scroll = 0;
            refreshFilter();
        });
        addRenderableWidget(searchBox);

        attributeBox = new EditBox(font, EDITOR_X, FORM_Y, 220, 20, Component.literal("Attribute"));
        attributeBox.setHint(Component.literal("attribute id"));
        addRenderableWidget(attributeBox);

        amountBox = new EditBox(font, EDITOR_X, FORM_Y + 48, 88, 20, Component.literal("Amount"));
        amountBox.setHint(Component.literal("amount"));
        amountBox.setFilter(this::isNumericInput);
        addRenderableWidget(amountBox);

        durabilityBox = new EditBox(font, EDITOR_X, DURABILITY_Y, 88, 20, Component.literal("Durability"));
        durabilityBox.setHint(Component.literal("empty"));
        durabilityBox.setFilter(this::isIntegerInput);
        addRenderableWidget(durabilityBox);

        actionButton = addRenderableWidget(Button.builder(Component.literal("Action: modify"), button -> cycleAction())
                .bounds(EDITOR_X, FORM_Y + 24, 106, 20)
                .build());
        operationButton = addRenderableWidget(Button.builder(Component.literal("Op: addition"), button -> cycleOperation())
                .bounds(EDITOR_X + 114, FORM_Y + 24, 126, 20)
                .build());
        slotButton = addRenderableWidget(Button.builder(Component.literal("Slot: mainhand"), button -> cycleSlot())
                .bounds(EDITOR_X + 96, FORM_Y + 48, 120, 20)
                .build());
        useBaseButton = addRenderableWidget(Button.builder(Component.literal("Next Base"), button -> useNextBaseAttribute())
                .bounds(EDITOR_X + 224, FORM_Y + 48, 82, 20)
                .build());

        saveButton = addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveCurrentRule())
                .bounds(width - 92, height - 32, 74, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(width - 174, height - 32, 74, 20)
                .build());

        EditorNetwork.INSTANCE.sendToServer(new C2SRequestEditorCatalogPacket());
        updateButtons();
    }

    @Override
    public void tick() {
        searchBox.tick();
        attributeBox.tick();
        amountBox.tick();
        durabilityBox.tick();
        consumeNetworkState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xFF0E1114);
        graphics.drawString(font, title, 18, 12, TEXT, false);

        renderItemList(graphics, mouseX, mouseY);
        renderEditor(graphics);
        renderStatus(graphics);

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
            int visible = Math.max(1, listBounds.getHeight() / 22);
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
            if (selectedItem != null && payloadTarget != null && !selectedItem.id().equals(payloadTarget)) {
                return;
            }

            resetAttributeForm();
            externalConflict = root.has("externalConflict") && root.get("externalConflict").getAsBoolean();
            parseBaseAttributes(root);
            currentRule = EditorJsonPayloads.ruleFromPayload(json).orElse(null);
            if (currentRule == null) {
                return;
            }

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
            } else {
                durabilityBox.setValue("");
            }

            status = externalConflict ? "External rule detected for this item." : "Rule loaded.";
            updateButtons();
        } catch (RuntimeException e) {
            status = "Rule parse failed: " + e.getMessage();
        }
    }

    private void resetAttributeForm() {
        attributeBox.setValue("");
        amountBox.setValue("");
        action = EditableAttributeAction.MODIFY;
        operation = EditableOperationType.ADDITION;
        slot = "mainhand";
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
        slot = attribute.getSlot() == null || attribute.getSlot().isBlank() ? "mainhand" : attribute.getSlot();
    }

    private void loadBaseAttributeIntoForm(BaseAttribute attribute) {
        attributeBox.setValue(attribute.attributeId().toString());
        amountBox.setValue(Double.toString(attribute.amount()));
        action = EditableAttributeAction.MODIFY;
        operation = attribute.operation();
        slot = attribute.slot();
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
        graphics.drawString(font, "Items (" + filteredItems.size() + ")", bounds.getX() + 8, bounds.getY() + 6, TEXT, false);

        int y = bounds.getY() + 24;
        int visible = Math.max(1, (bounds.getHeight() - 26) / 22);
        for (int index = scroll; index < Math.min(filteredItems.size(), scroll + visible); index++) {
            CatalogItem item = filteredItems.get(index);
            boolean selected = selectedItem != null && selectedItem.id().equals(item.id());
            int rowColor = selected ? 0xFF24483D : (index % 2 == 0 ? PANEL_ALT : 0xAA141A20);
            graphics.fill(bounds.getX() + 4, y, bounds.getX() + bounds.getWidth() - 4, y + 20, rowColor);

            Item registryItem = ForgeRegistries.ITEMS.getValue(item.id());
            if (registryItem != null) {
                graphics.renderItem(new ItemStack(registryItem), bounds.getX() + 8, y + 2);
            }
            graphics.drawString(font, truncate(item.translatedName(), 25), bounds.getX() + 30, y + 3, TEXT, false);
            graphics.drawString(font, truncate(item.id().toString(), 32), bounds.getX() + 30, y + 12, MUTED, false);
            y += 22;
        }
    }

    private void renderEditor(GuiGraphics graphics) {
        int x = 250;
        int y = 28;
        int w = width - x - 18;
        graphics.fill(x, y, x + w, height - 42, PANEL);

        if (selectedItem == null) {
            graphics.drawString(font, "Select an item to edit.", x + 10, y + 12, MUTED, false);
            return;
        }

        graphics.drawString(font, selectedItem.translatedName(), x + 10, y + 10, TEXT, false);
        graphics.drawString(font, selectedItem.id().toString(), x + 10, y + 22, MUTED, false);
        graphics.drawString(font, "Attribute", x + 10, y + 62, MUTED, false);
        graphics.drawString(font, "Amount", x + 10, y + 110, MUTED, false);
        if (baseAttributes.isEmpty() && (currentRule == null || currentRule.getAttributes().isEmpty())) {
            graphics.drawString(font, "No real attributes found for this item.", x + 10, y + 154, ERROR, false);
        }

        int baseY = y + 178;
        graphics.drawString(font, "Base item attributes (" + baseAttributes.size() + ")", x + 10, baseY, MUTED, false);
        if (baseAttributes.isEmpty()) {
            graphics.drawString(font, "This item has no vanilla attribute modifiers.", x + 10, baseY + 14, MUTED, false);
        } else {
            for (int i = 0; i < Math.min(6, baseAttributes.size()); i++) {
                BaseAttribute attribute = baseAttributes.get(i);
                int lineY = baseY + 14 + (i * 14);
                if (i == selectedBaseAttributeIndex) {
                    graphics.fill(x + 8, lineY - 2, x + Math.min(w - 8, 590), lineY + 11, 0xFF24483D);
                }
                String line = attribute.attributeId() + "  " + attribute.amount() + "  " + attribute.slot();
                graphics.drawString(font, truncate(line, 72), x + 10, lineY, TEXT, false);
            }
        }

        graphics.drawString(font, "Durability", x + 10, DURABILITY_Y - 16, MUTED, false);

        String vanillaDurability = selectedItem.damageable() ? Integer.toString(selectedItem.maxDamage()) : "not damageable";
        graphics.drawString(font, "Current: " + vanillaDurability, x + 104, DURABILITY_Y + 5, MUTED, false);

        graphics.drawString(font, "Saved in this editor rule", x + 10, y + 348, MUTED, false);
        int rowY = y + 362;
        if (currentRule == null || currentRule.getAttributes().isEmpty()) {
            graphics.drawString(font, "No attributes yet.", x + 10, rowY, MUTED, false);
        } else {
            for (int i = 0; i < Math.min(5, currentRule.getAttributes().size()); i++) {
                EditableAttributeModifier attribute = currentRule.getAttributes().get(i);
                String line = attribute.getAction().serializedName() + " "
                        + (attribute.getAttributeId() == null ? "unknown" : attribute.getAttributeId())
                        + " " + attribute.getOperation().serializedName()
                        + " " + attribute.getSlot();
                graphics.drawString(font, truncate(line, 58), x + 10, rowY + (i * 12), TEXT, false);
            }
        }

        if (externalConflict) {
            graphics.drawString(font, "Warning: another datapack also defines runtime rules for this item.", x + 10, y + 430, ERROR, false);
        }

        graphics.drawString(font, "After saving, run /reload to apply.", x + 10, y + 448, ACCENT, false);
    }

    private void renderStatus(GuiGraphics graphics) {
        graphics.drawString(font, truncate(status, Math.max(20, width / 6)), 18, height - 26, status.startsWith("Save failed") ? ERROR : MUTED, false);
    }

    private boolean clickItemList(double mouseX, double mouseY) {
        Rect2i bounds = listBounds();
        if (!bounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }

        int row = ((int) mouseY - (bounds.getY() + 24)) / 22;
        int index = scroll + row;
        if (row >= 0 && index >= 0 && index < filteredItems.size()) {
            selectedItem = filteredItems.get(index);
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
        if (selectedItem == null) {
            status = "Select an item first.";
            return;
        }

        String rawAttribute = attributeBox.getValue().trim();
        if (rawAttribute.isBlank()) {
            status = "This item has no selected attribute. Pick a base attribute or type one.";
            return;
        }

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

        EditableItemRule rule = currentRule != null && selectedItem.id().equals(currentRule.getTargetId())
                ? currentRule.copy()
                : new EditableItemRule(selectedItem.id(), false);
        EditableAttributeModifier editedAttribute = new EditableAttributeModifier(attributeId, action, amount, operation, EditableSlotType.STANDARD, slot);
        rule.getAttributes().removeIf(existing -> editedAttribute.duplicateKey().equalsIgnoreCase(existing.duplicateKey()));
        rule.getAttributes().add(editedAttribute);

        if (!durabilityBox.getValue().isBlank()) {
            try {
                rule.setDurability(new EditableDurabilityModifier(Integer.parseInt(durabilityBox.getValue().trim())));
            } catch (NumberFormatException e) {
                status = "Durability must be an integer.";
                return;
            }
        } else {
            rule.setDurability(null);
        }

        currentRule = rule.copy();
        status = "Saving...";
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
    }

    private Rect2i listBounds() {
        return new Rect2i(18, 56, 220, height - 94);
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
        updateButtons();
    }

    private void updateButtons() {
        if (actionButton != null) {
            actionButton.setMessage(Component.literal("Action: " + action.serializedName()));
        }
        if (operationButton != null) {
            operationButton.setMessage(Component.literal("Op: " + operation.serializedName()));
            operationButton.active = selectedItem != null && action != EditableAttributeAction.REMOVE;
            operationButton.visible = selectedItem != null;
        }
        if (slotButton != null) {
            slotButton.setMessage(Component.literal("Slot: " + slot));
            slotButton.active = selectedItem != null;
            slotButton.visible = selectedItem != null;
        }
        if (useBaseButton != null) {
            useBaseButton.active = selectedItem != null && !baseAttributes.isEmpty();
            useBaseButton.visible = selectedItem != null;
        }
        if (saveButton != null) {
            saveButton.active = selectedItem != null;
        }
        if (amountBox != null) {
            amountBox.active = selectedItem != null && action != EditableAttributeAction.REMOVE;
            amountBox.visible = selectedItem != null;
        }
        if (attributeBox != null) {
            attributeBox.active = selectedItem != null;
            attributeBox.visible = selectedItem != null;
        }
        if (durabilityBox != null) {
            durabilityBox.active = selectedItem != null;
            durabilityBox.visible = selectedItem != null;
        }
        if (actionButton != null) {
            actionButton.active = selectedItem != null;
            actionButton.visible = selectedItem != null;
        }
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

        int x = 250;
        int baseY = 28 + 178;
        int rowStart = baseY + 12;
        int row = ((int) mouseY - rowStart) / 14;
        if (mouseX < x + 8 || mouseX > x + 590 || row < 0 || row >= Math.min(6, baseAttributes.size())) {
            return false;
        }

        selectedBaseAttributeIndex = row;
        loadBaseAttributeIntoForm(baseAttributes.get(row));
        status = "Selected base attribute " + (row + 1) + "/" + baseAttributes.size();
        updateButtons();
        return true;
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
}
