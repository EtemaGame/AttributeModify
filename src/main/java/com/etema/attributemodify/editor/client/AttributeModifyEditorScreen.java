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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AttributeModifyEditorScreen extends Screen {
    private static final int BG = 0xFF090D12;
    private static final int PANEL = 0xF211171D;
    private static final int PANEL_ALT = 0xEE151D25;
    private static final int PANEL_DEEP = 0xFF0B1015;
    private static final int PANEL_DISABLED = 0xFF0C1014;
    private static final int BORDER = 0xFF2A3542;
    private static final int BORDER_SOFT = 0xFF1D2630;
    private static final int SEL_BG = 0xFF163D30;
    private static final int HEADER_BG = 0xFF0C1218;
    private static final int INPUT_BG = 0xFF070B0F;
    private static final int TEXT = 0xFFE1EDF5;
    private static final int TEXT_DIM = 0xFF8EACBE;
    private static final int TEXT_MUTED = 0xFF5F7889;
    private static final int ACCENT = 0xFF45D59B;
    private static final int ACCENT2 = 0xFF3BA87D;
    private static final int WARN = 0xFFFFB74D;
    private static final int ERROR = 0xFFFF6B6B;
    private static final int HOVER_TINT = 0x1FFFFFFF;

    private static final int OUTER_MARGIN = 8;
    private static final int PANEL_GAP = 6;
    private static final int INNER_PAD = 8;
    private static final int TOP_BAR_H = 26;
    private static final int GLOBAL_FOOTER_H = 20;
    private static final int EDITOR_FOOTER_H = 24;
    private static final int LIST_MIN_W = 184;
    private static final int LIST_MAX_W = 280;
    private static final int ROW_H = 22;
    private static final int PANEL_HEADER_H = 28;
    private static final int SEARCH_H = 22;
    private static final int CONTROL_H = 17;
    private static final int BUTTON_H = 15;
    private static final int FIELD_LABEL_H = 9;
    private static final int SECTION_HEADER_H = 14;
    private static final int LABEL_INPUT_GAP = 4;
    private static final int FIELD_GAP = 11;
    private static final int SECTION_GAP = 8;
    private static final int MIN_SPLIT_W = 540;
    private static final int MIN_SPLIT_H = 230;
    private static final int MIN_INFO_H = 105;
    private static final int SCROLL_STEP = 12;
    private static final int DROPDOWN_ITEM_H = 16;
    private static final int MAX_DROPDOWN_VISIBLE = 4;
    private static final int ATTRIBUTE_DROPDOWN_ITEM_H = 20;
    private static final int ATTRIBUTE_DROPDOWN_TITLE_H = 14;
    private static final int ATTRIBUTE_DROPDOWN_SEARCH_H = 18;
    private static final int ATTRIBUTE_DROPDOWN_MAX_VISIBLE = 6;
    private static final int ATTRIBUTE_PICKER_W = 18;

    private static final String[] STANDARD_SLOTS = {"mainhand", "offhand", "head", "chest", "legs", "feet"};
    private static final EditableAttributeAction[] ACTION_VALUES = {
            EditableAttributeAction.ADD,
            EditableAttributeAction.MODIFY
    };
    private static final EditableOperationType[] OPERATION_VALUES = {
            EditableOperationType.ADDITION,
            EditableOperationType.MULTIPLY_BASE,
            EditableOperationType.MULTIPLY_TOTAL
    };

    private final List<CatalogItem> allItems = new ArrayList<>();
    private final List<CatalogItem> filteredItems = new ArrayList<>();
    private final List<AttributeOption> attributes = new ArrayList<>();
    private final List<AttributeOption> filteredAttributes = new ArrayList<>();
    private final List<BaseAttribute> baseAttributes = new ArrayList<>();

    private String lastAttributeFilterQuery = null;
    private boolean attributeFilterDirty = true;

    private EditBox searchBox;
    private EditBox attributeBox;
    private EditBox attributeSearchBox;
    private EditBox amountBox;
    private EditBox durabilityBox;
    private Button attributePickerButton;
    private Button applyButton;
    private Button closeButton;

    private CatalogItem selectedItem;
    private EditableItemRule currentRule;
    private EditableAttributeAction action = EditableAttributeAction.MODIFY;
    private EditableOperationType operation = EditableOperationType.ADDITION;
    private String slot = "mainhand";

    private String lastCatalogJson = "";
    private String lastRuleJson = "";
    private String lastSaveJson = "";
    private String status = "Connecting...";
    private boolean statusIsError;
    private boolean statusIsOk;
    private boolean externalConflict;

    private int selectedBaseAttributeIndex = -1;
    private int selectedModifierIndex = -1;
    private DraftSource draftSource = DraftSource.NONE;

    private int itemScroll;
    private int rightScroll;
    private int formScroll;
    private int dropdownScroll;

    private EditorLayout layout;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    private boolean layoutDirty = true;
    private OpenDropdown openDropdown = OpenDropdown.NONE;

    private enum DraftSource {
        NONE,
        BASE,
        MODIFIER,
        REGISTRY
    }

    private enum OpenDropdown {
        NONE,
        ATTRIBUTE,
        ACTION,
        OPERATION,
        SLOT
    }

    private record EditorLayout(
            Rect2i listPanel,
            Rect2i editorPanel,
            int contentX,
            int contentW,
            int contentTop,
            int contentBottom,
            int formX,
            int formW,
            int attributeY,
            int actionY,
            int amountY,
            int durabilityY,
            int formBottom,
            int rightX,
            int rightY,
            int rightW,
            int rightH,
            boolean split
    ) {
    }

    private record CatalogItem(ResourceLocation id, String descriptionId, String translatedName, int maxDamage, boolean damageable) {
    }

    private record BaseAttribute(ResourceLocation attributeId, double amount, EditableOperationType operation, String slot) {
    }

    private record VisibleAttributeRow(int baseIndex, int modifierIndex, BaseAttribute baseAttribute, EditableAttributeModifier modifier) {
    }

    private record AttributeOption(ResourceLocation id, String descriptionId, String translatedName, String namespace, String searchKey) {
    }

    private record DropdownBounds(int x, int y, int w, int h, int visibleRows) {
    }

    private record AttributeDropdownBounds(int x, int y, int w, int h, int visibleRows, int searchY, int listY, int listH) {
    }

    @FunctionalInterface
    private interface DropdownLabelProvider {
        String label(int index);
    }

    public AttributeModifyEditorScreen() {
        super(Component.literal("Attribute Modify Editor"));
    }

    @Override
    protected void init() {
        recomputeLayout();

        searchBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        searchBox.setHint(Component.literal("Search item..."));
        searchBox.setBordered(false);
        searchBox.setResponder(v -> {
            itemScroll = 0;
            refreshFilter();
        });
        addRenderableWidget(searchBox);

        attributeBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        attributeBox.setHint(Component.literal("minecraft:generic.attack_damage"));
        attributeBox.setBordered(false);
        addRenderableWidget(attributeBox);

        attributeSearchBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        attributeSearchBox.setHint(Component.literal("Search attribute/mod..."));
        attributeSearchBox.setBordered(false);
        attributeSearchBox.setVisible(false);
        attributeSearchBox.setResponder(v -> {
            dropdownScroll = 0;
            attributeFilterDirty = true;
            markLayoutDirty();
        });
        addRenderableWidget(attributeSearchBox);

        amountBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        amountBox.setHint(Component.literal("0"));
        amountBox.setBordered(false);
        amountBox.setFilter(this::isNumericInput);
        addRenderableWidget(amountBox);

        durabilityBox = new EditBox(font, 0, 0, 0, 14, Component.empty());
        durabilityBox.setHint(Component.literal("leave empty"));
        durabilityBox.setBordered(false);
        durabilityBox.setFilter(this::isIntegerInput);
        addRenderableWidget(durabilityBox);

        attributePickerButton = addRenderableWidget(Button.builder(Component.literal("v"), b -> toggleDropdown(OpenDropdown.ATTRIBUTE))
                .bounds(0, 0, ATTRIBUTE_PICKER_W, CONTROL_H).build());
        applyButton = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyDraft())
                .bounds(0, 0, 64, BUTTON_H).build());
        closeButton = addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(0, 0, 64, BUTTON_H).build());

        markLayoutDirty();
        ensureLayoutApplied();
        EditorNetwork.INSTANCE.sendToServer(new C2SRequestEditorCatalogPacket());
        updateWidgets();
    }

    @Override
    public void tick() {
        searchBox.tick();
        attributeBox.tick();
        attributeSearchBox.tick();
        amountBox.tick();
        durabilityBox.tick();
        consumeNetworkState();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ensureLayoutApplied();

        g.fill(0, 0, width, height, BG);
        renderTitleBar(g);
        renderListPanel(g, mx, my, layout);
        renderEditorPanel(g, mx, my, layout);
        renderStatusBar(g);

        super.render(g, mx, my, pt);
        renderDropdownOverlay(g, mx, my, pt);
    }

    private void renderTitleBar(GuiGraphics g) {
        g.fill(0, 0, width, TOP_BAR_H, HEADER_BG);
        g.fill(0, 0, width, 1, BORDER_SOFT);
        g.fill(0, TOP_BAR_H - 1, width, TOP_BAR_H, BORDER_SOFT);
        g.fill(0, 0, 3, TOP_BAR_H, ACCENT);

        String title = "Attribute Modify Editor";
        int titleY = centeredTextY(0, TOP_BAR_H);
        g.drawString(font, title, 9, titleY, ACCENT, false);

        int x = 9 + font.width(title);
        if (selectedItem != null) {
            String crumb = "  >  " + selectedItem.translatedName();
            g.drawString(font, truncatePx(crumb, Math.max(40, width - x - 120)), x, titleY, TEXT_DIM, false);
        }

        String meta = allItems.size() + " items  |  " + attributes.size() + " attrs";
        int metaX = width - font.width(meta) - 10;
        if (metaX > x + 16) {
            g.drawString(font, meta, metaX, titleY, TEXT_MUTED, false);
        }
    }

    private void renderListPanel(GuiGraphics g, int mx, int my, EditorLayout l) {
        Rect2i b = l.listPanel();
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), PANEL);
        drawBorder(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), BORDER);

        int headerBottom = b.getY() + PANEL_HEADER_H;
        int searchTop = headerBottom + 1;
        int searchBottom = searchTop + SEARCH_H;
        int listTop = searchBottom + 1;
        int listBottom = b.getY() + b.getHeight() - 18;

        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), headerBottom, HEADER_BG);
        g.fill(b.getX(), headerBottom, b.getX() + b.getWidth(), headerBottom + 1, BORDER_SOFT);
        g.drawString(font, "ITEM CATALOG", b.getX() + 8, centeredTextY(b.getY(), PANEL_HEADER_H), TEXT_DIM, false);

        String total = Integer.toString(allItems.size());
        g.drawString(font, total, b.getX() + b.getWidth() - font.width(total) - 8,
                centeredTextY(b.getY(), PANEL_HEADER_H), TEXT_MUTED, false);

        g.fill(b.getX() + 6, searchTop + 3, b.getX() + b.getWidth() - 6, searchBottom - 3, INPUT_BG);
        drawBorder(g, b.getX() + 6, searchTop + 3, b.getWidth() - 12, SEARCH_H - 6, BORDER_SOFT);

        int visible = Math.max(1, (listBottom - listTop) / ROW_H);
        itemScroll = clamp(itemScroll, 0, Math.max(0, filteredItems.size() - visible));

        g.enableScissor(b.getX() + 1, listTop, b.getX() + b.getWidth() - 1, listBottom);
        for (int i = itemScroll; i < Math.min(filteredItems.size(), itemScroll + visible); i++) {
            int rowY = listTop + (i - itemScroll) * ROW_H;
            CatalogItem item = filteredItems.get(i);
            boolean selected = selectedItem != null && selectedItem.id().equals(item.id());
            boolean hover = mx >= b.getX() && mx < b.getX() + b.getWidth() && my >= rowY && my < rowY + ROW_H;

            g.fill(b.getX() + 1, rowY, b.getX() + b.getWidth() - 1, rowY + ROW_H,
                    selected ? SEL_BG : (i % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !selected) {
                g.fill(b.getX() + 1, rowY, b.getX() + b.getWidth() - 1, rowY + ROW_H, HOVER_TINT);
            }
            if (selected) {
                g.fill(b.getX() + 1, rowY, b.getX() + 4, rowY + ROW_H, ACCENT);
            }

            Item reg = ForgeRegistries.ITEMS.getValue(item.id());
            if (reg != null) {
                g.renderItem(new ItemStack(reg), b.getX() + 7, rowY + 3);
            }

            int textX = b.getX() + 28;
            int textW = Math.max(8, b.getWidth() - 36);
            g.drawString(font, truncatePx(item.translatedName(), textW), textX, rowY + 3, selected ? ACCENT : TEXT, false);
            g.drawString(font, truncatePx(item.id().toString(), textW), textX, rowY + 13, TEXT_DIM, false);
        }
        g.disableScissor();

        if (filteredItems.size() > visible) {
            int trackTop = listTop + 2;
            int trackBottom = listBottom - 2;
            int trackH = Math.max(1, trackBottom - trackTop);
            int thumbH = Math.max(14, trackH * visible / Math.max(visible, filteredItems.size()));
            int maxScroll = Math.max(1, filteredItems.size() - visible);
            int thumbY = trackTop + (trackH - thumbH) * itemScroll / maxScroll;
            g.fill(b.getX() + b.getWidth() - 4, trackTop, b.getX() + b.getWidth() - 2, trackBottom, BORDER_SOFT);
            g.fill(b.getX() + b.getWidth() - 4, thumbY, b.getX() + b.getWidth() - 2, thumbY + thumbH, TEXT_DIM);
        }

        g.fill(b.getX(), listBottom, b.getX() + b.getWidth(), b.getY() + b.getHeight(), HEADER_BG);
        g.fill(b.getX(), listBottom - 1, b.getX() + b.getWidth(), listBottom, BORDER_SOFT);
        String count = filteredItems.size() + " / " + allItems.size();
        g.drawString(font, count, b.getX() + b.getWidth() - font.width(count) - 7, listBottom + 5, TEXT_DIM, false);
    }

    private void renderEditorPanel(GuiGraphics g, int mx, int my, EditorLayout l) {
        Rect2i b = l.editorPanel();
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), PANEL);
        drawBorder(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), BORDER);

        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + PANEL_HEADER_H, HEADER_BG);
        g.fill(b.getX(), b.getY() + PANEL_HEADER_H, b.getX() + b.getWidth(), b.getY() + PANEL_HEADER_H + 1, BORDER_SOFT);

        if (selectedItem == null) {
            String hint = "Select an item from the list";
            g.drawString(font, hint, b.getX() + (b.getWidth() - font.width(hint)) / 2,
                    b.getY() + b.getHeight() / 2 - font.lineHeight / 2, TEXT_DIM, false);
            return;
        }

        renderSelectedItemHeader(g, b);

        int formViewportTop = l.contentTop();
        int formViewportBottom = formViewportBottom(l);
        int maxFormScroll = maxFormScroll(l);
        formScroll = clamp(formScroll, 0, maxFormScroll);

        g.enableScissor(l.formX(), formViewportTop, l.formX() + l.formW(), formViewportBottom);
        g.pose().pushPose();
        g.pose().translate(0, -formScroll, 0);
        renderFormColumn(g, l, mx, my + formScroll);
        g.pose().popPose();
        g.disableScissor();

        if (maxFormScroll > 0) {
            drawSmallScrollbar(g, l.formX() + l.formW() - 3, formViewportTop + 2,
                    formViewportBottom - 2, formScroll, maxFormScroll, measureFormContentHeight(l));
        }

        renderRightPanel(g, l.rightX(), l.rightY(), l.rightW(), l.rightH(), mx, my);

        int footerY = b.getY() + b.getHeight() - EDITOR_FOOTER_H;
        g.fill(b.getX(), footerY, b.getX() + b.getWidth(), b.getY() + b.getHeight(), HEADER_BG);
        g.fill(b.getX(), footerY - 1, b.getX() + b.getWidth(), footerY, BORDER_SOFT);
        g.drawString(font, "Apply saves the current change to this item rule.",
                b.getX() + 8, centeredTextY(footerY, EDITOR_FOOTER_H), ACCENT2, false);
    }

    private void renderSelectedItemHeader(GuiGraphics g, Rect2i b) {
        Item reg = ForgeRegistries.ITEMS.getValue(selectedItem.id());
        if (reg != null) {
            g.renderItem(new ItemStack(reg), b.getX() + 8, b.getY() + 6);
        }

        int titleX = b.getX() + 30;
        int rightReserve = externalConflict ? 112 : 8;
        int titleW = Math.max(40, b.getWidth() - 30 - rightReserve);
        g.drawString(font, truncatePx(selectedItem.translatedName(), titleW), titleX, b.getY() + 5, TEXT, false);
        g.drawString(font, truncatePx(selectedItem.id().toString(), titleW), titleX, b.getY() + 15, TEXT_DIM, false);

        if (externalConflict) {
            String warn = "External conflict";
            g.drawString(font, warn, b.getX() + b.getWidth() - font.width(warn) - 8,
                    centeredTextY(b.getY(), PANEL_HEADER_H), WARN, false);
        }
    }

    private int renderFormColumn(GuiGraphics g, EditorLayout l, int mx, int logicalMy) {
        int x = l.formX();
        int w = l.formW();

        renderSectionHeader(g, x, sectionHeaderYForInput(l.attributeY()), w, "EDIT CHANGE");

        if (!hasActiveDraft()) {
            g.drawString(font, "No change selected.", x + 8, l.attributeY() + 3, TEXT_DIM, false);
            g.drawString(font, "Click a base attribute, an existing change, or + ADD.", x + 8, l.attributeY() + 15, TEXT_DIM, false);
            return l.attributeY() + 32;
        }

        drawFieldLabel(g, "Attribute", x, l.attributeY());
        renderInputBg(g, x, l.attributeY(), w, CONTROL_H, true);

        int actionW = actionColW(w);
        Rect2i actionRect = actionDropdownLogicalRect(l);
        Rect2i operationRect = operationDropdownLogicalRect(l);

        boolean operationEnabled = action != EditableAttributeAction.REMOVE;
        boolean amountEnabled = action != EditableAttributeAction.REMOVE;

        drawFieldLabel(g, "Change type", x, l.actionY());
        drawFieldLabel(g, "Effect", x + actionW + 6, l.actionY());
        renderDropdownField(g, actionRect, actionDisplay(action), openDropdown == OpenDropdown.ACTION, true, mx, logicalMy);
        renderDropdownField(g, operationRect, operationEnabled ? operationDisplay(operation) : "Not used",
                openDropdown == OpenDropdown.OPERATION, operationEnabled, mx, logicalMy);

        drawFieldLabel(g, "Value", x, l.amountY());
        renderInputBg(g, x, l.amountY(), w, CONTROL_H, amountEnabled);
        if (!amountEnabled) {
            g.drawString(font, "not used for remove", x + 6, centeredTextY(l.amountY(), CONTROL_H), TEXT_MUTED, false);
        }

        return l.amountY() + Math.max(CONTROL_H, BUTTON_H) + SECTION_GAP;
    }

    private void renderRightPanel(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int contentH = measureRightContentHeight();
        int maxScroll = Math.max(0, contentH - h);
        rightScroll = clamp(rightScroll, 0, maxScroll);

        g.fill(x, y, x + w, y + h, PANEL_ALT);
        drawBorder(g, x, y, w, h, BORDER);

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        g.pose().pushPose();
        g.pose().translate(0, -rightScroll, 0);

        int cy = y + 6;
        cy = renderAttributesSection(g, x, cy, w, mx, my);
        cy += 8;
        cy = renderDurabilitySection(g, x, cy, w);

        if (externalConflict) {
            cy += 22;
            g.drawString(font, truncatePx("Warning: another datapack also defines this item.", w - 20),
                    x + 10, cy + 3, WARN, false);
        }

        g.pose().popPose();
        g.disableScissor();

        if (maxScroll > 0) {
            drawSmallScrollbar(g, x + w - 5, y + 4, y + h - 4, rightScroll, maxScroll, contentH);
        }
    }

    private int renderAttributesSection(GuiGraphics g, int x, int cy, int w, int mx, int my) {
        int headerY = cy;
        cy = renderSectionHeader(g, x + 6, cy, w - 12, "ATTRIBUTES");

        Rect2i addRect = addButtonScreenRect(x, headerY, w);
        drawHeaderButton(g, addRect, "+ ADD", contains(addRect, mx, my), true);

        Rect2i resetRect = resetButtonScreenRect(x, headerY, w);
        boolean canReset = hasRuleOverrides();
        drawHeaderButton(g, resetRect, "RESET", canReset && contains(resetRect, mx, my), canReset);

        List<VisibleAttributeRow> rows = visibleAttributeRows();
        if (rows.isEmpty()) {
            g.drawString(font, "No attributes detected. Use + ADD to create one.", x + 10, cy + 3, TEXT_DIM, false);
            return cy + 18;
        }

        for (int i = 0; i < rows.size(); i++) {
            VisibleAttributeRow row = rows.get(i);
            int rowY = cy + i * 20;
            boolean selected = row.modifierIndex() >= 0
                    ? row.modifierIndex() == selectedModifierIndex && draftSource == DraftSource.MODIFIER
                    : row.baseIndex() == selectedBaseAttributeIndex && draftSource == DraftSource.BASE;
            boolean hover = mx >= x + 6 && mx < x + w - 6 && my >= rowY - rightScroll && my < rowY - rightScroll + 20;

            if (row.modifier() != null) {
                EditableAttributeModifier mod = row.modifier();
                String name = mod.getAttributeId() == null ? "Unknown attribute" : attributeDisplayName(mod.getAttributeId());
                String effect = effectText(mod.getAmount(), mod.getOperation(), mod.getAction());
                renderAttributeSummaryRow(g, x, rowY, w, name, effect, selected, hover, i, 22);
            } else {
                BaseAttribute attr = row.baseAttribute();
                renderAttributeSummaryRow(g, x, rowY, w, attributeDisplayName(attr.attributeId()),
                        effectText(attr.amount(), attr.operation(), EditableAttributeAction.MODIFY), selected, hover, i, 22);
            }

            renderAttributeDeleteButton(g, x, rowY, w, contains(attributeDeleteButtonScreenRect(x, rowY - rightScroll, w), mx, my));
        }

        return cy + rows.size() * 20;
    }

    private int renderDurabilitySection(GuiGraphics g, int x, int cy, int w) {
        cy = renderSectionHeader(g, x + 6, cy, w - 12, "DURABILITY");

        boolean damageable = selectedItem != null && selectedItem.damageable();
        String vanillaStr = damageable ? Integer.toString(selectedItem.maxDamage()) : "not damageable";
        String vanillaLabel = "Vanilla: " + vanillaStr;
        g.drawString(font, truncatePx(vanillaLabel, w - 20), x + 10, cy + 2, TEXT_MUTED, false);
        cy += font.lineHeight + 6;

        int inputW = Math.min(170, w - 16);
        boolean editable = damageable;
        renderInputBg(g, x + 8, cy, inputW, CONTROL_H, editable);

        if (durabilityBox != null) {
            durabilityBox.setX(x + 13);
            durabilityBox.setY(centeredEditBoxY(cy));
            durabilityBox.setWidth(Math.max(20, inputW - 10));
            durabilityBox.visible = true;
            durabilityBox.active = editable;

            boolean hasOverride = currentRule != null
                    && currentRule.getDurability() != null
                    && currentRule.getDurability().getDurability() != null;
            String hint = hasOverride
                    ? Integer.toString(currentRule.getDurability().getDurability())
                    : "Vanilla: " + vanillaStr;
            durabilityBox.setHint(Component.literal(hint));
        }

        return cy + CONTROL_H + 6;
    }

    private void renderAttributeSummaryRow(GuiGraphics g, int x, int rowY, int w, String name, String effect,
                                           boolean selected, boolean hover, int rowIndex, int rightInset) {
        drawCleanRowBackground(g, x + 6, rowY, w - 12, 20, selected, hover, rowIndex);

        int effectRight = x + w - 10 - Math.max(0, rightInset);
        int effectW = Math.min(96, Math.max(48, font.width(effect) + 4));
        int nameW = Math.max(20, effectRight - (x + 12) - effectW - 12);
        int color = selected ? ACCENT : TEXT;
        int effectColor = selected ? ACCENT : TEXT_DIM;

        g.drawString(font, truncatePx(name, nameW), x + 12, centeredTextY(rowY, 20), color, false);
        String clippedEffect = truncatePx(effect, effectW);
        g.drawString(font, clippedEffect, effectRight - font.width(clippedEffect), centeredTextY(rowY, 20), effectColor, false);
    }

    private void renderAttributeDeleteButton(GuiGraphics g, int x, int rowY, int w, boolean hover) {
        Rect2i rect = attributeDeleteButtonScreenRect(x, rowY, w);
        int border = hover ? ERROR : BORDER_SOFT;
        int textColor = hover ? ERROR : TEXT_MUTED;
        int bg = hover ? 0xFF24171B : INPUT_BG;
        g.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), bg);
        drawBorder(g, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), border);
        g.drawString(font, "X", rect.getX() + Math.max(1, (rect.getWidth() - font.width("X")) / 2),
                centeredTextY(rect.getY(), rect.getHeight()), textColor, false);
    }

    private Rect2i attributeDeleteButtonScreenRect(int x, int rowY, int w) {
        return new Rect2i(x + w - 28, rowY + 3, 16, 14);
    }

    private void drawHeaderButton(GuiGraphics g, Rect2i rect, String label, boolean hover, boolean enabled) {
        int border = !enabled ? BORDER_SOFT : (hover ? ACCENT : BORDER);
        int textColor = !enabled ? TEXT_MUTED : ACCENT;
        g.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(),
                hover && enabled ? 0xFF182028 : INPUT_BG);
        drawBorder(g, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), border);
        g.drawString(font, label, rect.getX() + Math.max(3, (rect.getWidth() - font.width(label)) / 2),
                centeredTextY(rect.getY(), rect.getHeight()), textColor, false);
    }

    private Rect2i addButtonScreenRect(int x, int headerY, int w) {
        return new Rect2i(x + w - 106, headerY + 2, 42, 14);
    }

    private Rect2i resetButtonScreenRect(int x, int headerY, int w) {
        return new Rect2i(x + w - 60, headerY + 2, 50, 14);
    }

    private void renderDropdownOverlay(GuiGraphics g, int mx, int my, float pt) {
        if (layout == null || selectedItem == null || openDropdown == OpenDropdown.NONE) {
            return;
        }

        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        if (openDropdown == OpenDropdown.ATTRIBUTE) {
            Rect2i anchor = attributeDropdownScreenRect(layout);
            List<AttributeOption> options = filteredAttributeOptions();
            if (isRectVisibleInForm(anchor, layout)) {
                AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, options.size());
                renderAttributeDropdownMenu(g, bounds, options, selectedAttributeOptionIndex(options), mx, my);
                if (attributeSearchBox != null && attributeSearchBox.visible) {
                    attributeSearchBox.render(g, mx, my, pt);
                }
            }
        } else if (openDropdown == OpenDropdown.ACTION) {
            Rect2i anchor = actionDropdownScreenRect(layout);
            renderDropdownMenu(g, anchor, ACTION_VALUES.length, i -> actionDisplay(ACTION_VALUES[i]), indexOfAction(action), mx, my);
        } else if (openDropdown == OpenDropdown.OPERATION) {
            Rect2i anchor = operationDropdownScreenRect(layout);
            if (action != EditableAttributeAction.REMOVE) {
                renderDropdownMenu(g, anchor, OPERATION_VALUES.length, i -> operationDisplay(OPERATION_VALUES[i]), indexOfOperation(operation), mx, my);
            }
        } else if (openDropdown == OpenDropdown.SLOT) {
            Rect2i anchor = slotDropdownScreenRect(layout);
            renderDropdownMenu(g, anchor, STANDARD_SLOTS.length, i -> STANDARD_SLOTS[i], indexOfSlot(slot), mx, my);
        }

        g.pose().popPose();
    }

    private void renderDropdownField(GuiGraphics g, Rect2i rect, String value, boolean open, boolean enabled, int mx, int my) {
        boolean hover = enabled && contains(rect, mx, my);
        int bg = !enabled ? PANEL_DISABLED : (hover ? 0xFF182028 : INPUT_BG);
        int border = !enabled ? BORDER_SOFT : (open ? ACCENT : BORDER);
        int textColor = enabled ? TEXT : TEXT_MUTED;

        g.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), bg);
        drawBorder(g, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), border);
        g.drawString(font, truncatePx(value, rect.getWidth() - 10), rect.getX() + 6, centeredTextY(rect.getY(), rect.getHeight()), textColor, false);
    }

    private void renderDropdownMenu(GuiGraphics g, Rect2i anchor, int count, DropdownLabelProvider labelProvider, int selectedIndex, int mx, int my) {
        DropdownBounds bounds = dropdownBounds(anchor, count);
        dropdownScroll = clamp(dropdownScroll, 0, Math.max(0, count - bounds.visibleRows()));

        g.fill(bounds.x(), bounds.y(), bounds.x() + bounds.w(), bounds.y() + bounds.h(), PANEL);
        drawBorder(g, bounds.x(), bounds.y(), bounds.w(), bounds.h(), ACCENT);

        for (int visualIndex = 0; visualIndex < bounds.visibleRows(); visualIndex++) {
            int itemIndex = dropdownScroll + visualIndex;
            if (itemIndex < 0 || itemIndex >= count) {
                continue;
            }

            int rowY = bounds.y() + visualIndex * DROPDOWN_ITEM_H;
            boolean hover = mx >= bounds.x() && mx < bounds.x() + bounds.w() && my >= rowY && my < rowY + DROPDOWN_ITEM_H;
            boolean selected = itemIndex == selectedIndex;

            g.fill(bounds.x() + 1, rowY, bounds.x() + bounds.w() - 1, rowY + DROPDOWN_ITEM_H,
                    selected ? SEL_BG : (visualIndex % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !selected) {
                g.fill(bounds.x() + 1, rowY, bounds.x() + bounds.w() - 1, rowY + DROPDOWN_ITEM_H, HOVER_TINT);
            }
            if (selected) {
                g.fill(bounds.x() + 1, rowY, bounds.x() + 4, rowY + DROPDOWN_ITEM_H, ACCENT);
            }

            g.drawString(font, truncatePx(labelProvider.label(itemIndex), bounds.w() - 16),
                    bounds.x() + 7, centeredTextY(rowY, DROPDOWN_ITEM_H), selected ? ACCENT : TEXT, false);
        }

        if (count > bounds.visibleRows()) {
            int maxScroll = Math.max(1, count - bounds.visibleRows());
            int trackX = bounds.x() + bounds.w() - 4;
            int trackY1 = bounds.y() + 2;
            int trackY2 = bounds.y() + bounds.h() - 2;
            int trackH = Math.max(1, trackY2 - trackY1);
            int thumbH = Math.max(10, trackH * bounds.visibleRows() / count);
            int thumbY = trackY1 + (trackH - thumbH) * dropdownScroll / maxScroll;
            g.fill(trackX, trackY1, trackX + 2, trackY2, BORDER_SOFT);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, TEXT_DIM);
        }
    }

    private void renderAttributeDropdownMenu(GuiGraphics g, AttributeDropdownBounds bounds, List<AttributeOption> options, int selectedIndex, int mx, int my) {
        dropdownScroll = clamp(dropdownScroll, 0, Math.max(0, options.size() - bounds.visibleRows()));
        g.fill(bounds.x(), bounds.y(), bounds.x() + bounds.w(), bounds.y() + bounds.h(), PANEL);
        drawBorder(g, bounds.x(), bounds.y(), bounds.w(), bounds.h(), ACCENT);

        g.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + bounds.w() - 1, bounds.y() + ATTRIBUTE_DROPDOWN_TITLE_H, HEADER_BG);
        g.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + 4, bounds.y() + ATTRIBUTE_DROPDOWN_TITLE_H, ACCENT);
        g.drawString(font, "ATTRIBUTE REGISTRY", bounds.x() + 7, bounds.y() + 3, TEXT_DIM, false);
        String count = Integer.toString(options.size());
        g.drawString(font, count, bounds.x() + bounds.w() - font.width(count) - 8, bounds.y() + 3, TEXT_MUTED, false);

        if (attributeSearchBox != null) {
            attributeSearchBox.setX(bounds.x() + 3);
            attributeSearchBox.setY(bounds.searchY());
            attributeSearchBox.setWidth(bounds.w() - 6);
            attributeSearchBox.setHeight(ATTRIBUTE_DROPDOWN_SEARCH_H);
            attributeSearchBox.visible = true;
            attributeSearchBox.active = true;
        }

        if (options.isEmpty()) {
            g.drawString(font, "No matches", bounds.x() + 7, bounds.listY() + 4, TEXT_MUTED, false);
            return;
        }

        for (int visualIndex = 0; visualIndex < bounds.visibleRows(); visualIndex++) {
            int itemIndex = dropdownScroll + visualIndex;
            if (itemIndex < 0 || itemIndex >= options.size()) {
                continue;
            }

            int rowY = bounds.listY() + visualIndex * ATTRIBUTE_DROPDOWN_ITEM_H;
            AttributeOption option = options.get(itemIndex);
            boolean hover = mx >= bounds.x() && mx < bounds.x() + bounds.w() && my >= rowY && my < rowY + ATTRIBUTE_DROPDOWN_ITEM_H;
            boolean selected = itemIndex == selectedIndex;

            g.fill(bounds.x() + 1, rowY, bounds.x() + bounds.w() - 1, rowY + ATTRIBUTE_DROPDOWN_ITEM_H,
                    selected ? SEL_BG : (visualIndex % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !selected) {
                g.fill(bounds.x() + 1, rowY, bounds.x() + bounds.w() - 1, rowY + ATTRIBUTE_DROPDOWN_ITEM_H, HOVER_TINT);
            }
            if (selected) {
                g.fill(bounds.x() + 1, rowY, bounds.x() + 4, rowY + ATTRIBUTE_DROPDOWN_ITEM_H, ACCENT);
            }

            g.drawString(font, truncatePx(option.translatedName(), bounds.w() - 14),
                    bounds.x() + 7, rowY + 2, selected ? ACCENT : TEXT, false);
            g.drawString(font, truncatePx(option.id().toString(), bounds.w() - 14),
                    bounds.x() + 7, rowY + 11, TEXT_DIM, false);
        }

        if (options.size() > bounds.visibleRows()) {
            int maxScroll = Math.max(1, options.size() - bounds.visibleRows());
            int trackX = bounds.x() + bounds.w() - 4;
            int trackY1 = bounds.listY() + 2;
            int trackY2 = bounds.listY() + bounds.listH() - 2;
            int trackH = Math.max(1, trackY2 - trackY1);
            int thumbH = Math.max(10, trackH * bounds.visibleRows() / Math.max(1, options.size()));
            int thumbY = trackY1 + (trackH - thumbH) * dropdownScroll / maxScroll;
            g.fill(trackX, trackY1, trackX + 2, trackY2, BORDER_SOFT);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, TEXT_DIM);
        }
    }

    private void renderStatusBar(GuiGraphics g) {
        int y = height - GLOBAL_FOOTER_H;
        g.fill(0, y, width, height, HEADER_BG);
        g.fill(0, y, width, y + 1, BORDER_SOFT);

        int color = statusIsError ? ERROR : (statusIsOk ? ACCENT : TEXT_DIM);
        int pillW = statusIsError ? 34 : (statusIsOk ? 30 : 22);
        int pillX = 8;
        int pillY = y + 4;
        g.fill(pillX, pillY, pillX + pillW, pillY + 12, INPUT_BG);
        drawBorder(g, pillX, pillY, pillW, 12, statusIsError ? ERROR : (statusIsOk ? ACCENT2 : BORDER_SOFT));

        String icon = statusIsError ? "ERR" : (statusIsOk ? "OK" : "...");
        g.drawString(font, icon, pillX + 5, y + 6, color, false);
        g.drawString(font, truncatePx(status, Math.max(40, width - 170)),
                pillX + pillW + 7, centeredTextY(y, GLOBAL_FOOTER_H), color, false);
    }

    private int renderSectionHeader(GuiGraphics g, int x, int y, int w, String title) {
        g.fill(x, y, x + w, y + 1, BORDER_SOFT);
        g.fill(x, y + 2, x + 3, y + SECTION_HEADER_H - 1, ACCENT);
        g.drawString(font, title, x + 8, y + 3, TEXT_DIM, false);
        return y + SECTION_HEADER_H + 2;
    }

    private void drawFieldLabel(GuiGraphics g, String label, int x, int inputY) {
        g.drawString(font, label, x + 4, inputY - FIELD_LABEL_H - LABEL_INPUT_GAP, TEXT_DIM, false);
    }

    private int sectionHeaderYForInput(int inputY) {
        return inputY - FIELD_LABEL_H - LABEL_INPUT_GAP - SECTION_HEADER_H - 2;
    }

    private int centeredTextY(int y, int h) {
        return y + Math.max(0, (h - font.lineHeight) / 2);
    }

    private int centeredEditBoxY(int inputY) {
        return centeredTextY(inputY, CONTROL_H);
    }

    private void renderInputBg(GuiGraphics g, int x, int y, int w, int h, boolean enabled) {
        g.fill(x, y, x + w, y + h, enabled ? INPUT_BG : PANEL_DISABLED);
        drawBorder(g, x, y, w, h, enabled ? BORDER : BORDER_SOFT);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSmallScrollbar(GuiGraphics g, int x, int y1, int y2, int value, int maxValue, int contentH) {
        int trackH = Math.max(1, y2 - y1);
        int thumbH = Math.max(14, trackH * trackH / Math.max(trackH, contentH));
        int thumbY = y1 + (trackH - thumbH) * value / Math.max(1, maxValue);
        g.fill(x, y1, x + 2, y2, BORDER_SOFT);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, TEXT_DIM);
    }

    private void recomputeLayout() {
        layout = computeLayout();
        lastLayoutWidth = width;
        lastLayoutHeight = height;
        layoutDirty = true;
    }

    private void ensureLayoutApplied() {
        if (layout == null || lastLayoutWidth != width || lastLayoutHeight != height) {
            recomputeLayout();
        }

        if (layoutDirty && layout != null) {
            applyLayout(layout);
            layoutDirty = false;
        }
    }

    private void markLayoutDirty() {
        layoutDirty = true;
    }

    private EditorLayout computeLayout() {
        int listW = clamp((int) Math.round(width * 0.235), LIST_MIN_W, LIST_MAX_W);
        int panelY = TOP_BAR_H + 6;
        int panelH = Math.max(180, height - panelY - GLOBAL_FOOTER_H - 6);
        int editorX = OUTER_MARGIN + listW + PANEL_GAP;
        int editorW = Math.max(260, width - OUTER_MARGIN - editorX);

        Rect2i listPanel = new Rect2i(OUTER_MARGIN, panelY, listW, panelH);
        Rect2i editorPanel = new Rect2i(editorX, panelY, editorW, panelH);

        int contentX = editorX + INNER_PAD;
        int contentW = Math.max(80, editorW - INNER_PAD * 2);
        int contentTop = panelY + PANEL_HEADER_H + INNER_PAD;
        int contentBottom = panelY + panelH - EDITOR_FOOTER_H - 4;
        boolean split = contentW >= MIN_SPLIT_W && panelH >= MIN_SPLIT_H;

        int formX = contentX;
        int formW;
        if (split) {
            int preferred = Math.max(270, Math.min(365, (int) Math.round(contentW * 0.52)));
            formW = Math.min(preferred, contentW - SECTION_GAP - 210);
            formW = Math.max(255, formW);
        } else {
            formW = contentW;
        }

        int y = contentTop + SECTION_HEADER_H + 2 + FIELD_LABEL_H + LABEL_INPUT_GAP;
        int attributeY = y;
        y += CONTROL_H + FIELD_GAP + FIELD_LABEL_H + LABEL_INPUT_GAP;
        int actionY = y;
        y += BUTTON_H + FIELD_GAP + FIELD_LABEL_H + LABEL_INPUT_GAP;
        int amountY = y;
        y += Math.max(CONTROL_H, BUTTON_H) + SECTION_GAP + SECTION_HEADER_H + 2 + FIELD_LABEL_H + LABEL_INPUT_GAP;
        int durabilityY = y;
        int formBottom = amountY + Math.max(CONTROL_H, BUTTON_H) + SECTION_GAP;

        int rightX;
        int rightY;
        int rightW;
        int rightH;

        if (split) {
            rightX = formX + formW + SECTION_GAP;
            rightY = contentTop;
            rightW = Math.max(200, contentX + contentW - rightX);
            rightH = Math.max(MIN_INFO_H, contentBottom - rightY);
        } else {
            int contentH = Math.max(1, contentBottom - contentTop);
            int formViewportH = Math.max(118, (contentH * 55) / 100);
            int formViewportBottom = Math.min(contentBottom - MIN_INFO_H - SECTION_GAP, contentTop + formViewportH);
            formViewportBottom = Math.max(contentTop + 90, formViewportBottom);
            rightX = contentX;
            rightY = formViewportBottom + SECTION_GAP;
            rightW = contentW;
            rightH = Math.max(70, contentBottom - rightY);
        }

        return new EditorLayout(
                listPanel,
                editorPanel,
                contentX,
                contentW,
                contentTop,
                contentBottom,
                formX,
                formW,
                attributeY,
                actionY,
                amountY,
                durabilityY,
                formBottom,
                rightX,
                rightY,
                rightW,
                rightH,
                split
        );
    }

    private void applyLayout(EditorLayout l) {
        formScroll = clamp(formScroll, 0, maxFormScroll(l));

        if (searchBox != null) {
            searchBox.setX(l.listPanel().getX() + 11);
            searchBox.setY(centeredTextY(l.listPanel().getY() + PANEL_HEADER_H + 3, SEARCH_H - 6));
            searchBox.setWidth(l.listPanel().getWidth() - 22);
        }

        boolean activeDraft = hasActiveDraft();
        int attributeWidth = Math.max(24, l.formW() - 10);
        setEditBoxBounds(attributeBox, l.formX() + 5, centeredEditBoxY(l.attributeY()) - formScroll, attributeWidth);
        setEditBoxBounds(amountBox, l.formX() + 5, centeredEditBoxY(l.amountY()) - formScroll, l.formW() - 10);

        if (attributePickerButton != null) {
            // The attribute field itself opens the picker. The extra arrow button was visual noise.
            attributePickerButton.setX(l.formX() + l.formW() - ATTRIBUTE_PICKER_W);
            attributePickerButton.setY(centeredEditBoxY(l.attributeY()) - formScroll);
            attributePickerButton.setWidth(ATTRIBUTE_PICKER_W);
            attributePickerButton.setHeight(CONTROL_H);
            attributePickerButton.visible = false;
            attributePickerButton.active = false;
        }

        if (attributeSearchBox != null && openDropdown == OpenDropdown.ATTRIBUTE && selectedItem != null) {
            Rect2i anchor = attributeDropdownScreenRect(l);
            AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, filteredAttributeOptions().size());
            attributeSearchBox.setX(bounds.x() + 3);
            attributeSearchBox.setY(bounds.searchY());
            attributeSearchBox.setWidth(Math.max(10, bounds.w() - 6));
            attributeSearchBox.setHeight(ATTRIBUTE_DROPDOWN_SEARCH_H);
            attributeSearchBox.visible = true;
            attributeSearchBox.active = true;
        }

        applyFormVisibility(attributeBox, l.attributeY() - formScroll, CONTROL_H, activeDraft, l);
        applyFormVisibility(amountBox, l.amountY() - formScroll, CONTROL_H, activeDraft && action != EditableAttributeAction.REMOVE, l);

        if (attributePickerButton != null) {
            attributePickerButton.visible = false;
            attributePickerButton.active = false;
        }

        hideWidgetsCoveredByDropdown(l);

        if (applyButton != null) {
            applyButton.setX(width - OUTER_MARGIN - 64);
            applyButton.setY(height - GLOBAL_FOOTER_H + 3);
            applyButton.setWidth(64);
            applyButton.setHeight(BUTTON_H);
            applyButton.visible = true;
            applyButton.active = activeDraft || (selectedItem != null && selectedItem.damageable());
        }

        if (closeButton != null) {
            closeButton.setX(width - OUTER_MARGIN - 64 - 68);
            closeButton.setY(height - GLOBAL_FOOTER_H + 3);
            closeButton.setWidth(64);
            closeButton.setHeight(BUTTON_H);
            closeButton.visible = true;
            closeButton.active = true;
        }
    }

    private void setEditBoxBounds(EditBox box, int x, int y, int w) {
        if (box == null) {
            return;
        }
        box.setX(x);
        box.setY(y);
        box.setWidth(Math.max(20, w));
    }

    private void applyFormVisibility(EditBox widget, int y, int h, boolean visible, EditorLayout l) {
        if (widget == null) {
            return;
        }
        int top = l.contentTop();
        int bottom = formViewportBottom(l);
        widget.visible = visible && y + h > top && y < bottom;
        widget.active = widget.visible;
    }

    private void hideWidgetsCoveredByDropdown(EditorLayout l) {
        if (openDropdown == OpenDropdown.NONE || selectedItem == null) {
            return;
        }

        Rect2i anchor = dropdownAnchor(openDropdown, l);
        if (anchor == null) {
            return;
        }

        Rect2i menu;
        if (openDropdown == OpenDropdown.ATTRIBUTE) {
            AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, dropdownItemCount(openDropdown));
            menu = new Rect2i(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        } else {
            DropdownBounds bounds = dropdownBounds(anchor, dropdownItemCount(openDropdown));
            menu = new Rect2i(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        }

        hideIfIntersects(attributeBox, menu);
        hideIfIntersects(amountBox, menu);
        hideIfIntersects(durabilityBox, menu);
    }

    private void hideIfIntersects(EditBox box, Rect2i rect) {
        if (box == null || !box.visible) {
            return;
        }

        Rect2i boxRect = new Rect2i(box.getX(), box.getY(), box.getWidth(), 14);
        if (intersects(boxRect, rect)) {
            box.visible = false;
            box.active = false;
        }
    }

    private boolean intersects(Rect2i a, Rect2i b) {
        return a.getX() < b.getX() + b.getWidth()
                && a.getX() + a.getWidth() > b.getX()
                && a.getY() < b.getY() + b.getHeight()
                && a.getY() + a.getHeight() > b.getY();
    }

    private int formViewportBottom(EditorLayout l) {
        return l.split() ? l.contentBottom() : Math.max(l.contentTop() + 40, l.rightY() - SECTION_GAP);
    }

    private int measureFormContentHeight(EditorLayout l) {
        if (!hasActiveDraft()) {
            return Math.max(0, l.attributeY() + 32 - l.contentTop());
        }
        return Math.max(0, l.formBottom() - l.contentTop());
    }

    private int maxFormScroll(EditorLayout l) {
        int viewportH = Math.max(1, formViewportBottom(l) - l.contentTop());
        return Math.max(0, measureFormContentHeight(l) - viewportH);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        ensureLayoutApplied();

        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }

        if (handleAttributeDropdownClick(mx, my, button)) {
            return true;
        }

        if (handleDropdownMenuClick(mx, my)) {
            return true;
        }

        if (clickDropdownToggles(mx, my)) {
            return true;
        }

        if (clickResetButton(mx, my)) {
            closeDropdown();
            return true;
        }

        if (clickAddButton(mx, my)) {
            return true;
        }

        if (clickDeleteAttributeModifierButton(mx, my)) {
            closeDropdown();
            return true;
        }

        if (clickPendingModifierList(mx, my)) {
            closeDropdown();
            return true;
        }

        if (clickItemList(mx, my)) {
            closeDropdown();
            return true;
        }

        closeDropdown();
        return super.mouseClicked(mx, my, button);
    }

    private boolean handleAttributeDropdownClick(double mx, double my, int button) {
        if (openDropdown != OpenDropdown.ATTRIBUTE || layout == null || selectedItem == null) {
            return false;
        }

        Rect2i anchor = attributeDropdownScreenRect(layout);
        List<AttributeOption> options = filteredAttributeOptions();
        AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, options.size());
        Rect2i menu = new Rect2i(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        Rect2i search = new Rect2i(bounds.x() + 3, bounds.searchY(), Math.max(10, bounds.w() - 6), ATTRIBUTE_DROPDOWN_SEARCH_H);

        if (contains(search, mx, my)) {
            return super.mouseClicked(mx, my, button);
        }

        int index = attributeDropdownMenuIndexAt(anchor, options.size(), mx, my);
        if (index >= 0) {
            selectRegistryAttribute(options.get(index));
            closeDropdown();
            return true;
        }

        return contains(menu, mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        ensureLayoutApplied();

        if (openDropdown != OpenDropdown.NONE) {
            int count = dropdownItemCount(openDropdown);
            Rect2i anchor = dropdownAnchor(openDropdown, layout);
            if (anchor != null) {
                int max;
                if (openDropdown == OpenDropdown.ATTRIBUTE) {
                    AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, count);
                    max = Math.max(0, count - bounds.visibleRows());
                } else {
                    DropdownBounds bounds = dropdownBounds(anchor, count);
                    max = Math.max(0, count - bounds.visibleRows());
                }
                if (max > 0) {
                    int dropdownStep = delta > 0 ? -1 : 1;
                    dropdownScroll = clamp(dropdownScroll + dropdownStep, 0, max);
                    markLayoutDirty();
                }
            }
            return true;
        }

        int step = delta > 0 ? -SCROLL_STEP : SCROLL_STEP;

        if (layout != null && containsArea(mx, my, layout.formX(), layout.contentTop(), layout.formW(), formViewportBottom(layout) - layout.contentTop())) {
            int max = maxFormScroll(layout);
            if (max > 0) {
                formScroll = clamp(formScroll + step, 0, max);
                markLayoutDirty();
                return true;
            }
        }

        if (layout != null && containsArea(mx, my, layout.rightX(), layout.rightY(), layout.rightW(), layout.rightH())) {
            int max = Math.max(0, measureRightContentHeight() - layout.rightH());
            if (max > 0) {
                rightScroll = clamp(rightScroll + step, 0, max);
                return true;
            }
        }

        if (layout != null && layout.listPanel().contains((int) mx, (int) my)) {
            Rect2i b = layout.listPanel();
            int listTop = b.getY() + PANEL_HEADER_H + 1 + SEARCH_H + 1;
            int listBottom = b.getY() + b.getHeight() - 18;
            int visible = Math.max(1, (listBottom - listTop) / ROW_H);
            int listStep = delta > 0 ? -3 : 3;
            itemScroll = clamp(itemScroll + listStep, 0, Math.max(0, filteredItems.size() - visible));
            return true;
        }

        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean clickDropdownToggles(double mx, double my) {
        if (layout == null || selectedItem == null || !hasActiveDraft()) {
            return false;
        }

        Rect2i attributeRect = attributeDropdownScreenRect(layout);
        Rect2i actionRect = actionDropdownScreenRect(layout);
        Rect2i operationRect = operationDropdownScreenRect(layout);
        if (contains(attributeRect, mx, my) && isRectVisibleInForm(attributeRect, layout)) {
            toggleDropdown(OpenDropdown.ATTRIBUTE);
            return true;
        }
        if (contains(actionRect, mx, my) && isRectVisibleInForm(actionRect, layout)) {
            toggleDropdown(OpenDropdown.ACTION);
            return true;
        }
        if (action != EditableAttributeAction.REMOVE && contains(operationRect, mx, my) && isRectVisibleInForm(operationRect, layout)) {
            toggleDropdown(OpenDropdown.OPERATION);
            return true;
        }
        return false;
    }

    private boolean handleDropdownMenuClick(double mx, double my) {
        if (layout == null || openDropdown == OpenDropdown.NONE || selectedItem == null) {
            return false;
        }

        if (openDropdown == OpenDropdown.ACTION) {
            int index = dropdownMenuIndexAt(actionDropdownScreenRect(layout), ACTION_VALUES.length, mx, my);
            if (index >= 0) {
                action = ACTION_VALUES[index];
                closeDropdown();
                updateWidgets();
                return true;
            }
        } else if (openDropdown == OpenDropdown.OPERATION) {
            int index = dropdownMenuIndexAt(operationDropdownScreenRect(layout), OPERATION_VALUES.length, mx, my);
            if (index >= 0) {
                operation = OPERATION_VALUES[index];
                closeDropdown();
                updateWidgets();
                return true;
            }
        } else if (openDropdown == OpenDropdown.SLOT) {
            int index = dropdownMenuIndexAt(slotDropdownScreenRect(layout), STANDARD_SLOTS.length, mx, my);
            if (index >= 0) {
                slot = STANDARD_SLOTS[index];
                closeDropdown();
                updateWidgets();
                return true;
            }
        }

        return false;
    }

    private boolean clickItemList(double mx, double my) {
        Rect2i b = layout.listPanel();
        int startY = b.getY() + PANEL_HEADER_H + 1 + SEARCH_H + 1;
        int endY = b.getY() + b.getHeight() - 18;
        if (mx < b.getX() || mx >= b.getX() + b.getWidth() || my < startY || my >= endY) {
            return false;
        }

        int index = itemScroll + ((int) my - startY) / ROW_H;
        if (index >= 0 && index < filteredItems.size()) {
            selectedItem = filteredItems.get(index);
            currentRule = new EditableItemRule(selectedItem.id(), false);
            baseAttributes.clear();
            resetDraft();
            rightScroll = 0;
            formScroll = 0;
            status = "Loading rule...";
            statusIsError = false;
            statusIsOk = false;
            lastRuleJson = "";
            EditorNetwork.INSTANCE.sendToServer(new C2SRequestItemRulePacket(selectedItem.id(), false));
            updateWidgets();
            return true;
        }

        return false;
    }

    private boolean clickAddButton(double mx, double my) {
        if (layout == null || selectedItem == null) {
            return false;
        }

        Rect2i button = addButtonRectForClick(layout);
        if (!contains(button, mx, my)) {
            return false;
        }

        startRegistryDraft();
        return true;
    }


    private boolean clickResetButton(double mx, double my) {
        if (layout == null || selectedItem == null || !hasRuleOverrides()) {
            return false;
        }

        Rect2i button = resetButtonRectForClick(layout);
        if (!contains(button, mx, my)) {
            return false;
        }

        resetRuleToVanilla();
        return true;
    }

    private boolean clickDeleteAttributeModifierButton(double mx, double my) {
        if (layout == null || selectedItem == null) {
            return false;
        }

        List<VisibleAttributeRow> rows = visibleAttributeRows();
        int top = attributesRowsTop(layout) - rightScroll;

        for (int i = 0; i < rows.size(); i++) {
            int screenRowY = top + i * 20;
            Rect2i deleteRect = attributeDeleteButtonScreenRect(layout.rightX(), screenRowY, layout.rightW());
            if (contains(deleteRect, mx, my)) {
                removeVisibleAttributeRow(rows.get(i));
                return true;
            }
        }

        return false;
    }

    private void removeVisibleAttributeRow(VisibleAttributeRow row) {
        if (selectedItem == null) {
            return;
        }

        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();
        String key;

        if (row.modifier() != null && row.modifierIndex() >= 0 && row.modifierIndex() < rule.getAttributes().size()) {
            EditableAttributeModifier removed = rule.getAttributes().remove(row.modifierIndex());
            if (draftSource == DraftSource.MODIFIER && selectedModifierIndex == row.modifierIndex()) {
                resetDraft();
            }

            String name = removed.getAttributeId() == null ? "attribute change" : attributeDisplayName(removed.getAttributeId());
            status = "Deleting custom change: " + name;
        } else if (row.baseAttribute() != null) {
            BaseAttribute attr = row.baseAttribute();
            key = visibleAttributeKey(attr.attributeId(), attr.operation(), EditableSlotType.STANDARD, attr.slot()).toLowerCase(Locale.ROOT);
            rule.getAttributes().removeIf(existing -> existing != null
                    && existing.getAttributeId() != null
                    && existing.getAction() != EditableAttributeAction.REMOVE
                    && visibleAttributeKey(existing.getAttributeId(), existing.getOperation(), existing.getSlotType(), existing.getSlot())
                    .toLowerCase(Locale.ROOT).equals(key));
            EditableAttributeModifier remove = new EditableAttributeModifier(
                    attr.attributeId(),
                    EditableAttributeAction.REMOVE,
                    null,
                    attr.operation(),
                    EditableSlotType.STANDARD,
                    attr.slot()
            );
            rule.getAttributes().add(remove);
            if (draftSource == DraftSource.BASE && selectedBaseAttributeIndex == row.baseIndex()) {
                resetDraft();
            }

            status = "Removing base attribute: " + attributeDisplayName(attr.attributeId());
        } else {
            return;
        }

        statusIsError = false;
        statusIsOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        currentRule = rule.copy();
        markLayoutDirty();
        updateWidgets();
    }

    private boolean clickPendingModifierList(double mx, double my) {
        if (layout == null || selectedItem == null) {
            return false;
        }

        List<VisibleAttributeRow> rows = visibleAttributeRows();
        int top = attributesRowsTop(layout) - rightScroll;
        int row = ((int) my - top) / 20;

        if (mx < layout.rightX() + 6 || mx > layout.rightX() + layout.rightW() - 6 || row < 0 || row >= rows.size()) {
            return false;
        }

        VisibleAttributeRow visible = rows.get(row);
        if (visible.modifier() != null) {
            selectedModifierIndex = visible.modifierIndex();
            selectedBaseAttributeIndex = visible.baseIndex();
            draftSource = DraftSource.MODIFIER;
            loadModifierIntoDraft(visible.modifier());
            status = "Attribute change selected.";
        } else {
            selectedBaseAttributeIndex = visible.baseIndex();
            selectedModifierIndex = -1;
            draftSource = DraftSource.BASE;
            loadBaseAttributeIntoDraft(visible.baseAttribute());
            status = "Attribute loaded. Edit values and apply.";
        }
        statusIsError = false;
        statusIsOk = false;
        return true;
    }

    private Rect2i addButtonRectForClick(EditorLayout l) {
        int headerY = attributesHeaderY(l) - rightScroll;
        int x = l.rightX() + 6;
        int w = l.rightW() - 12;
        return addButtonScreenRect(x, headerY, w);
    }

    private Rect2i resetButtonRectForClick(EditorLayout l) {
        int headerY = attributesHeaderY(l) - rightScroll;
        int x = l.rightX() + 6;
        int w = l.rightW() - 12;
        return resetButtonScreenRect(x, headerY, w);
    }

    private int attributesHeaderY(EditorLayout l) {
        return l.rightY() + 6;
    }

    private int attributesRowsTop(EditorLayout l) {
        return attributesHeaderY(l) + SECTION_HEADER_H + 2;
    }

    private int visibleAttributeRowCount() {
        return visibleAttributeRows().size();
    }

    private boolean hasRuleOverrides() {
        return currentRule != null
                && (!currentRule.getAttributes().isEmpty()
                || (currentRule.getDurability() != null && currentRule.getDurability().getDurability() != null));
    }

    private int baseHeaderY(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private int baseRowsTop(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private int changesHeaderY(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private int changesRowsTop(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private int pendingHeaderY(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private int pendingRowsTop(EditorLayout l) {
        return attributesRowsTop(l);
    }

    private void resetRuleToVanilla() {
        if (selectedItem == null) {
            return;
        }

        currentRule = new EditableItemRule(selectedItem.id(), false);
        resetDraft();
        rightScroll = 0;
        formScroll = 0;
        status = "Resetting rule...";
        statusIsError = false;
        statusIsOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(currentRule)));
        markLayoutDirty();
    }

    private void startRegistryDraft() {
        if (selectedItem == null) {
            return;
        }

        ensureCurrentRule();
        selectedModifierIndex = -1;
        selectedBaseAttributeIndex = -1;
        draftSource = DraftSource.REGISTRY;
        action = EditableAttributeAction.ADD;
        operation = EditableOperationType.ADDITION;
        slot = "mainhand";

        attributeBox.setValue("");
        attributeSearchBox.setValue("");
        amountBox.setValue("0");

        openDropdown = OpenDropdown.ATTRIBUTE;
        dropdownScroll = 0;
        setFocused(attributeSearchBox);

        status = "Choose an attribute from the registry.";
        statusIsError = false;
        statusIsOk = false;
        updateWidgets();
    }

    private void selectRegistryAttribute(AttributeOption option) {
        if (option == null) {
            return;
        }

        ensureCurrentRule();
        selectedModifierIndex = -1;
        selectedBaseAttributeIndex = -1;
        draftSource = DraftSource.REGISTRY;

        attributeBox.setValue(option.id().toString());
        attributeSearchBox.setValue(option.id().toString());
        if (amountBox.getValue().isBlank()) {
            amountBox.setValue("0");
        }

        action = EditableAttributeAction.ADD;
        operation = EditableOperationType.ADDITION;
        slot = "mainhand";

        status = "Registry attribute loaded. Edit values and apply.";
        statusIsError = false;
        statusIsOk = false;
        updateWidgets();
    }

    private void applyDraft() {
        if (selectedItem == null) {
            setStatus("Select an item first.", true);
            return;
        }

        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();

        if (hasActiveDraft()) {
            String rawAttribute = attributeBox.getValue().trim();
            if (rawAttribute.isBlank()) {
                setStatus("No attribute selected.", true);
                return;
            }

            ResourceLocation attributeId = ResourceLocation.tryParse(rawAttribute);
            if (attributeId == null) {
                setStatus("Invalid attribute ID format.", true);
                return;
            }

            Double amount = null;
            if (action != EditableAttributeAction.REMOVE) {
                try {
                    amount = Double.parseDouble(amountBox.getValue().trim());
                } catch (NumberFormatException e) {
                    setStatus("Amount must be a number.", true);
                    return;
                }
            }

            EditableAttributeModifier edited = new EditableAttributeModifier(
                    attributeId,
                    action,
                    amount,
                    operation,
                    EditableSlotType.STANDARD,
                    slot
            );

            int targetIndex = selectedModifierIndex;
            if (draftSource == DraftSource.MODIFIER && targetIndex >= 0 && targetIndex < rule.getAttributes().size()) {
                rule.getAttributes().set(targetIndex, edited);
            } else {
                rule.getAttributes().removeIf(existing -> edited.duplicateKey().equalsIgnoreCase(existing.duplicateKey()));
                rule.getAttributes().add(edited);
                targetIndex = rule.getAttributes().size() - 1;
            }

            selectedModifierIndex = clamp(targetIndex, 0, Math.max(0, rule.getAttributes().size() - 1));
            selectedBaseAttributeIndex = -1;
            draftSource = DraftSource.MODIFIER;
        }

        if (!durabilityBox.getValue().isBlank()) {
            try {
                rule.setDurability(new EditableDurabilityModifier(Integer.parseInt(durabilityBox.getValue().trim())));
            } catch (NumberFormatException e) {
                setStatus("Durability must be an integer.", true);
                return;
            }
        } else {
            rule.setDurability(null);
        }

        currentRule = rule.copy();
        status = "Saving...";
        statusIsError = false;
        statusIsOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        updateWidgets();
    }

    private void ensureCurrentRule() {
        if (selectedItem != null && (currentRule == null || !selectedItem.id().equals(currentRule.getTargetId()))) {
            currentRule = new EditableItemRule(selectedItem.id(), false);
        }
    }

    private void consumeNetworkState() {
        String catalogJson = EditorClientState.latestCatalogJson();
        if (!catalogJson.equals(lastCatalogJson)) {
            lastCatalogJson = catalogJson;
            parseCatalog(catalogJson);
            refreshFilter();
            status = "Catalog loaded: " + allItems.size() + " items";
            statusIsError = false;
            statusIsOk = true;
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
                    allItems.add(new CatalogItem(id, descriptionId, translated,
                            item.get("maxDamage").getAsInt(),
                            item.get("damageable").getAsBoolean()));
                }
            }

            JsonArray attributeArray = root.getAsJsonArray("attributes");
            if (attributeArray != null) {
                for (var element : attributeArray) {
                    JsonObject attribute = element.getAsJsonObject();
                    ResourceLocation id = ResourceLocation.tryParse(attribute.get("id").getAsString());
                    if (id == null) {
                        continue;
                    }
                    String descriptionId = attribute.has("descriptionId")
                            ? attribute.get("descriptionId").getAsString()
                            : id.toString();
                    String translated = Component.translatable(descriptionId).getString();
                    String namespace = id.getNamespace();
                    String searchKey = (id + " " + namespace + " " + translated + " " + descriptionId).toLowerCase(Locale.ROOT);
                    attributes.add(new AttributeOption(id, descriptionId, translated, namespace, searchKey));
                }
            }

            attributes.sort(Comparator.comparing(AttributeOption::namespace)
                    .thenComparing(AttributeOption::translatedName)
                    .thenComparing(option -> option.id().toString()));
            attributeFilterDirty = true;
        } catch (RuntimeException e) {
            status = "Catalog parse failed: " + e.getMessage();
            statusIsError = true;
            statusIsOk = false;
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
            ResourceLocation target = ResourceLocation.tryParse(root.get("targetId").getAsString());
            if (selectedItem != null && target != null && !selectedItem.id().equals(target)) {
                return;
            }

            resetDraft();
            externalConflict = root.has("externalConflict") && root.get("externalConflict").getAsBoolean();
            parseBaseAttributes(root);
            currentRule = EditorJsonPayloads.ruleFromPayload(json).orElse(null);
            if (currentRule == null && selectedItem != null) {
                currentRule = new EditableItemRule(selectedItem.id(), false);
            }

            rightScroll = 0;
            formScroll = 0;
            closeDropdown();

            if (currentRule != null && currentRule.getDurability() != null && currentRule.getDurability().getDurability() != null) {
                durabilityBox.setValue(Integer.toString(currentRule.getDurability().getDurability()));
            } else {
                durabilityBox.setValue("");
            }

            status = externalConflict ? "External rule detected for this item." : "Rule loaded.";
            statusIsError = false;
            statusIsOk = !externalConflict;
            updateWidgets();
        } catch (RuntimeException e) {
            status = "Rule parse failed: " + e.getMessage();
            statusIsError = true;
            statusIsOk = false;
        }
    }

    private void parseSaveResult(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean success = root.has("success") && root.get("success").getAsBoolean();
            String message = root.has("message") ? root.get("message").getAsString() : "";
            status = (success ? "Saved. " : "Save failed. ") + message;
            statusIsError = !success;
            statusIsOk = success;
        } catch (RuntimeException e) {
            status = "Save result parse failed: " + e.getMessage();
            statusIsError = true;
            statusIsOk = false;
        }
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
            EditableOperationType op = EditableOperationType.fromString(object.get("operation").getAsString());
            if (attributeId == null || op == null) {
                continue;
            }
            baseAttributes.add(new BaseAttribute(
                    attributeId,
                    object.get("amount").getAsDouble(),
                    op,
                    object.get("slot").getAsString()
            ));
        }
    }

    private void loadBaseAttributeIntoDraft(BaseAttribute attribute) {
        attributeBox.setValue(attribute.attributeId().toString());
        attributeSearchBox.setValue(attribute.attributeId().toString());
        amountBox.setValue(formatAmountForEdit(attribute.amount()));
        action = EditableAttributeAction.MODIFY;
        operation = attribute.operation();
        slot = attribute.slot();
        closeDropdown();
        updateWidgets();
    }

    private void loadModifierIntoDraft(EditableAttributeModifier modifier) {
        attributeBox.setValue(modifier.getAttributeId() == null ? "" : modifier.getAttributeId().toString());
        attributeSearchBox.setValue(modifier.getAttributeId() == null ? "" : modifier.getAttributeId().toString());
        amountBox.setValue(formatAmountForEdit(modifier.getAmount()));
        action = modifier.getAction();
        operation = modifier.getOperation();
        slot = modifier.getSlot() == null || modifier.getSlot().isBlank() ? "mainhand" : modifier.getSlot();
        closeDropdown();
        updateWidgets();
    }

    private void resetDraft() {
        selectedBaseAttributeIndex = -1;
        selectedModifierIndex = -1;
        draftSource = DraftSource.NONE;
        attributeBox.setValue("");
        attributeSearchBox.setValue("");
        amountBox.setValue("");
        action = EditableAttributeAction.MODIFY;
        operation = EditableOperationType.ADDITION;
        slot = "mainhand";
        closeDropdown();
        updateWidgets();
    }

    private void updateWidgets() {
        boolean activeDraft = hasActiveDraft();
        boolean amountEnabled = activeDraft && action != EditableAttributeAction.REMOVE;
        boolean durabilityEnabled = selectedItem != null && selectedItem.damageable();
        boolean applyEnabled = activeDraft || durabilityEnabled;

        if (attributeBox != null) {
            attributeBox.active = activeDraft;
            attributeBox.visible = activeDraft;
        }
        if (amountBox != null) {
            amountBox.active = amountEnabled;
            amountBox.visible = activeDraft;
        }
        if (durabilityBox != null) {
            durabilityBox.active = durabilityEnabled;
            durabilityBox.visible = durabilityEnabled;
        }
        if (attributePickerButton != null) {
            attributePickerButton.active = false;
            attributePickerButton.visible = false;
        }
        if (attributeSearchBox != null) {
            attributeSearchBox.active = activeDraft && openDropdown == OpenDropdown.ATTRIBUTE;
            attributeSearchBox.visible = activeDraft && openDropdown == OpenDropdown.ATTRIBUTE;
        }
        if (applyButton != null) {
            applyButton.active = applyEnabled;
            applyButton.visible = true;
        }
        if (closeButton != null) {
            closeButton.active = true;
            closeButton.visible = true;
        }
        markLayoutDirty();
    }

    private boolean hasActiveDraft() {
        return selectedItem != null && draftSource != DraftSource.NONE;
    }

    private void setStatus(String msg, boolean error) {
        status = msg;
        statusIsError = error;
        statusIsOk = !error;
    }

    private List<AttributeOption> filteredAttributeOptions() {
        String query = attributeSearchBox == null ? "" : attributeSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (!attributeFilterDirty && query.equals(lastAttributeFilterQuery)) {
            return filteredAttributes;
        }

        filteredAttributes.clear();
        for (AttributeOption option : attributes) {
            if (query.isBlank() || option.searchKey().contains(query)) {
                filteredAttributes.add(option);
            }
        }

        // attributes ya se ordena al parsear el catálogo. No volver a ordenar aquí evita trabajo por frame.
        lastAttributeFilterQuery = query;
        attributeFilterDirty = false;
        return filteredAttributes;
    }

    private int selectedAttributeOptionIndex(List<AttributeOption> options) {
        String raw = attributeBox == null ? "" : attributeBox.getValue().trim();
        if (raw.isBlank()) {
            return -1;
        }

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().toString().equalsIgnoreCase(raw)) {
                return i;
            }
        }
        return -1;
    }

    private void toggleDropdown(OpenDropdown target) {
        if (openDropdown == target) {
            closeDropdown();
            return;
        }

        openDropdown = target;
        dropdownScroll = 0;

        if (target == OpenDropdown.ATTRIBUTE && attributeSearchBox != null) {
            attributeSearchBox.setValue(attributeBox == null ? "" : attributeBox.getValue().trim());
            setFocused(attributeSearchBox);
        }

        updateWidgets();
    }

    private void closeDropdown() {
        openDropdown = OpenDropdown.NONE;
        dropdownScroll = 0;
        setFocused(null);

        if (attributeSearchBox != null) {
            attributeSearchBox.visible = false;
            attributeSearchBox.active = false;
        }
        if (attributePickerButton != null) {
            attributePickerButton.visible = false;
            attributePickerButton.active = false;
        }
        markLayoutDirty();
    }

    private int dropdownMenuIndexAt(Rect2i anchor, int itemCount, double mx, double my) {
        DropdownBounds bounds = dropdownBounds(anchor, itemCount);
        if (mx < bounds.x() || mx >= bounds.x() + bounds.w() || my < bounds.y() || my >= bounds.y() + bounds.h()) {
            return -1;
        }

        int visualRow = ((int) my - bounds.y()) / DROPDOWN_ITEM_H;
        int itemIndex = dropdownScroll + visualRow;
        return itemIndex >= 0 && itemIndex < itemCount ? itemIndex : -1;
    }

    private int attributeDropdownMenuIndexAt(Rect2i anchor, int itemCount, double mx, double my) {
        AttributeDropdownBounds bounds = attributeDropdownBounds(anchor, itemCount);
        int listTop = bounds.listY();
        int listBottom = listTop + bounds.listH();

        if (mx < bounds.x() || mx >= bounds.x() + bounds.w() || my < listTop || my >= listBottom) {
            return -1;
        }

        int visualRow = ((int) my - listTop) / ATTRIBUTE_DROPDOWN_ITEM_H;
        int itemIndex = dropdownScroll + visualRow;
        return itemIndex >= 0 && itemIndex < itemCount ? itemIndex : -1;
    }

    private Rect2i dropdownAnchor(OpenDropdown target, EditorLayout l) {
        if (l == null) {
            return null;
        }

        return switch (target) {
            case ATTRIBUTE -> attributeDropdownScreenRect(l);
            case ACTION -> actionDropdownScreenRect(l);
            case OPERATION -> operationDropdownScreenRect(l);
            case SLOT -> slotDropdownScreenRect(l);
            case NONE -> null;
        };
    }

    private int dropdownItemCount(OpenDropdown target) {
        return switch (target) {
            case ATTRIBUTE -> filteredAttributeOptions().size();
            case ACTION -> ACTION_VALUES.length;
            case OPERATION -> OPERATION_VALUES.length;
            case SLOT -> STANDARD_SLOTS.length;
            case NONE -> 0;
        };
    }

    private DropdownBounds dropdownBounds(Rect2i anchor, int itemCount) {
        int visibleRows = Math.min(MAX_DROPDOWN_VISIBLE, itemCount);
        int menuW = anchor.getWidth();
        int menuH = visibleRows * DROPDOWN_ITEM_H;

        int screenBottom = height - GLOBAL_FOOTER_H - 2;
        if (layout != null) {
            Rect2i editor = layout.editorPanel();
            screenBottom = Math.min(screenBottom, editor.getY() + editor.getHeight() - EDITOR_FOOTER_H - 2);
        }

        int screenTop = TOP_BAR_H + 2;
        int belowY = anchor.getY() + anchor.getHeight() + 1;
        int aboveY = anchor.getY() - menuH - 1;

        int menuY;
        if (belowY + menuH <= screenBottom) {
            menuY = belowY;
        } else if (aboveY >= screenTop) {
            menuY = aboveY;
        } else {
            int availableBelow = Math.max(0, screenBottom - belowY);
            int availableAbove = Math.max(0, anchor.getY() - screenTop - 1);
            if (availableBelow >= availableAbove) {
                visibleRows = Math.max(1, availableBelow / DROPDOWN_ITEM_H);
                menuH = visibleRows * DROPDOWN_ITEM_H;
                menuY = belowY;
            } else {
                visibleRows = Math.max(1, availableAbove / DROPDOWN_ITEM_H);
                menuH = visibleRows * DROPDOWN_ITEM_H;
                menuY = anchor.getY() - menuH - 1;
            }
        }

        return new DropdownBounds(anchor.getX(), menuY, menuW, menuH, visibleRows);
    }

    private AttributeDropdownBounds attributeDropdownBounds(Rect2i anchor, int itemCount) {
        int visibleRows = Math.max(1, Math.min(ATTRIBUTE_DROPDOWN_MAX_VISIBLE, itemCount == 0 ? 1 : itemCount));
        int menuW = anchor.getWidth();
        int menuH = ATTRIBUTE_DROPDOWN_TITLE_H + ATTRIBUTE_DROPDOWN_SEARCH_H + 6
                + visibleRows * ATTRIBUTE_DROPDOWN_ITEM_H + 4;

        int screenBottom = height - GLOBAL_FOOTER_H - 2;
        if (layout != null) {
            Rect2i editor = layout.editorPanel();
            screenBottom = Math.min(screenBottom, editor.getY() + editor.getHeight() - EDITOR_FOOTER_H - 2);
        }

        int screenTop = TOP_BAR_H + 2;
        int belowY = anchor.getY() + anchor.getHeight() + 1;
        int aboveY = anchor.getY() - menuH - 1;

        int menuY;
        if (belowY + menuH <= screenBottom) {
            menuY = belowY;
        } else if (aboveY >= screenTop) {
            menuY = aboveY;
        } else {
            int availableBelow = Math.max(0, screenBottom - belowY);
            int availableAbove = Math.max(0, anchor.getY() - screenTop - 1);
            if (availableBelow >= availableAbove) {
                visibleRows = Math.max(1, availableBelow / ATTRIBUTE_DROPDOWN_ITEM_H);
                menuH = ATTRIBUTE_DROPDOWN_TITLE_H + ATTRIBUTE_DROPDOWN_SEARCH_H + 6
                        + visibleRows * ATTRIBUTE_DROPDOWN_ITEM_H + 4;
                menuY = belowY;
            } else {
                visibleRows = Math.max(1, availableAbove / ATTRIBUTE_DROPDOWN_ITEM_H);
                menuH = ATTRIBUTE_DROPDOWN_TITLE_H + ATTRIBUTE_DROPDOWN_SEARCH_H + 6
                        + visibleRows * ATTRIBUTE_DROPDOWN_ITEM_H + 4;
                menuY = anchor.getY() - menuH - 1;
            }
        }

        int searchY = menuY + ATTRIBUTE_DROPDOWN_TITLE_H + 3;
        int listY = searchY + ATTRIBUTE_DROPDOWN_SEARCH_H + 5;
        int listH = visibleRows * ATTRIBUTE_DROPDOWN_ITEM_H;
        return new AttributeDropdownBounds(anchor.getX(), menuY, menuW, menuH, visibleRows, searchY, listY, listH);
    }

    private void drawCleanRowBackground(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hover, int index) {
        g.fill(x, y, x + w, y + h, selected ? SEL_BG : (index % 2 == 0 ? PANEL_DEEP : PANEL));
        if (hover && !selected) {
            g.fill(x, y, x + w, y + h, HOVER_TINT);
        }
        if (selected) {
            g.fill(x, y, x + 3, y + h, ACCENT);
        }
    }

    private void drawBadge(GuiGraphics g, int x, int y, int w, int h, String text, boolean selected) {
        int border = selected ? ACCENT : BORDER;
        int textColor = selected ? ACCENT : TEXT_DIM;
        g.fill(x, y, x + w, y + h, INPUT_BG);
        drawBorder(g, x, y, w, h, border);
        g.drawString(font, text, x + Math.max(2, (w - font.width(text)) / 2), centeredTextY(y, h), textColor, false);
    }

    private String attributeDisplayName(ResourceLocation id) {
        if (id == null) {
            return "Unknown attribute";
        }
        for (AttributeOption option : attributes) {
            if (option.id().equals(id) && option.translatedName() != null && !option.translatedName().isBlank()) {
                String translated = option.translatedName();
                if (!translated.equals(option.descriptionId()) && !translated.equals(id.toString())) {
                    return translated;
                }
            }
        }
        return titleCaseAttributePath(id.getPath());
    }

    private String titleCaseAttributePath(String path) {
        String cleaned = path == null ? "" : path;
        cleaned = cleaned.replace("generic.", "")
                .replace("player.", "")
                .replace("zombie.", "")
                .replace("horse.", "")
                .replace('.', ' ')
                .replace('_', ' ')
                .trim();
        if (cleaned.isEmpty()) {
            return "Attribute";
        }

        StringBuilder out = new StringBuilder(cleaned.length());
        boolean upper = true;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
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

    private String actionBadge(EditableAttributeAction value) {
        return switch (value) {
            case ADD -> "ADD";
            case MODIFY -> "EDIT";
            case REMOVE -> "DEL";
        };
    }

    private String effectText(Double amount, EditableOperationType op, EditableAttributeAction action) {
        if (action == EditableAttributeAction.REMOVE) {
            return "removed";
        }
        if (amount == null) {
            return "—";
        }

        EditableOperationType safeOp = op == null ? EditableOperationType.ADDITION : op;
        return switch (safeOp) {
            case ADDITION -> signedAmount(amount) + " flat";
            case MULTIPLY_BASE -> signedPercent(amount) + " base";
            case MULTIPLY_TOTAL -> signedPercent(amount) + " total";
        };
    }

    private String signedAmount(double value) {
        String raw = formatAmountForEdit(value);
        return value > 0 ? "+" + raw : raw;
    }

    private String signedPercent(double value) {
        double pct = value * 100.0D;
        String raw = formatAmountForEdit(pct);
        return pct > 0 ? "+" + raw + "%" : raw + "%";
    }

    private String actionDisplay(EditableAttributeAction value) {
        return switch (value) {
            case ADD -> "Add";
            case MODIFY -> "Modify";
            case REMOVE -> "Remove Attribute";
        };
    }

    private String operationDisplay(EditableOperationType value) {
        return switch (value) {
            case ADDITION -> "Addition";
            case MULTIPLY_BASE -> "Multiply Base";
            case MULTIPLY_TOTAL -> "Multiply Total";
        };
    }

    private int indexOfAction(EditableAttributeAction value) {
        for (int i = 0; i < ACTION_VALUES.length; i++) {
            if (ACTION_VALUES[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfOperation(EditableOperationType value) {
        for (int i = 0; i < OPERATION_VALUES.length; i++) {
            if (OPERATION_VALUES[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private int indexOfSlot(String value) {
        for (int i = 0; i < STANDARD_SLOTS.length; i++) {
            if (STANDARD_SLOTS[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private Rect2i actionDropdownLogicalRect(EditorLayout l) {
        return new Rect2i(l.formX(), l.actionY(), actionColW(l.formW()), BUTTON_H);
    }

    private Rect2i operationDropdownLogicalRect(EditorLayout l) {
        int actionW = actionColW(l.formW());
        return new Rect2i(l.formX() + actionW + 6, l.actionY(), l.formW() - actionW - 6, BUTTON_H);
    }

    private Rect2i slotDropdownLogicalRect(EditorLayout l) {
        int amountW = amountColW(l.formW());
        return new Rect2i(l.formX() + amountW + 6, l.amountY(), slotColW(l.formW()), BUTTON_H);
    }

    private Rect2i actionDropdownScreenRect(EditorLayout l) {
        return toFormScreenRect(actionDropdownLogicalRect(l));
    }

    private Rect2i operationDropdownScreenRect(EditorLayout l) {
        return toFormScreenRect(operationDropdownLogicalRect(l));
    }

    private Rect2i slotDropdownScreenRect(EditorLayout l) {
        return toFormScreenRect(slotDropdownLogicalRect(l));
    }

    private Rect2i toFormScreenRect(Rect2i rect) {
        return new Rect2i(rect.getX(), rect.getY() - formScroll, rect.getWidth(), rect.getHeight());
    }

    private Rect2i attributeDropdownScreenRect(EditorLayout l) {
        return new Rect2i(l.formX(), l.attributeY() - formScroll, l.formW(), BUTTON_H);
    }

    private boolean isNumericInput(String value) {
        return value.isBlank() || value.matches("-?\\d*(\\.\\d*)?");
    }

    private boolean isIntegerInput(String value) {
        return value.isBlank() || value.matches("\\d*");
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

    private String formatAmountForEdit(Double value) {
        if (value == null) {
            return "";
        }
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }

        BigDecimal raw = BigDecimal.valueOf(value).stripTrailingZeros();
        String rawText = raw.toPlainString();
        if (rawText.length() <= 12) {
            return rawText;
        }

        BigDecimal rounded = BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.toPlainString();
    }

    private String formatAmountForList(Double value) {
        return value == null ? "-" : formatAmountForEdit(value);
    }

    private int measureRightContentHeight() {
        int h = 6 + SECTION_HEADER_H + 2;
        int rowCount = visibleAttributeRows().size();

        if (rowCount == 0) {
            h += 18;
        } else {
            h += rowCount * 20;
        }

        if (selectedItem != null) {
            h += SECTION_HEADER_H + 2 + font.lineHeight + 6 + CONTROL_H + 6;
        }
        if (externalConflict) {
            h += 28;
        }
        return h + 12;
    }

    private List<VisibleAttributeRow> visibleAttributeRows() {
        List<VisibleAttributeRow> rows = new ArrayList<>();
        if (baseAttributes.isEmpty() && (currentRule == null || currentRule.getAttributes().isEmpty())) {
            return rows;
        }

        Map<String, VisibleAttributeRow> effective = new LinkedHashMap<>();
        for (int i = 0; i < baseAttributes.size(); i++) {
            BaseAttribute base = baseAttributes.get(i);
            String key = visibleAttributeKey(base.attributeId(), base.operation(), EditableSlotType.STANDARD, base.slot())
                    .toLowerCase(Locale.ROOT);
            effective.put(key, new VisibleAttributeRow(i, -1, base, null));
        }

        if (currentRule != null) {
            for (int i = 0; i < currentRule.getAttributes().size(); i++) {
                EditableAttributeModifier modifier = currentRule.getAttributes().get(i);
                if (modifier == null || modifier.getAttributeId() == null) {
                    continue;
                }

                String key = visibleAttributeKey(modifier.getAttributeId(), modifier.getOperation(), modifier.getSlotType(), modifier.getSlot())
                        .toLowerCase(Locale.ROOT);
                if (modifier.getAction() == EditableAttributeAction.REMOVE) {
                    effective.remove(key);
                    continue;
                }

                VisibleAttributeRow existing = effective.get(key);
                if (existing != null && existing.baseAttribute() != null) {
                    effective.put(key, new VisibleAttributeRow(existing.baseIndex(), i, existing.baseAttribute(), modifier));
                } else {
                    effective.put(key, new VisibleAttributeRow(-1, i, null, modifier));
                }
            }
        }

        rows.addAll(effective.values());
        return rows;
    }

    private String visibleAttributeKey(ResourceLocation attributeId, EditableOperationType operation, EditableSlotType slotType, String slot) {
        return Objects.toString(attributeId, "") + "|" + Objects.toString(operation, "") + "|"
                + Objects.toString(slotType, "") + "|" + Objects.toString(slot, "");
    }

    private int actionColW(int formW) {
        return Math.max(78, Math.min(126, (formW - 6) / 2));
    }

    private int amountColW(int formW) {
        return Math.max(96, Math.min(138, (formW - 6) / 3));
    }

    private int slotColW(int formW) {
        return Math.max(84, formW - amountColW(formW) - 6);
    }

    private boolean containsArea(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean contains(Rect2i r, double mx, double my) {
        return mx >= r.getX() && mx < r.getX() + r.getWidth()
                && my >= r.getY() && my < r.getY() + r.getHeight();
    }

    private boolean isRectVisibleInForm(Rect2i rect, EditorLayout l) {
        int top = l.contentTop();
        int bottom = formViewportBottom(l);
        return rect.getY() + rect.getHeight() > top && rect.getY() < bottom;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
