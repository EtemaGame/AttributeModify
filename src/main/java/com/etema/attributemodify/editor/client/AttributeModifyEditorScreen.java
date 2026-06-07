package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.editor.EditorClientState;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableDurabilityModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableMiningOverride;
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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.util.Optional;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pantalla principal del editor de atributos.
 *
 * Layout:
 *   [Lista ítems] | [Panel editor: header + lista atributos + footer]
 *                                              ↑
 *                               Drawer lateral (aparece al Modify/+Add)
 *
 * La lista de atributos muestra cada fila como:
 *   [valor]  [Nombre Atributo]               [Modify] [Delete]
 *
 * El drawer de edición expone solo tres campos al usuario:
 *   Atributo  → dropdown del registry
 *   Valor     → campo numérico (acepta "10" o "10%")
 *   Tipo      → Exacto / Porcentual
 */
public final class AttributeModifyEditorScreen extends Screen {

    // ── Paleta de colores ──────────────────────────────────────────────────
    private static final int BG           = 0xFF090D12;
    private static final int PANEL        = 0xF211171D;
    private static final int PANEL_ALT    = 0xEE151D25;
    private static final int PANEL_DEEP   = 0xFF0B1015;
    private static final int PANEL_DIM    = 0xFF0C1014;
    private static final int BORDER       = 0xFF2A3542;
    private static final int BORDER_SOFT  = 0xFF1D2630;
    private static final int SEL_BG       = 0xFF163D30;
    private static final int HEADER_BG    = 0xFF0C1218;
    private static final int INPUT_BG     = 0xFF070B0F;
    private static final int DRAWER_BG    = 0xFF0D1520;
    private static final int DRAWER_BRD   = 0xFF45D59B;
    private static final int TEXT         = 0xFFE1EDF5;
    private static final int TEXT_DIM     = 0xFF8EACBE;
    private static final int TEXT_MUTED   = 0xFF5F7889;
    private static final int ACCENT       = 0xFF45D59B;
    private static final int ACCENT2      = 0xFF3BA87D;
    private static final int WARN         = 0xFFFFB74D;
    private static final int ERROR        = 0xFFFF6B6B;
    private static final int HOVER_TINT   = 0x1FFFFFFF;

    // ── Medidas ────────────────────────────────────────────────────────────
    private static final int ROW_H            = 22;
    private static final int SEARCH_H         = 22;
    private static final int PANEL_HEADER_H   = LayoutEngine.PANEL_HEADER_H;
    private static final int CONTROL_H        = 17;
    private static final int BUTTON_H         = 15;
    private static final int SCROLL_STEP      = 12;
    private static final int DROPDOWN_ITEM_H  = 16;
    private static final int ATTR_DD_ITEM_H   = 20;
    private static final int ATTR_DD_TITLE_H  = 14;
    private static final int ATTR_DD_SEARCH_H = 18;
    private static final int MAX_DD_VISIBLE   = 4;
    private static final int MAX_ATTR_VISIBLE = 6;

    // ── Datos del catálogo ─────────────────────────────────────────────────
    private final List<CatalogItem>    allItems         = new ArrayList<>();
    private final List<CatalogItem>    filteredItems    = new ArrayList<>();
    private final List<AttributeOption> attributes      = new ArrayList<>();
    private final List<AttributeOption> filteredAttrs   = new ArrayList<>();
    private final List<BaseAttribute>  baseAttributes   = new ArrayList<>();
    private final List<String> miningTiers              = new ArrayList<>();

    // ── Estado de UI ───────────────────────────────────────────────────────
    private CatalogItem      selectedItem;
    private EditableItemRule currentRule;
    private final EditorDraftState     draft     = new EditorDraftState();
    private final DropdownController   dropdown  = new DropdownController();

    private List<VisibleRow> rowsCache  = List.of();
    private boolean          rowsDirty  = true;

    private int  itemScroll;
    private int  attrScroll;
    private int  drawerScroll;

    private EditorLayout layout;
    private boolean      layoutDirty = true;
    private boolean      drawerOpen  = false;
    private int          selectedMiningIndex = -1;

    // ── Widgets ────────────────────────────────────────────────────────────
    private EditBox searchBox;          // buscar ítems
    private EditBox drawerAttrBox;      // atributo en el drawer (texto)
    private EditBox drawerAttrSearch;   // búsqueda dentro del dropdown de atributos
    private EditBox drawerValueBox;     // valor numérico
    private EditBox drawerMiningTierBox;
    private Button  drawerApplyBtn;
    private Button  drawerCancelBtn;
    private Button  drawerTypeExact;    // botón selector tipo "Exacto"
    private Button  drawerTypePercent;  // botón selector tipo "Porcentual"
    private boolean attrSearchResetPending;
    private boolean drawerValueResetPending;

    // ── Red / estado ───────────────────────────────────────────────────────
    private String lastCatalogJson = "";
    private String lastRuleJson    = "";
    private String lastSaveJson    = "";
    private String status          = "Connecting...";
    private boolean statusError;
    private boolean statusOk;
    private boolean externalConflict;
    private String  attrFilterQuery   = "";
    private boolean attrFilterDirty   = true;

    // ── Registros internos ─────────────────────────────────────────────────
    private record CatalogItem(ResourceLocation id, String descriptionId, String translatedName,
                                int maxDamage, boolean damageable) {}

    private record BaseAttribute(ResourceLocation attributeId, double amount,
                                  EditableOperationType operation, String slot) {}

    /**
     * Fila visible en la lista de atributos.
     * Puede representar un atributo base (vanilla) o un modifier aplicado.
     */
    private record VisibleRow(int baseIndex, int modIndex,
                               BaseAttribute base, EditableAttributeModifier modifier) {}

    private record AttributeOption(ResourceLocation id, String descriptionId,
                                    String translatedName, String namespace, String searchKey) {}

    // ══════════════════════════════════════════════════════════════════════
    //  Inicialización
    // ══════════════════════════════════════════════════════════════════════

    public AttributeModifyEditorScreen() {
        super(Component.literal("Attribute Modify Editor"));
    }

    @Override
    protected void init() {
        rebuildWidgets();
        EditorNetwork.INSTANCE.sendToServer(new C2SRequestEditorCatalogPacket());
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        // ── Búsqueda de ítems ──────────────────────────────────────────────
        searchBox = new EditBox(font, 0, 0, 10, 14, Component.empty());
        searchBox.setHint(Component.literal("Search item..."));
        searchBox.setBordered(false);
        searchBox.setResponder(v -> { itemScroll = 0; refreshItemFilter(); });
        addRenderableWidget(searchBox);

        // ── Drawer: almacén del atributo seleccionado (no se muestra como widget)
        // Su display es pintado manualmente en renderDrawerBackground como pseudo-botón.
        drawerAttrBox = new EditBox(font, 0, 0, 10, 14, Component.empty());
        drawerAttrBox.setBordered(false);
        drawerAttrBox.setVisible(false);  // siempre invisible; valor almacenado aquí
        drawerAttrBox.active = false;
        addRenderableWidget(drawerAttrBox);

        // ── Drawer: búsqueda en el dropdown de atributos ───────────────────
        drawerAttrSearch = new EditBox(font, 0, 0, 10, 14, Component.empty());
        drawerAttrSearch.setHint(Component.literal("Search attribute / mod..."));
        drawerAttrSearch.setBordered(false);
        drawerAttrSearch.setVisible(false);
        drawerAttrSearch.setResponder(v -> { dropdown.dropdownScroll(0); attrFilterDirty = true; });
        addRenderableWidget(drawerAttrSearch);

        // ── Drawer: valor ──────────────────────────────────────────────────
        drawerValueBox = new EditBox(font, 0, 0, 10, CONTROL_H, Component.empty());
        drawerValueBox.setHint(Component.literal("0"));
        drawerValueBox.setBordered(false);   // nosotros pintamos el fondo
        drawerValueBox.setFilter(this::isAmountInput);
        drawerValueBox.setVisible(false);
        drawerValueBox.setTextColor(0xFFE1EDF5);
        drawerValueBox.setTextColorUneditable(0xFF5F7889);
        addRenderableWidget(drawerValueBox);

        drawerMiningTierBox = new EditBox(font, 0, 0, 10, CONTROL_H, Component.empty());
        drawerMiningTierBox.setHint(Component.literal("Select tier"));
        drawerMiningTierBox.setBordered(false);
        drawerMiningTierBox.setVisible(false);
        drawerMiningTierBox.active = false;
        drawerMiningTierBox.setTextColor(0xFFE1EDF5);
        drawerMiningTierBox.setTextColorUneditable(0xFF5F7889);
        addRenderableWidget(drawerMiningTierBox);

        // ── Drawer: botones tipo ───────────────────────────────────────────
        drawerTypeExact = addRenderableWidget(
            Button.builder(Component.literal("Exacto"), b -> setDraftType(false))
                  .bounds(0, 0, 80, BUTTON_H).build());
        drawerTypeExact.visible = false;

        drawerTypePercent = addRenderableWidget(
            Button.builder(Component.literal("Porcentual"), b -> setDraftType(true))
                  .bounds(0, 0, 80, BUTTON_H).build());
        drawerTypePercent.visible = false;

        // ── Drawer: Apply / Cancel ─────────────────────────────────────────
        drawerApplyBtn = addRenderableWidget(
            Button.builder(Component.literal("Apply"), b -> applyDraft())
                  .bounds(0, 0, 60, BUTTON_H).build());
        drawerApplyBtn.visible = false;

        drawerCancelBtn = addRenderableWidget(
            Button.builder(Component.literal("Cancel"), b -> closeDrawer())
                  .bounds(0, 0, 60, BUTTON_H).build());
        drawerCancelBtn.visible = false;

        layoutDirty = true;
        applyLayoutNow();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tick / Render
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        if (searchBox      != null) searchBox.tick();
        if (drawerAttrBox  != null) drawerAttrBox.tick();
        if (drawerAttrSearch != null) drawerAttrSearch.tick();
        if (drawerValueBox != null) drawerValueBox.tick();
        if (drawerMiningTierBox != null) drawerMiningTierBox.tick();
        consumeNetworkState();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        applyLayoutNow();

        g.fill(0, 0, width, height, BG);
        renderTitleBar(g);
        renderListPanel(g, mx, my);
        renderEditorPanel(g, mx, my);
        renderStatusBar(g);

        // Drawer background + field labels BEFORE super.render(),
        // so Minecraft EditBox widgets appear on top of our painted backgrounds.
        if (drawerOpen) {
            renderDrawerBackground(g);
        }

        super.render(g, mx, my, pt);   // Minecraft widgets (EditBox, Button)

        // Dropdown overlay goes on top of everything including widgets
        renderDropdownOverlay(g, mx, my, pt);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — barra de título
    // ══════════════════════════════════════════════════════════════════════

    private void renderTitleBar(GuiGraphics g) {
        g.fill(0, 0, width, LayoutEngine.TOP_BAR_H, HEADER_BG);
        g.fill(0, 0, width, 1, BORDER_SOFT);
        g.fill(0, LayoutEngine.TOP_BAR_H - 1, width, LayoutEngine.TOP_BAR_H, BORDER_SOFT);
        g.fill(0, 0, 3, LayoutEngine.TOP_BAR_H, ACCENT);

        String title = "Attribute Modify Editor";
        int ty = centeredTextY(0, LayoutEngine.TOP_BAR_H);
        g.drawString(font, title, 9, ty, ACCENT, false);

        int tx = 9 + font.width(title);
        if (selectedItem != null) {
            String crumb = "  >  " + selectedItem.translatedName();
            g.drawString(font, truncatePx(crumb, Math.max(40, width - tx - 120)), tx, ty, TEXT_DIM, false);
        }

        String meta = allItems.size() + " items  |  " + attributes.size() + " attrs";
        int metaX = width - font.width(meta) - 10;
        if (metaX > tx + 16) g.drawString(font, meta, metaX, ty, TEXT_MUTED, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — panel lista de ítems
    // ══════════════════════════════════════════════════════════════════════

    private void renderListPanel(GuiGraphics g, int mx, int my) {
        Rect2i b = layout.listPanel();
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), PANEL);
        drawBorder(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), BORDER);

        // Cabecera
        int headerBot = b.getY() + PANEL_HEADER_H;
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), headerBot, HEADER_BG);
        g.fill(b.getX(), headerBot, b.getX() + b.getWidth(), headerBot + 1, BORDER_SOFT);
        g.drawString(font, "ITEM CATALOG", b.getX() + 8, centeredTextY(b.getY(), PANEL_HEADER_H), TEXT_DIM, false);
        String total = Integer.toString(allItems.size());
        g.drawString(font, total, b.getX() + b.getWidth() - font.width(total) - 8,
                     centeredTextY(b.getY(), PANEL_HEADER_H), TEXT_MUTED, false);

        // Caja de búsqueda
        int searchTop = headerBot + 1;
        int searchBot = searchTop + SEARCH_H;
        g.fill(b.getX() + 6, searchTop + 3, b.getX() + b.getWidth() - 6, searchBot - 3, INPUT_BG);
        drawBorder(g, b.getX() + 6, searchTop + 3, b.getWidth() - 12, SEARCH_H - 6, BORDER_SOFT);

        // Lista
        int listTop = searchBot + 1;
        int listBot = b.getY() + b.getHeight() - 18;
        int visible = Math.max(1, (listBot - listTop) / ROW_H);
        itemScroll = clamp(itemScroll, 0, Math.max(0, filteredItems.size() - visible));

        g.enableScissor(b.getX() + 1, listTop, b.getX() + b.getWidth() - 1, listBot);
        for (int i = itemScroll; i < Math.min(filteredItems.size(), itemScroll + visible); i++) {
            int ry = listTop + (i - itemScroll) * ROW_H;
            CatalogItem item = filteredItems.get(i);
            boolean sel   = selectedItem != null && selectedItem.id().equals(item.id());
            boolean hover = inRect(mx, my, b.getX(), ry, b.getWidth(), ROW_H);

            g.fill(b.getX() + 1, ry, b.getX() + b.getWidth() - 1, ry + ROW_H,
                   sel ? SEL_BG : (i % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !sel) g.fill(b.getX() + 1, ry, b.getX() + b.getWidth() - 1, ry + ROW_H, HOVER_TINT);
            if (sel) g.fill(b.getX() + 1, ry, b.getX() + 4, ry + ROW_H, ACCENT);

            Item reg = ForgeRegistries.ITEMS.getValue(item.id());
            if (reg != null) g.renderItem(new ItemStack(reg), b.getX() + 7, ry + 3);

            int tx = b.getX() + 28;
            int tw = Math.max(8, b.getWidth() - 36);
            g.drawString(font, truncatePx(item.translatedName(), tw), tx, ry + 3,  sel ? ACCENT : TEXT, false);
            g.drawString(font, truncatePx(item.id().toString(),   tw), tx, ry + 13, TEXT_DIM, false);
        }
        g.disableScissor();

        // Scrollbar
        if (filteredItems.size() > visible) {
            drawScrollbar(g, b.getX() + b.getWidth() - 4, listTop + 2, listBot - 2,
                          itemScroll, filteredItems.size() - visible, filteredItems.size(), visible);
        }

        // Footer con conteo
        g.fill(b.getX(), listBot, b.getX() + b.getWidth(), b.getY() + b.getHeight(), HEADER_BG);
        g.fill(b.getX(), listBot - 1, b.getX() + b.getWidth(), listBot, BORDER_SOFT);
        String count = filteredItems.size() + " / " + allItems.size();
        g.drawString(font, count, b.getX() + b.getWidth() - font.width(count) - 7, listBot + 5, TEXT_DIM, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — panel editor principal
    // ══════════════════════════════════════════════════════════════════════

    private void renderEditorPanel(GuiGraphics g, int mx, int my) {
        Rect2i b = layout.editorPanel();
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), PANEL);
        drawBorder(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), BORDER);

        // Cabecera del panel
        g.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + PANEL_HEADER_H, HEADER_BG);
        g.fill(b.getX(), b.getY() + PANEL_HEADER_H, b.getX() + b.getWidth(), b.getY() + PANEL_HEADER_H + 1, BORDER_SOFT);

        if (selectedItem == null) {
            String hint = "Select an item from the list";
            g.drawString(font, hint,
                         b.getX() + (b.getWidth() - font.width(hint)) / 2,
                         b.getY() + b.getHeight() / 2 - font.lineHeight / 2,
                         TEXT_DIM, false);
            return;
        }

        // Header del ítem seleccionado
        renderItemHeader(g, b);

        // Zona de atributos con scroll
        int aTop  = layout.contentTop();
        int aBot  = layout.contentBottom() - LayoutEngine.EDITOR_FOOTER_H;
        int contentH = measureAttrContentHeight();
        int maxScroll = Math.max(0, contentH - (aBot - aTop));
        attrScroll = clamp(attrScroll, 0, maxScroll);

        g.enableScissor(layout.attrListX(), aTop, layout.attrListX() + layout.attrListW(), aBot);
        g.pose().pushPose();
        g.pose().translate(0, -attrScroll, 0);
        renderAttributeList(g, mx, my + attrScroll);
        g.pose().popPose();
        g.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(g, layout.attrListX() + layout.attrListW() - 3,
                          aTop + 2, aBot - 2, attrScroll, maxScroll, contentH, aBot - aTop);
        }

        // Footer
        int footerY = b.getY() + b.getHeight() - LayoutEngine.EDITOR_FOOTER_H;
        g.fill(b.getX(), footerY, b.getX() + b.getWidth(), b.getY() + b.getHeight(), HEADER_BG);
        g.fill(b.getX(), footerY - 1, b.getX() + b.getWidth(), footerY, BORDER_SOFT);
        String tip = drawerOpen ? "Edit the value in the panel ->" : "Click an entry to modify it, or use + Add";
        g.drawString(font, tip, b.getX() + 8, centeredTextY(footerY, LayoutEngine.EDITOR_FOOTER_H), ACCENT2, false);
    }

    // ── Cabecera del ítem ─────────────────────────────────────────────────

    private void renderItemHeader(GuiGraphics g, Rect2i b) {
        Item reg = ForgeRegistries.ITEMS.getValue(selectedItem.id());
        if (reg != null) g.renderItem(new ItemStack(reg), b.getX() + 8, b.getY() + 6);

        int titleX = b.getX() + 30;
        int reserve = externalConflict ? 120 : 66;  // espacio para "RESET"
        int titleW = Math.max(40, b.getWidth() - 30 - reserve);
        g.drawString(font, truncatePx(selectedItem.translatedName(), titleW), titleX, b.getY() + 5,  TEXT, false);
        g.drawString(font, truncatePx(selectedItem.id().toString(),   titleW), titleX, b.getY() + 15, TEXT_DIM, false);

        if (externalConflict) {
            g.drawString(font, "External conflict",
                         b.getX() + b.getWidth() - font.width("External conflict") - 66,
                         centeredTextY(b.getY(), PANEL_HEADER_H), WARN, false);
        }

        // Botón RESET (visible solo si hay overrides)
        boolean canReset = hasRuleOverrides();
        Rect2i resetRect = resetButtonRect(b);
        drawSmallButton(g, resetRect, "RESET", canReset, false);
    }

    private Rect2i resetButtonRect(Rect2i b) {
        return new Rect2i(b.getX() + b.getWidth() - 56, b.getY() + 7, 50, 14);
    }

    // ── Lista de atributos ────────────────────────────────────────────────

    /**
     * Dibuja la lista de atributos con el formato:
     *   [valor]  [Nombre]                    [Modify] [Delete]
     */
    private void renderAttributeList(GuiGraphics g, int mx, int logicalMy) {
        int x  = layout.attrListX();
        int w  = layout.attrListW();
        int cy = layout.contentTop() + 6;

        // Cabecera de sección con botón "+ Add"
        cy = renderSectionHeader(g, x, cy, w, "ATTRIBUTES");
        Rect2i addRect = addButtonRect(x, cy - (14 + 2 + 2), w);   // sobre la última línea del header
        drawSmallButton(g, addRect, "+ Add", true, false);

        List<VisibleRow> rows = visibleRows();
        if (rows.isEmpty()) {
            g.drawString(font, "No attributes. Use + Add to create one.", x + 8, cy + 4, TEXT_DIM, false);
            cy += 24;
        } else {
            for (int i = 0; i < rows.size(); i++) {
                VisibleRow row = rows.get(i);
                int ry = cy + i * 22;
                renderAttributeRow(g, x, ry, w, row, i, mx, logicalMy);
            }
            cy += rows.size() * 22 + 12;
        }

        cy = renderDurabilitySection(g, x, cy, w, mx, logicalMy);
        renderMiningSection(g, x, cy, w, mx, logicalMy);
    }

    private void renderAttributeRow(GuiGraphics g, int x, int ry, int w,
                                     VisibleRow row, int rowIndex, int mx, int logicalMy) {
        boolean selected = isRowSelected(row);
        boolean hover    = inRect(mx, logicalMy, x + 4, ry, w - 8, 20);

        // Fondo
        g.fill(x + 4, ry, x + w - 4, ry + 20, selected ? SEL_BG : (rowIndex % 2 == 0 ? PANEL_DEEP : PANEL_ALT));
        if (hover && !selected) g.fill(x + 4, ry, x + w - 4, ry + 20, HOVER_TINT);
        if (selected)           g.fill(x + 4, ry, x + 7,     ry + 20, ACCENT);

        // Valor (izquierda, ~50 px)
        String valueStr = rowValueText(row);
        int valueColor  = selected ? ACCENT : TEXT_DIM;
        g.drawString(font, valueStr, x + 10, centeredTextY(ry, 20), valueColor, false);

        // Nombre (centro)
        String name  = rowNameText(row);
        int nameX    = x + 10 + font.width(valueStr) + 6;
        int nameW    = Math.max(20, w - (nameX - x) - 88);
        int nameColor = selected ? ACCENT : TEXT;
        g.drawString(font, truncatePx(name, nameW), nameX, centeredTextY(ry, 20), nameColor, false);

        // Botones Modify / Delete (derecha)
        Rect2i modRect = modifyButtonRect(x, ry, w);
        Rect2i delRect = deleteButtonRect(x, ry, w);
        boolean hoverMod = inRect(mx, logicalMy, modRect);
        boolean hoverDel = inRect(mx, logicalMy, delRect);
        drawSmallButton(g, modRect, "Modify", true, hoverMod);
        drawDeleteButton(g, delRect, hoverDel);
    }

    private Rect2i modifyButtonRect(int x, int ry, int w) {
        return new Rect2i(x + w - 76, ry + 3, 46, 14);
    }

    private Rect2i deleteButtonRect(int x, int ry, int w) {
        return new Rect2i(x + w - 26, ry + 3, 22, 14);
    }

    private Rect2i addButtonRect(int x, int headerY, int w) {
        return new Rect2i(x + w - 50, headerY + 2, 46, 14);
    }

    // ── Helpers de texto para filas ───────────────────────────────────────

    private String rowValueText(VisibleRow row) {
        if (row.modifier() != null) {
            EditableAttributeModifier m = row.modifier();
            if (m.getAction() == EditableAttributeAction.REMOVE) return "removed";
            return effectDisplay(m.getAmount(), m.getOperation(), m.getAction());
        }
        if (row.base() != null) {
            return effectDisplay(row.base().amount(), row.base().operation(), EditableAttributeAction.MODIFY);
        }
        return "—";
    }

    private String rowNameText(VisibleRow row) {
        ResourceLocation id = row.modifier() != null ? row.modifier().getAttributeId()
                                                     : (row.base() != null ? row.base().attributeId() : null);
        return id == null ? "Unknown attribute" : attrDisplayName(id);
    }

    private boolean isRowSelected(VisibleRow row) {
        if (row.modifier() != null)
            return row.modIndex() == draft.selectedModifierIndex() && draft.draftSource() == DraftSource.MODIFIER;
        return row.baseIndex() == draft.selectedBaseAttributeIndex() && draft.draftSource() == DraftSource.BASE;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — Drawer lateral de edición
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fase 1 del drawer: pintar fondo, etiquetas de campos e inputs background.
     * Se llama ANTES de super.render() para que los EditBox de Minecraft queden
     * por encima del fondo que pintamos.
     */
    private void renderDrawerBackground(GuiGraphics g) {
        if (layout == null || !layout.drawerOpen()) return;

        int dx = layout.drawerX(), dy = layout.drawerY();
        int dw = layout.drawerW(), dh = layout.drawerH();

        // Sombra lateral izquierda
        g.fill(dx - 4, dy, dx, dy + dh, 0x55000000);

        // Fondo principal del drawer
        g.fill(dx, dy, dx + dw, dy + dh, DRAWER_BG);
        drawBorder(g, dx, dy, dw, dh, DRAWER_BRD);
        g.fill(dx, dy, dx + 2, dy + dh, DRAWER_BRD);   // franja izquierda de acento

        // Cabecera
        g.fill(dx, dy, dx + dw, dy + PANEL_HEADER_H, HEADER_BG);
        g.fill(dx, dy + PANEL_HEADER_H, dx + dw, dy + PANEL_HEADER_H + 1, BORDER_SOFT);
        boolean durabilityDraft = draft.draftSource() == DraftSource.DURABILITY;
        boolean miningDraft = draft.draftSource() == DraftSource.MINING;
        String drawerTitle = durabilityDraft ? "Modify durability"
                : (miningDraft ? "Modify mining"
                : (draft.draftSource() == DraftSource.REGISTRY ? "Add attribute" : "Modify attribute"));
        g.drawString(font, drawerTitle, dx + 10, centeredTextY(dy, PANEL_HEADER_H), ACCENT, false);

        int pad = LayoutEngine.DRAWER_PADDING;
        int fw  = dw - pad * 2;

        if (durabilityDraft) {
            int fy = layout.drawerFieldValueY();
            drawFieldLabel(g, "Durability", dx + pad, fy);
            int valueBorderColor = (drawerValueBox != null && drawerValueBox.isFocused()) ? ACCENT : BORDER;
            g.fill(dx + pad, fy, dx + pad + fw, fy + CONTROL_H, INPUT_BG);
            drawBorder(g, dx + pad, fy, fw, CONTROL_H, valueBorderColor);
            String helper = "Writes top-level durability for this item.";
            g.drawString(font, truncatePx(helper, fw), dx + pad, fy + CONTROL_H + 7, TEXT_DIM, false);
            return;
        }

        if (miningDraft) {
            int fy = layout.drawerFieldValueY();
            drawFieldLabel(g, "Mining speed", dx + pad, fy);
            int speedBorder = (drawerValueBox != null && drawerValueBox.isFocused()) ? ACCENT : BORDER;
            g.fill(dx + pad, fy, dx + pad + fw, fy + CONTROL_H, INPUT_BG);
            drawBorder(g, dx + pad, fy, fw, CONTROL_H, speedBorder);

            fy = layout.drawerFieldTypeY();
            drawFieldLabel(g, "Mining tier", dx + pad, fy);
            boolean tierDDOpen = dropdown.openDropdown() == OpenDropdown.MINING_TIER;
            String tierValue = drawerMiningTierBox == null ? "" : drawerMiningTierBox.getValue().trim();
            String tierLabel = tierValue.isBlank()
                    ? (miningTiers.isEmpty() ? "No mining tiers detected" : "Select tier")
                    : tierValue;
            int tierBorder = tierDDOpen ? ACCENT : BORDER;
            g.fill(dx + pad, fy, dx + pad + fw, fy + CONTROL_H, INPUT_BG);
            drawBorder(g, dx + pad, fy, fw, CONTROL_H, tierBorder);
            int tierTxtW = Math.max(10, fw - font.width(" ▾") - 6);
            g.drawString(font, truncatePx(tierLabel, tierTxtW), dx + pad + 4,
                         centeredTextY(fy, CONTROL_H), tierDDOpen ? ACCENT : TEXT, false);
            g.drawString(font, "▾", dx + pad + fw - font.width("▾") - 4,
                         centeredTextY(fy, CONTROL_H), tierDDOpen ? ACCENT : TEXT_DIM, false);
            return;
        }

        // ── Atributo: etiqueta + fondo del campo ──────────────────────────
        int fy = layout.drawerFieldAttrY();
        drawFieldLabel(g, "Attribute", dx + pad, fy);
        // El campo Attribute es un pseudo-botón: muestra el valor con un indicador ▼
        String attrVal = (drawerAttrBox != null && !drawerAttrBox.getValue().isBlank())
                          ? drawerAttrBox.getValue()
                          : "§8minecraft:generic.attack_damage";  // hint en gris
        boolean attrDDOpen = dropdown.openDropdown() == OpenDropdown.ATTRIBUTE;
        int attrBorderColor = attrDDOpen ? ACCENT : BORDER;
        g.fill(dx + pad, fy, dx + pad + fw, fy + CONTROL_H, INPUT_BG);
        drawBorder(g, dx + pad, fy, fw, CONTROL_H, attrBorderColor);
        // Texto del valor seleccionado (truncado para dejar espacio al ▼)
        int attrTxtW = Math.max(10, fw - font.width(" ▾") - 6);
        g.drawString(font, truncatePx(attrVal, attrTxtW), dx + pad + 4,
                     centeredTextY(fy, CONTROL_H), attrDDOpen ? ACCENT : TEXT, false);
        g.drawString(font, "▾", dx + pad + fw - font.width("▾") - 4,
                     centeredTextY(fy, CONTROL_H), attrDDOpen ? ACCENT : TEXT_DIM, false);

        // ── Valor: etiqueta + fondo del EditBox ───────────────────────────
        fy = layout.drawerFieldValueY();
        boolean isRemove = draft.action() == EditableAttributeAction.REMOVE;
        drawFieldLabel(g, "Value", dx + pad, fy);
        // El fondo lo pintamos nosotros; el EditBox va encima (super.render lo dibuja)
        int valueBorderColor = (drawerValueBox != null && drawerValueBox.isFocused()) ? ACCENT : BORDER;
        g.fill(dx + pad, fy, dx + pad + fw, fy + CONTROL_H, isRemove ? PANEL_DIM : INPUT_BG);
        drawBorder(g, dx + pad, fy, fw, CONTROL_H, isRemove ? BORDER_SOFT : valueBorderColor);
        if (isRemove)
            g.drawString(font, "not used for remove", dx + pad + 4,
                         centeredTextY(fy, CONTROL_H), TEXT_MUTED, false);

        // ── Tipo: etiqueta + toggles ──────────────────────────────────────
        fy = layout.drawerFieldTypeY();
        drawFieldLabel(g, "Type", dx + pad, fy);
        int halfW = (fw - 4) / 2;
        boolean isPercent = draft.isPercentual();
        renderTypeToggle(g, dx + pad,             fy, halfW, "Exact",    !isPercent);
        renderTypeToggle(g, dx + pad + halfW + 4, fy, halfW, "Percent",  isPercent);
    }

    private void renderTypeToggle(GuiGraphics g, int x, int y, int w, String label, boolean active) {
        int border = active ? ACCENT : BORDER;
        int bg     = active ? SEL_BG : INPUT_BG;
        int color  = active ? ACCENT : TEXT_DIM;
        g.fill(x, y, x + w, y + CONTROL_H, bg);
        drawBorder(g, x, y, w, CONTROL_H, border);
        g.drawString(font, label, x + (w - font.width(label)) / 2, centeredTextY(y, CONTROL_H), color, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — Dropdown overlay (solo dentro del drawer)
    // ══════════════════════════════════════════════════════════════════════

    private void renderDropdownOverlay(GuiGraphics g, int mx, int my, float pt) {
        if (!drawerOpen || dropdown.openDropdown() == OpenDropdown.NONE) return;
        if (layout == null) return;

        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        if (dropdown.openDropdown() == OpenDropdown.ATTRIBUTE) {
            Rect2i anchor = attrDropdownAnchor();
            List<AttributeOption> opts = filteredAttrOptions();
            DropdownController.AttributeDropdownBounds bounds =
                    dropdown.attributeDropdownBounds(anchor, opts.size(),
                            height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                            layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);

            renderAttrDropdownMenu(g, bounds, opts, mx, my);
            if (drawerAttrSearch != null && drawerAttrSearch.visible) {
                drawerAttrSearch.render(g, mx, my, pt);
            }
        } else if (dropdown.openDropdown() == OpenDropdown.MINING_TIER) {
            Rect2i anchor = miningTierDropdownAnchor();
            DropdownController.DropdownBounds bounds =
                    dropdown.dropdownBounds(anchor, miningTiers.size(),
                            height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                            layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);
            renderMiningTierDropdownMenu(g, bounds, mx, my);
        }

        g.pose().popPose();
    }

    private void renderAttrDropdownMenu(GuiGraphics g, DropdownController.AttributeDropdownBounds b,
                                         List<AttributeOption> opts, int mx, int my) {
        int selectedIdx = selectedAttrIndex(opts);
        dropdown.dropdownScroll(clamp(dropdown.dropdownScroll(), 0, Math.max(0, opts.size() - b.visibleRows())));

        g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), PANEL);
        drawBorder(g, b.x(), b.y(), b.w(), b.h(), ACCENT);

        // Mini cabecera
        g.fill(b.x() + 1, b.y() + 1, b.x() + b.w() - 1, b.y() + ATTR_DD_TITLE_H, HEADER_BG);
        g.fill(b.x() + 1, b.y() + 1, b.x() + 4, b.y() + ATTR_DD_TITLE_H, ACCENT);
        g.drawString(font, "ATTRIBUTE REGISTRY", b.x() + 7, b.y() + 3, TEXT_DIM, false);
        String cnt = Integer.toString(opts.size());
        g.drawString(font, cnt, b.x() + b.w() - font.width(cnt) - 8, b.y() + 3, TEXT_MUTED, false);

        // Posicionar el search box
        if (drawerAttrSearch != null) {
            drawerAttrSearch.setX(b.x() + 3);
            drawerAttrSearch.setY(b.searchY());
            drawerAttrSearch.setWidth(b.w() - 6);
            drawerAttrSearch.setHeight(ATTR_DD_SEARCH_H);
            if (attrSearchResetPending) {
                resetEditBoxCaret(drawerAttrSearch);
                attrSearchResetPending = false;
            }
            drawerAttrSearch.visible = true;
            drawerAttrSearch.active  = true;
        }

        if (opts.isEmpty()) {
            g.drawString(font, "No matches", b.x() + 7, b.listY() + 4, TEXT_MUTED, false);
            return;
        }

        for (int vi = 0; vi < b.visibleRows(); vi++) {
            int ii = dropdown.dropdownScroll() + vi;
            if (ii < 0 || ii >= opts.size()) continue;

            int ry = b.listY() + vi * ATTR_DD_ITEM_H;
            AttributeOption opt = opts.get(ii);
            boolean hover = inRect(mx, my, b.x(), ry, b.w(), ATTR_DD_ITEM_H);
            boolean sel   = ii == selectedIdx;

            g.fill(b.x() + 1, ry, b.x() + b.w() - 1, ry + ATTR_DD_ITEM_H,
                   sel ? SEL_BG : (vi % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !sel) g.fill(b.x() + 1, ry, b.x() + b.w() - 1, ry + ATTR_DD_ITEM_H, HOVER_TINT);
            if (sel)           g.fill(b.x() + 1, ry, b.x() + 4,         ry + ATTR_DD_ITEM_H, ACCENT);

            g.drawString(font, truncatePx(opt.translatedName(), b.w() - 14), b.x() + 7, ry + 2,  sel ? ACCENT : TEXT, false);
            g.drawString(font, truncatePx(opt.id().toString(),  b.w() - 14), b.x() + 7, ry + 11, TEXT_DIM, false);
        }

        // Scrollbar del dropdown
        if (opts.size() > b.visibleRows()) {
            int maxS = opts.size() - b.visibleRows();
            drawScrollbar(g, b.x() + b.w() - 4, b.listY() + 2, b.listY() + b.listH() - 2,
                          dropdown.dropdownScroll(), maxS, opts.size(), b.visibleRows());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Renderizado — Barra de estado
    // ══════════════════════════════════════════════════════════════════════

    private void renderMiningTierDropdownMenu(GuiGraphics g, DropdownController.DropdownBounds b, int mx, int my) {
        List<String> tiers = miningTiers;
        int selectedIdx = selectedMiningTierIndex();
        dropdown.dropdownScroll(clamp(dropdown.dropdownScroll(), 0, Math.max(0, tiers.size() - b.visibleRows())));

        g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), PANEL);
        drawBorder(g, b.x(), b.y(), b.w(), b.h(), ACCENT);

        if (tiers.isEmpty()) {
            g.drawString(font, "No tiers detected", b.x() + 7, b.y() + 4, TEXT_MUTED, false);
            return;
        }

        for (int vi = 0; vi < b.visibleRows(); vi++) {
            int ii = dropdown.dropdownScroll() + vi;
            if (ii < 0 || ii >= tiers.size()) continue;

            int ry = b.y() + vi * 16;
            String tier = tiers.get(ii);
            boolean hover = inRect(mx, my, b.x(), ry, b.w(), 16);
            boolean sel = ii == selectedIdx;

            g.fill(b.x() + 1, ry, b.x() + b.w() - 1, ry + 16, sel ? SEL_BG : (vi % 2 == 0 ? PANEL_ALT : PANEL_DEEP));
            if (hover && !sel) g.fill(b.x() + 1, ry, b.x() + b.w() - 1, ry + 16, HOVER_TINT);
            if (sel) g.fill(b.x() + 1, ry, b.x() + 4, ry + 16, ACCENT);

            g.drawString(font, truncatePx(tier, b.w() - 14), b.x() + 7, ry + 2, sel ? ACCENT : TEXT, false);
        }

        if (tiers.size() > b.visibleRows()) {
            int maxS = tiers.size() - b.visibleRows();
            drawScrollbar(g, b.x() + b.w() - 4, b.y() + 2, b.y() + b.h() - 2,
                          dropdown.dropdownScroll(), maxS, tiers.size(), b.visibleRows());
        }
    }

    private void renderStatusBar(GuiGraphics g) {
        int y = height - LayoutEngine.STATUS_BAR_H;
        g.fill(0, y, width, height, HEADER_BG);
        g.fill(0, y, width, y + 1, BORDER_SOFT);

        int color = statusError ? ERROR : (statusOk ? ACCENT : TEXT_DIM);
        String icon = statusError ? "ERR" : (statusOk ? "OK" : "...");
        int pillW = font.width(icon) + 10;
        g.fill(8, y + 4, 8 + pillW, y + 16, INPUT_BG);
        drawBorder(g, 8, y + 4, pillW, 12, statusError ? ERROR : (statusOk ? ACCENT2 : BORDER_SOFT));
        g.drawString(font, icon, 13, y + 6, color, false);
        g.drawString(font, truncatePx(status, Math.max(40, width - 200)),
                     8 + pillW + 6, centeredTextY(y, LayoutEngine.STATUS_BAR_H), color, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Interacción — Clicks
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        applyLayoutNow();
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        // 1. Click en dropdown del drawer
        if (drawerOpen && dropdown.openDropdown() == OpenDropdown.ATTRIBUTE) {
            if (handleAttrDropdownClick(mx, my, btn)) return true;
        } else if (drawerOpen && dropdown.openDropdown() == OpenDropdown.MINING_TIER) {
            if (handleMiningTierDropdownClick(mx, my, btn)) return true;
        }

        // 2. Click fuera del drawer cierra el dropdown
        if (drawerOpen && dropdown.openDropdown() != OpenDropdown.NONE) {
            Rect2i anchor = dropdown.openDropdown() == OpenDropdown.ATTRIBUTE
                    ? attrDropdownAnchor()
                    : miningTierDropdownAnchor();
            Rect2i boundsRect;
            if (dropdown.openDropdown() == OpenDropdown.ATTRIBUTE) {
                DropdownController.AttributeDropdownBounds b = dropdown.attributeDropdownBounds(
                        anchor, filteredAttrOptions().size(),
                        height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                        layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);
                boundsRect = new Rect2i(b.x(), b.y(), b.w(), b.h());
            } else {
                DropdownController.DropdownBounds b = dropdown.dropdownBounds(
                        anchor, miningTiers.size(),
                        height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                        layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);
                boundsRect = new Rect2i(b.x(), b.y(), b.w(), b.h());
            }
            if (!inRect(mx, my, boundsRect)) {
                dropdown.closeDropdown();
                hideAttrSearch();
                return true;
            }
        }

        // 3. Click en el drawer: campo atributo abre el dropdown
        if (drawerOpen) {
            Rect2i attrFieldRect = drawerAttrFieldRect();
            if (inRect(mx, my, attrFieldRect)) {
                openAttrDropdown();
                return true;
            }
            Rect2i tierFieldRect = miningTierFieldRect();
            if (inRect(mx, my, tierFieldRect)) {
                openMiningTierDropdown();
                return true;
            }
            // Click en tipo Exacto / Porcentual
            if (handleTypeToggleClick(mx, my)) return true;
        }

        // 4. RESET button
        if (selectedItem != null && hasRuleOverrides()) {
            Rect2i rr = resetButtonRect(layout.editorPanel());
            if (inRect(mx, my, rr)) { clickReset(); return true; }
        }

        // 5. Botón + Add
        if (selectedItem != null) {
            Rect2i addRect = addButtonRectForClick();
            if (inRect(mx, my, addRect)) { clickAddAttribute(); return true; }
            Rect2i addDurabilityRect = addDurabilityButtonRectForClick();
            if (inRect(mx, my, addDurabilityRect)) { clickAddDurability(); return true; }
            Rect2i addMiningRect = addMiningButtonRectForClick();
            if (inRect(mx, my, addMiningRect)) { clickAddMining(); return true; }
        }

        // 6. Modify / Delete en filas de atributos
        if (selectedItem != null && clickAttrRowButtons(mx, my)) return true;
        if (selectedItem != null && clickDurabilityButtons(mx, my)) return true;
        if (selectedItem != null && clickMiningButtons(mx, my)) return true;

        // 7. Click en lista de ítems
        if (clickItemList(mx, my)) { closeDrawer(); return true; }

        // 8. Click fuera del drawer ciérralo
        if (drawerOpen && !inRect(mx, my, layout.drawerX(), layout.drawerY(),
                                          layout.drawerW(), layout.drawerH())) {
            closeDrawer();
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleAttrDropdownClick(double mx, double my, int btn) {
        Rect2i anchor = attrDropdownAnchor();
        List<AttributeOption> opts = filteredAttrOptions();
        DropdownController.AttributeDropdownBounds b = dropdown.attributeDropdownBounds(
                anchor, opts.size(),
                height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);

        // Click en el search box
        Rect2i search = new Rect2i(b.x() + 3, b.searchY(), b.w() - 6, ATTR_DD_SEARCH_H);
        if (inRect(mx, my, search)) return super.mouseClicked(mx, my, btn) || true;

        // Click en un ítem
        int listTop = b.listY();
        if (mx >= b.x() && mx < b.x() + b.w() && my >= listTop && my < listTop + b.listH()) {
            int vi = ((int) my - listTop) / ATTR_DD_ITEM_H;
            int ii = dropdown.dropdownScroll() + vi;
            if (ii >= 0 && ii < opts.size()) {
                selectAttrOption(opts.get(ii));
                dropdown.closeDropdown();
                hideAttrSearch();
                return true;
            }
        }

        return inRect(mx, my, b.x(), b.y(), b.w(), b.h());
    }

    private boolean handleMiningTierDropdownClick(double mx, double my, int btn) {
        Rect2i anchor = miningTierDropdownAnchor();
        DropdownController.DropdownBounds b = dropdown.dropdownBounds(
                anchor, miningTiers.size(),
                height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);

        int listTop = b.y();
        if (mx >= b.x() && mx < b.x() + b.w() && my >= listTop && my < listTop + b.h()) {
            int vi = ((int) my - listTop) / 16;
            int ii = dropdown.dropdownScroll() + vi;
            if (ii >= 0 && ii < miningTiers.size()) {
                selectMiningTierOption(miningTiers.get(ii));
                dropdown.closeDropdown();
                return true;
            }
        }

        return inRect(mx, my, b.x(), b.y(), b.w(), b.h());
    }

    private boolean handleTypeToggleClick(double mx, double my) {
        if (layout == null) return false;
        int fy = layout.drawerFieldTypeY();
        int fw = layout.drawerW() - LayoutEngine.DRAWER_PADDING * 2;
        int half = (fw - 4) / 2;
        int dx = layout.drawerX() + LayoutEngine.DRAWER_PADDING;

        Rect2i exactRect   = new Rect2i(dx, fy, half, CONTROL_H);
        Rect2i percentRect = new Rect2i(dx + half + 4, fy, half, CONTROL_H);
        if (inRect(mx, my, exactRect))   { setDraftType(false); return true; }
        if (inRect(mx, my, percentRect)) { setDraftType(true);  return true; }
        return false;
    }

    private boolean clickAttrRowButtons(double mx, double my) {
        List<VisibleRow> rows = visibleRows();
        int baseY = layout.contentTop() + 6 + 14 + 2;   // después del section header
        for (int i = 0; i < rows.size(); i++) {
            int ry = baseY + i * 22 - attrScroll;
            if (my < ry - 4 || my >= ry + 22) continue;

            Rect2i modRect = modifyButtonRect(layout.attrListX(), ry, layout.attrListW());
            Rect2i delRect = deleteButtonRect(layout.attrListX(), ry, layout.attrListW());

            if (inRect(mx, my, modRect)) { openDrawerForRow(rows.get(i)); return true; }
            if (inRect(mx, my, delRect)) { deleteRow(rows.get(i));        return true; }
        }
        return false;
    }

    private boolean clickItemList(double mx, double my) {
        Rect2i b = layout.listPanel();
        int startY = b.getY() + PANEL_HEADER_H + 1 + SEARCH_H + 1;
        int endY   = b.getY() + b.getHeight() - 18;
        if (mx < b.getX() || mx >= b.getX() + b.getWidth() || my < startY || my >= endY) return false;

        int idx = itemScroll + ((int) my - startY) / ROW_H;
        if (idx >= 0 && idx < filteredItems.size()) {
            loadItem(filteredItems.get(idx));
            return true;
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Interacción — Scroll
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        applyLayoutNow();
        int step = delta > 0 ? -SCROLL_STEP : SCROLL_STEP;

        // Scroll en dropdown del drawer
        if (drawerOpen && dropdown.openDropdown() == OpenDropdown.ATTRIBUTE) {
            int count = filteredAttrOptions().size();
            Rect2i anchor = attrDropdownAnchor();
            DropdownController.AttributeDropdownBounds b = dropdown.attributeDropdownBounds(
                    anchor, count, height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                    layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);
            int max = Math.max(0, count - b.visibleRows());
            if (max > 0) {
                dropdown.dropdownScroll(clamp(dropdown.dropdownScroll() + (delta > 0 ? -1 : 1), 0, max));
                return true;
            }
        } else if (drawerOpen && dropdown.openDropdown() == OpenDropdown.MINING_TIER) {
            int count = miningTiers.size();
            Rect2i anchor = miningTierDropdownAnchor();
            DropdownController.DropdownBounds b = dropdown.dropdownBounds(
                    anchor, count, height, LayoutEngine.TOP_BAR_H, LayoutEngine.STATUS_BAR_H,
                    layout.editorPanel(), LayoutEngine.EDITOR_FOOTER_H);
            int max = Math.max(0, count - b.visibleRows());
            if (max > 0) {
                dropdown.dropdownScroll(clamp(dropdown.dropdownScroll() + (delta > 0 ? -1 : 1), 0, max));
                return true;
            }
        }

        // Scroll en lista de atributos
        if (layout != null) {
            Rect2i ep = layout.editorPanel();
            if (inRect(mx, my, ep)) {
                int contentH  = measureAttrContentHeight();
                int viewH     = layout.contentBottom() - LayoutEngine.EDITOR_FOOTER_H - layout.contentTop();
                int max = Math.max(0, contentH - viewH);
                if (max > 0) { attrScroll = clamp(attrScroll + step, 0, max); return true; }
            }
        }

        // Scroll en lista de ítems
        if (layout != null && layout.listPanel().contains((int) mx, (int) my)) {
            Rect2i b = layout.listPanel();
            int listTop = b.getY() + PANEL_HEADER_H + 1 + SEARCH_H + 1;
            int listBot = b.getY() + b.getHeight() - 18;
            int visible = Math.max(1, (listBot - listTop) / ROW_H);
            itemScroll = clamp(itemScroll + (delta > 0 ? -3 : 3), 0, Math.max(0, filteredItems.size() - visible));
            return true;
        }

        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ══════════════════════════════════════════════════════════════════════
    //  Acciones de negocio
    // ══════════════════════════════════════════════════════════════════════

    private void loadItem(CatalogItem item) {
        selectedItem = item;
        currentRule  = new EditableItemRule(item.id(), false);
        baseAttributes.clear();
        rowsDirty = true;
        attrScroll = 0;
        selectedMiningIndex = -1;
        closeDrawer();
        status = "Loading rule..."; statusError = false; statusOk = false;
        lastRuleJson = "";
        EditorNetwork.INSTANCE.sendToServer(new C2SRequestItemRulePacket(item.id(), false));
        layoutDirty = true;
    }

    private void clickAddAttribute() {
        ensureCurrentRule();
        draft.startNewAttributeDraft();
        openDrawer();
        clearDrawerFields();
        status = "Choose an attribute from the registry.";
        statusError = false; statusOk = false;
    }

    private void clickAddDurability() {
        ensureCurrentRule();
        draft.resetDraft();
        draft.draftSource(DraftSource.DURABILITY);
        openDrawer();
        clearDrawerFields();
        if (drawerValueBox != null) {
            Integer existing = currentRule.getDurability() == null ? null : currentRule.getDurability().getDurability();
            int suggested = existing != null ? existing : (selectedItem != null && selectedItem.maxDamage() > 0 ? selectedItem.maxDamage() : 100);
            drawerValueBox.setValue(Integer.toString(suggested));
            drawerValueBox.setHint(Component.literal("100"));
            drawerValueResetPending = true;
        }
        status = "Editing item durability.";
        statusError = false; statusOk = false;
    }

    private void clickAddMining() {
        ensureCurrentRule();
        selectedMiningIndex = -1;
        draft.resetDraft();
        draft.draftSource(DraftSource.MINING);
        openDrawer();
        clearDrawerFields();
        if (drawerValueBox != null) {
            float baseSpeed = selectedItemBaseMiningSpeed();
            drawerValueBox.setHint(Component.literal("Base " + formatAmount(baseSpeed)));
            drawerValueResetPending = true;
        }
        if (drawerMiningTierBox != null) {
            drawerMiningTierBox.setValue("");
            drawerMiningTierBox.setHint(Component.literal("Select tier"));
        }
        status = "Editing mining override.";
        statusError = false; statusOk = false;
    }

    private void openDrawerForMining(int index) {
        ensureCurrentRule();
        if (index < 0 || index >= currentRule.getMiningOverrides().size()) {
            return;
        }
        selectedMiningIndex = index;
        draft.resetDraft();
        draft.draftSource(DraftSource.MINING);
        EditableMiningOverride override = currentRule.getMiningOverrides().get(index);
        openDrawer();
        clearDrawerFields();
        if (drawerValueBox != null) {
            drawerValueBox.setValue(override.getSpeed() == null ? "" : formatAmount(override.getSpeed()));
            float baseSpeed = selectedItemBaseMiningSpeed();
            drawerValueBox.setHint(Component.literal("Base " + formatAmount(baseSpeed)));
            drawerValueResetPending = true;
        }
        if (drawerMiningTierBox != null) {
            drawerMiningTierBox.setValue(override.getTier() == null ? "" : override.getTier());
            drawerMiningTierBox.setHint(Component.literal("Select tier"));
            resetEditBoxCaret(drawerMiningTierBox);
        }
        status = "Editing mining override.";
        statusError = false; statusOk = false;
    }

    private void openDrawerForRow(VisibleRow row) {
        ensureCurrentRule();
        if (row.modifier() != null) {
            draft.selectedModifierIndex(row.modIndex());
            draft.selectedBaseAttributeIndex(row.baseIndex());
            draft.loadModifierIntoDraft(row.modifier());
            loadModifierIntoDrawerFields(row.modifier());
            status = "Editing attribute change.";
        } else {
            draft.selectedBaseAttributeIndex(row.baseIndex());
            draft.selectedModifierIndex(-1);
            draft.loadBaseAttributeIntoDraft(
                    row.base().attributeId(), row.base().amount(), row.base().operation(), row.base().slot());
            loadBaseAttrIntoDrawerFields(row.base());
            status = "Editing base attribute.";
        }
        statusError = false; statusOk = false;
        openDrawer();
    }

    private void loadModifierIntoDrawerFields(EditableAttributeModifier m) {
        if (drawerAttrBox != null) {
            drawerAttrBox.setValue(m.getAttributeId() == null ? "" : m.getAttributeId().toString());
            resetEditBoxCaret(drawerAttrBox);
        }
        if (drawerValueBox != null) {
            drawerValueBox.setValue(formatAmountForInput(m.getAmount(), m.getOperation()));
            drawerValueResetPending = true;
        }
    }

    private void loadBaseAttrIntoDrawerFields(BaseAttribute a) {
        if (drawerAttrBox != null) {
            drawerAttrBox.setValue(a.attributeId().toString());
            resetEditBoxCaret(drawerAttrBox);
        }
        if (drawerValueBox != null) {
            drawerValueBox.setValue(formatAmountForInput(a.amount(), a.operation()));
            drawerValueResetPending = true;
        }
    }

    private void clearDrawerFields() {
        if (drawerAttrBox   != null) {
            drawerAttrBox.setValue("");
            resetEditBoxCaret(drawerAttrBox);
        }
        if (drawerAttrSearch != null) {
            drawerAttrSearch.setValue("");
            attrSearchResetPending = true;
        }
        if (drawerValueBox  != null) {
            drawerValueBox.setValue("");
            drawerValueResetPending = true;
        }
        if (drawerMiningTierBox != null) {
            drawerMiningTierBox.setValue("");
            resetEditBoxCaret(drawerMiningTierBox);
        }
    }

    private void applyDraft() {
        if (selectedItem == null) { setStatus("Select an item first.", true); return; }
        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();

        if (draft.draftSource() == DraftSource.DURABILITY) {
            Integer durability = parseDurabilityInput(drawerValueBox == null ? "" : drawerValueBox.getValue().trim());
            if (durability == null) { setStatus("Durability must be a whole number of at least 1.", true); return; }
            EditableDurabilityModifier edited = rule.getDurability() == null
                    ? new EditableDurabilityModifier(durability)
                    : rule.getDurability().copy();
            edited.setDurability(durability);
            rule.setDurability(edited);
            currentRule = rule.copy();
            rowsDirty = true;
            status = "Saving..."; statusError = false; statusOk = false;
            EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
            closeDrawer();
            return;
        }

        if (draft.draftSource() == DraftSource.MINING) {
            Float speed;
            try {
                speed = parseMiningSpeed(drawerValueBox == null ? "" : drawerValueBox.getValue().trim());
            } catch (NumberFormatException e) {
                setStatus("Mining speed must be a number.", true); return;
            }
            if (speed != null && speed < 0.0f) {
                setStatus("Mining speed must be non-negative.", true); return;
            }
            String tier = drawerMiningTierBox == null ? "" : drawerMiningTierBox.getValue().trim().toLowerCase(Locale.ROOT);
            if (speed == null && tier.isBlank()) {
                setStatus("Mining needs speed, tier, or both.", true); return;
            }
            if (!tier.isBlank() && !isSupportedTier(tier)) {
                setStatus("Unsupported mining tier.", true); return;
            }

            EditableMiningOverride edited = new EditableMiningOverride(speed, tier.isBlank() ? null : tier);
            if (selectedMiningIndex >= 0 && selectedMiningIndex < rule.getMiningOverrides().size()) {
                rule.getMiningOverrides().set(selectedMiningIndex, edited);
            } else {
                rule.getMiningOverrides().add(edited);
                selectedMiningIndex = rule.getMiningOverrides().size() - 1;
            }
            currentRule = rule.copy();
            rowsDirty = true;
            status = "Saving..."; statusError = false; statusOk = false;
            EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
            closeDrawer();
            return;
        }

        String rawAttr = drawerAttrBox == null ? "" : drawerAttrBox.getValue().trim();
        if (rawAttr.isBlank()) { setStatus("No attribute selected.", true); return; }

        ResourceLocation attrId = ResourceLocation.tryParse(rawAttr);
        if (attrId == null) { setStatus("Invalid attribute ID.", true); return; }

        Double amount;
        try {
            amount = parseAmountInput(drawerValueBox == null ? "" : drawerValueBox.getValue().trim());
        } catch (NumberFormatException e) {
            setStatus("Amount must be a number.", true); return;
        }

        EditableAttributeModifier edited = new EditableAttributeModifier(
                attrId,
                draft.effectiveAction(),
                amount,
                draft.effectiveOperation(),
                draft.slotType(),
                draft.slot()
        );

        int targetIdx = draft.selectedModifierIndex();
        if (draft.draftSource() == DraftSource.MODIFIER && targetIdx >= 0 && targetIdx < rule.getAttributes().size()) {
            rule.getAttributes().set(targetIdx, edited);
        } else {
            rule.getAttributes().removeIf(ex -> edited.duplicateKey().equalsIgnoreCase(ex.duplicateKey()));
            rule.getAttributes().add(edited);
            targetIdx = rule.getAttributes().size() - 1;
        }

        draft.selectedModifierIndex(clamp(targetIdx, 0, Math.max(0, rule.getAttributes().size() - 1)));
        draft.selectedBaseAttributeIndex(-1);
        draft.draftSource(DraftSource.MODIFIER);

        currentRule = rule.copy();
        rowsDirty = true;
        status = "Saving..."; statusError = false; statusOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        closeDrawer();
    }

    private void deleteRow(VisibleRow row) {
        if (selectedItem == null) return;
        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();

        if (row.modifier() != null && row.modIndex() >= 0 && row.modIndex() < rule.getAttributes().size()) {
            EditableAttributeModifier removed = rule.getAttributes().remove(row.modIndex());
            if (draft.draftSource() == DraftSource.MODIFIER && draft.selectedModifierIndex() == row.modIndex())
                draft.resetDraft();
            status = "Deleted: " + attrDisplayName(removed.getAttributeId());
        } else if (row.base() != null) {
            BaseAttribute a   = row.base();
            String structKey  = structKey(a.attributeId(), a.operation(), EditableSlotType.STANDARD, a.slot()).toLowerCase(Locale.ROOT);
            rule.getAttributes().removeIf(ex -> ex != null && ex.getAttributeId() != null
                    && ex.getAction() != EditableAttributeAction.REMOVE
                    && structKey(ex.getAttributeId(), ex.getOperation(), ex.getSlotType(), ex.getSlot())
                                    .toLowerCase(Locale.ROOT).equals(structKey));
            rule.getAttributes().add(new EditableAttributeModifier(
                    a.attributeId(), EditableAttributeAction.REMOVE, null,
                    a.operation(), EditableSlotType.STANDARD, a.slot()));
            if (draft.draftSource() == DraftSource.BASE && draft.selectedBaseAttributeIndex() == row.baseIndex())
                draft.resetDraft();
            status = "Removed: " + attrDisplayName(a.attributeId());
        } else return;

        statusError = false; statusOk = false;
        currentRule = rule.copy();
        rowsDirty = true;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        closeDrawer();
        layoutDirty = true;
    }

    private void clickReset() {
        if (selectedItem == null || !hasRuleOverrides()) return;
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    minecraft.setScreen(this);
                    if (confirmed) resetRuleToVanilla();
                },
                Component.literal("Confirm reset"),
                Component.literal("This will restore the item to vanilla attributes and discard all changes.")));
    }

    private void resetRuleToVanilla() {
        if (selectedItem == null) return;
        currentRule = new EditableItemRule(selectedItem.id(), false);
        rowsDirty = true;
        draft.resetDraft();
        selectedMiningIndex = -1;
        attrScroll = 0;
        closeDrawer();
        status = "Resetting..."; statusError = false; statusOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(currentRule)));
    }

    // ── Drawer open/close ────────────────────────────────────────────────

    private void openDrawer() {
        drawerOpen = true;
        layoutDirty = true;
        applyLayoutNow();
        setDrawerWidgetsVisible(true);
    }

    private void closeDrawer() {
        drawerOpen  = false;
        dropdown.closeDropdown();
        hideAttrSearch();
        layoutDirty = true;
        applyLayoutNow();
        setDrawerWidgetsVisible(false);
    }

    private void setDrawerWidgetsVisible(boolean visible) {
        // drawerAttrBox is NEVER visible as a widget; we draw it as a pseudo-button manually.
        if (drawerAttrBox    != null) { drawerAttrBox.visible = false;    drawerAttrBox.active = false; }
        // drawerValueBox IS a real EditBox — visible when drawer is open and action != REMOVE
        boolean valueVisible = visible && draft.action() != EditableAttributeAction.REMOVE;
        if (drawerValueBox   != null) { drawerValueBox.visible = valueVisible; drawerValueBox.active = valueVisible; }
        if (drawerMiningTierBox != null) { drawerMiningTierBox.visible = false; drawerMiningTierBox.active = false; }
        if (drawerApplyBtn   != null) { drawerApplyBtn.visible = visible;   drawerApplyBtn.active = visible; }
        if (drawerCancelBtn  != null) { drawerCancelBtn.visible = visible;  drawerCancelBtn.active = visible; }
        // drawerTypeExact / drawerTypePercent are drawn manually as toggles, not as Minecraft Buttons
        if (drawerTypeExact  != null) { drawerTypeExact.visible = false;    drawerTypeExact.active = false; }
        if (drawerTypePercent != null){ drawerTypePercent.visible = false;  drawerTypePercent.active = false; }
        if (visible && draft.draftSource() == DraftSource.DURABILITY && drawerValueBox != null) {
            drawerValueBox.visible = true;
            drawerValueBox.active = true;
        }
        if (visible && draft.draftSource() == DraftSource.MINING && drawerValueBox != null) {
            drawerValueBox.visible = true;
            drawerValueBox.active = true;
        }
        if (!visible) { hideAttrSearch(); }
    }

    private void openAttrDropdown() {
        if (drawerAttrSearch != null) {
            drawerAttrSearch.setValue(drawerAttrBox == null ? "" : drawerAttrBox.getValue().trim());
            attrSearchResetPending = true;
        }
        attrFilterDirty = true;
        dropdown.toggleDropdown(OpenDropdown.ATTRIBUTE);
        if (dropdown.openDropdown() == OpenDropdown.ATTRIBUTE && drawerAttrSearch != null) {
            drawerAttrSearch.visible = true;
            drawerAttrSearch.active  = true;
            setFocused(drawerAttrSearch);
            attrSearchResetPending = true;
        }
    }

    private void openMiningTierDropdown() {
        if (miningTiers.isEmpty()) {
            setStatus("No mining tiers detected in the registry.", true);
            return;
        }
        dropdown.toggleDropdown(OpenDropdown.MINING_TIER);
        if (dropdown.openDropdown() == OpenDropdown.MINING_TIER) {
            dropdown.dropdownScroll(Math.max(0, selectedMiningTierIndex()));
        }
    }

    private void hideAttrSearch() {
        if (drawerAttrSearch != null) { drawerAttrSearch.visible = false; drawerAttrSearch.active = false; }
    }

    private void selectAttrOption(AttributeOption opt) {
        if (drawerAttrBox != null) {
            drawerAttrBox.setValue(opt.id().toString());
            resetEditBoxCaret(drawerAttrBox);
        }
        hideAttrSearch();
        dropdown.closeDropdown();
    }

    private void selectMiningTierOption(String tier) {
        if (drawerMiningTierBox == null) {
            return;
        }
        drawerMiningTierBox.setValue(tier == null ? "" : tier);
        resetEditBoxCaret(drawerMiningTierBox);
    }

    private Rect2i miningTierFieldRect() {
        if (layout == null) return new Rect2i(0, 0, 0, 0);
        return new Rect2i(layout.drawerX() + LayoutEngine.DRAWER_PADDING,
                          layout.drawerFieldTypeY(), layout.drawerW() - LayoutEngine.DRAWER_PADDING * 2, CONTROL_H);
    }

    private Rect2i miningTierDropdownAnchor() {
        return miningTierFieldRect();
    }

    private void setDraftType(boolean percent) {
        draft.operationType(percent ? EditableOperationType.MULTIPLY_BASE : EditableOperationType.ADDITION);
        if (drawerValueBox != null) {
            String raw = drawerValueBox.getValue().trim().replace("%", "");
            if (!raw.isBlank()) {
                drawerValueBox.setValue(percent ? raw + "%" : raw);
                drawerValueResetPending = true;
            }
            drawerValueBox.setHint(Component.literal(percent ? "10%" : "0"));
        }
        layoutDirty = true;
    }

    private void resetEditBoxCaret(EditBox box) {
        if (box == null) return;
        box.moveCursorToStart();
        box.setHighlightPos(0);
        box.setCursorPosition(0);
        try {
            var f = EditBox.class.getDeclaredField("displayPos");
            f.setAccessible(true);
            f.setInt(box, 0);
        } catch (ReflectiveOperationException ignored) {
            // If the field name changes, the public cursor reset still helps.
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Layout
    // ══════════════════════════════════════════════════════════════════════

    private void applyLayoutNow() {
        if (layout == null || layoutDirty
                || layout.listPanel().getX() != 8   // detección de resize
                || layout.listPanel().getY() != LayoutEngine.TOP_BAR_H + LayoutEngine.PANEL_GAP) {
            layout = LayoutEngine.computeLayout(width, height, drawerOpen);
            layoutDirty = false;
            positionWidgets();
        }
    }

    private void positionWidgets() {
        if (layout == null) return;
        Rect2i lp = layout.listPanel();

        // Search box
        if (searchBox != null) {
            searchBox.setX(lp.getX() + 11);
            searchBox.setY(centeredTextY(lp.getY() + PANEL_HEADER_H + 3, SEARCH_H - 6));
            searchBox.setWidth(lp.getWidth() - 22);
        }

        if (!drawerOpen) return;

        int dx = layout.drawerX();
        int dw = layout.drawerW();
        int pad = LayoutEngine.DRAWER_PADDING;
        int fw  = dw - pad * 2;

        // drawerAttrBox: stays invisible, just positioned so getValue/setValue work
        if (drawerAttrBox != null) {
            drawerAttrBox.setX(dx + pad + 4);
            drawerAttrBox.setY(layout.drawerFieldAttrY());
            drawerAttrBox.setWidth(Math.max(10, fw - 8));
            drawerAttrBox.setHeight(CONTROL_H);
        }

        // drawerValueBox: real EditBox painted over our INPUT_BG background
        // Position it exactly matching the renderInputBg call in renderDrawerBackground
        if (drawerValueBox != null) {
            drawerValueBox.setX(dx + pad + 4);
            drawerValueBox.setY(layout.drawerFieldValueY() + 5);
            drawerValueBox.setWidth(Math.max(10, fw - 8));
            drawerValueBox.setHeight(CONTROL_H);
            if (drawerValueResetPending) {
                resetEditBoxCaret(drawerValueBox);
                drawerValueResetPending = false;
            }
        }

        if (drawerMiningTierBox != null) {
            drawerMiningTierBox.setX(dx + pad + 4);
            drawerMiningTierBox.setY(layout.drawerFieldTypeY() + 5);
            drawerMiningTierBox.setWidth(Math.max(10, fw - 8));
            drawerMiningTierBox.setHeight(CONTROL_H);
        }

        // Botones Apply / Cancel
        int btY = layout.drawerButtonsY();
        int btW = (fw - 6) / 2;
        if (drawerApplyBtn != null) {
            drawerApplyBtn.setX(dx + pad);
            drawerApplyBtn.setY(btY);
            drawerApplyBtn.setWidth(btW);
            drawerApplyBtn.setHeight(BUTTON_H);
        }
        if (drawerCancelBtn != null) {
            drawerCancelBtn.setX(dx + pad + btW + 6);
            drawerCancelBtn.setY(btY);
            drawerCancelBtn.setWidth(btW);
            drawerCancelBtn.setHeight(BUTTON_H);
        }
    }

    private Rect2i drawerAttrFieldRect() {
        if (layout == null) return new Rect2i(0, 0, 0, 0);
        return new Rect2i(layout.drawerX() + LayoutEngine.DRAWER_PADDING,
                          layout.drawerFieldAttrY(), layout.drawerW() - LayoutEngine.DRAWER_PADDING * 2, CONTROL_H);
    }

    private Rect2i attrDropdownAnchor() {
        return drawerAttrFieldRect();
    }

    private Rect2i addButtonRectForClick() {
        // El botón está en el area de la lista de atributos (lógica, sin scroll)
        int cy = layout.contentTop() + 6;
        return addButtonRect(layout.attrListX(), cy, layout.attrListW());
    }

    // ══════════════════════════════════════════════════════════════════════
    private Rect2i addDurabilityButtonRectForClick() {
        int y = durabilitySectionHeaderY() - attrScroll;
        return addButtonRect(layout.attrListX(), y, layout.attrListW());
    }

    private Rect2i addMiningButtonRectForClick() {
        int y = miningSectionHeaderY() - attrScroll;
        return addButtonRect(layout.attrListX(), y, layout.attrListW());
    }

    //  Red
    // ══════════════════════════════════════════════════════════════════════

    private void consumeNetworkState() {
        String catJson = EditorClientState.latestCatalogJson();
        if (!catJson.equals(lastCatalogJson)) {
            lastCatalogJson = catJson;
            parseCatalog(catJson);
            refreshItemFilter();
            status = "Catalog loaded: " + allItems.size() + " items";
            statusError = false; statusOk = true;
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
        allItems.clear(); attributes.clear(); miningTiers.clear();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            if (items != null) for (var el : items) {
                JsonObject o = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(o.get("id").getAsString());
                if (id == null) continue;
                String desc = o.get("descriptionId").getAsString();
                allItems.add(new CatalogItem(id, desc, Component.translatable(desc).getString(),
                             o.get("maxDamage").getAsInt(), o.get("damageable").getAsBoolean()));
            }
            JsonArray attrs = root.getAsJsonArray("attributes");
            if (attrs != null) for (var el : attrs) {
                JsonObject o = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(o.get("id").getAsString());
                if (id == null) continue;
                String desc = o.has("descriptionId") ? o.get("descriptionId").getAsString() : id.toString();
                String translated = Component.translatable(desc).getString();
                String ns = id.getNamespace();
                attributes.add(new AttributeOption(id, desc, translated, ns,
                        (id + " " + ns + " " + translated + " " + desc).toLowerCase(Locale.ROOT)));
            }
            attributes.sort(Comparator.comparing(AttributeOption::namespace)
                    .thenComparing(AttributeOption::translatedName)
                    .thenComparing(o -> o.id().toString()));

            JsonArray tiers = root.getAsJsonArray("miningTiers");
            if (tiers != null) for (var el : tiers) {
                String tier = el.getAsString();
                if (tier != null && !tier.isBlank()) {
                    miningTiers.add(tier);
                }
            }
            miningTiers.sort(String::compareToIgnoreCase);
            attrFilterDirty = true;
        } catch (RuntimeException e) {
            status = "Catalog parse failed: " + e.getMessage(); statusError = true; statusOk = false;
        }
    }

    private void refreshItemFilter() {
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredItems.clear();
        for (CatalogItem it : allItems) {
            if (q.isBlank() || it.id().toString().toLowerCase(Locale.ROOT).contains(q)
                    || it.translatedName().toLowerCase(Locale.ROOT).contains(q)
                    || it.descriptionId().toLowerCase(Locale.ROOT).contains(q))
                filteredItems.add(it);
        }
        filteredItems.sort(Comparator.comparing(CatalogItem::translatedName));
    }

    private void loadRulePayload(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            ResourceLocation target = ResourceLocation.tryParse(root.get("targetId").getAsString());
            if (selectedItem != null && target != null && !selectedItem.id().equals(target)) return;

            draft.resetDraft();
            selectedMiningIndex = -1;
            externalConflict = root.has("externalConflict") && root.get("externalConflict").getAsBoolean();
            parseBaseAttributes(root);
            currentRule = EditorJsonPayloads.ruleFromPayload(json).orElse(null);
            if (currentRule == null && selectedItem != null)
                currentRule = new EditableItemRule(selectedItem.id(), false);
            rowsDirty  = true;
            attrScroll = 0;
            selectedMiningIndex = -1;
            closeDrawer();
            status = externalConflict ? "External rule detected." : "Rule loaded.";
            statusError = false; statusOk = !externalConflict;
            layoutDirty = true;
        } catch (RuntimeException e) {
            status = "Rule parse failed: " + e.getMessage(); statusError = true; statusOk = false;
        }
    }

    private void parseSaveResult(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            boolean ok  = root.has("success") && root.get("success").getAsBoolean();
            String  msg = root.has("message") ? root.get("message").getAsString() : "";
            status = (ok ? "Saved. " : "Save failed. ") + msg;
            statusError = !ok; statusOk = ok;
        } catch (RuntimeException e) {
            status = "Save result parse failed: " + e.getMessage(); statusError = true; statusOk = false;
        }
    }

    private void parseBaseAttributes(JsonObject root) {
        baseAttributes.clear();
        JsonArray arr = root.getAsJsonArray("baseAttributes");
        if (arr == null) return;
        for (var el : arr) {
            JsonObject o = el.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(o.get("attribute").getAsString());
            EditableOperationType op = EditableOperationType.fromString(o.get("operation").getAsString());
            if (id == null || op == null) continue;
            baseAttributes.add(new BaseAttribute(id, o.get("amount").getAsDouble(), op, o.get("slot").getAsString()));
        }
        rowsDirty = true;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Construcción de filas visibles
    // ══════════════════════════════════════════════════════════════════════

    private List<VisibleRow> visibleRows() {
        if (rowsDirty) { rowsCache = buildVisibleRows(); rowsDirty = false; }
        return rowsCache;
    }

    private List<VisibleRow> buildVisibleRows() {
        if (baseAttributes.isEmpty() && (currentRule == null || currentRule.getAttributes().isEmpty()))
            return List.of();

        Map<String, VisibleRow> effective = new LinkedHashMap<>();
        for (int i = 0; i < baseAttributes.size(); i++) {
            BaseAttribute b = baseAttributes.get(i);
            String key = structKey(b.attributeId(), b.operation(), EditableSlotType.STANDARD, b.slot()).toLowerCase(Locale.ROOT);
            effective.put(key, new VisibleRow(i, -1, b, null));
        }

        if (currentRule != null) {
            for (int i = 0; i < currentRule.getAttributes().size(); i++) {
                EditableAttributeModifier m = currentRule.getAttributes().get(i);
                if (m == null || m.getAttributeId() == null) continue;
                String sk = structKey(m.getAttributeId(), m.getOperation(), m.getSlotType(), m.getSlot()).toLowerCase(Locale.ROOT);
                if (m.getAction() == EditableAttributeAction.REMOVE) {
                    effective.entrySet().removeIf(e -> e.getKey().equals(sk) || e.getKey().startsWith(sk + "|"));
                    effective.put(sk, new VisibleRow(-1, i, null, m));
                    continue;
                }
                VisibleRow existing = effective.get(sk);
                if (existing == null) { effective.put(sk, new VisibleRow(-1, i, null, m)); continue; }
                effective.put(sk, new VisibleRow(existing.baseIndex(), i, existing.base(), m));
            }
        }

        return List.copyOf(effective.values());
    }

    private int measureAttrContentHeight() {
        int rows = visibleRows().size();
        int miningRows = currentRule == null ? 0 : currentRule.getMiningOverrides().size();
        return 6 + 14 + 2 + (rows == 0 ? 24 : rows * 22 + 12)
                + 14 + 2 + 20 + 12
                + 14 + 2 + (miningRows == 0 ? 20 : miningRows * 22) + 12;
    }

    // ══════════════════════════════════════════════════════════════════════
    private int renderDurabilitySection(GuiGraphics g, int x, int y, int w, int mx, int logicalMy) {
        int headerY = y;
        int cy = renderSectionHeader(g, x, headerY, w, "DURABILITY");
        Rect2i addRect = addButtonRect(x, headerY, w);
        boolean hasDurability = currentRule != null && currentRule.getDurability() != null
                && currentRule.getDurability().getDurability() != null;
        drawSmallButton(g, addRect, hasDurability ? "Modify" : "+ Add", true, inRect(mx, logicalMy, addRect));

        int ry = cy;
        g.fill(x + 4, ry, x + w - 4, ry + 20, PANEL_DEEP);
        if (hasDurability) {
            int value = currentRule.getDurability().getDurability();
            String valueText = Integer.toString(value);
            g.drawString(font, valueText, x + 10, centeredTextY(ry, 20), ACCENT, false);
            String label = selectedItem != null && selectedItem.damageable() ? "Vanilla durability override" : "Custom durability";
            g.drawString(font, truncatePx(label, Math.max(20, w - 100)), x + 10 + font.width(valueText) + 8,
                    centeredTextY(ry, 20), TEXT, false);
            Rect2i delRect = durabilityDeleteButtonRect(x, ry, w);
            drawDeleteButton(g, delRect, inRect(mx, logicalMy, delRect));
        } else {
            String label = selectedItem != null && selectedItem.damageable()
                    ? "No durability override. Vanilla max: " + selectedItem.maxDamage()
                    : "No custom durability.";
            g.drawString(font, truncatePx(label, Math.max(20, w - 20)), x + 10, centeredTextY(ry, 20), TEXT_DIM, false);
        }
        return ry + 32;
    }

    private boolean clickDurabilityButtons(double mx, double my) {
        int headerY = durabilitySectionHeaderY() - attrScroll;
        Rect2i addRect = addButtonRect(layout.attrListX(), headerY, layout.attrListW());
        if (inRect(mx, my, addRect)) {
            clickAddDurability();
            return true;
        }

        if (currentRule == null || currentRule.getDurability() == null) {
            return false;
        }

        int rowY = headerY + 14 + 2;
        Rect2i delRect = durabilityDeleteButtonRect(layout.attrListX(), rowY, layout.attrListW());
        if (inRect(mx, my, delRect)) {
            deleteDurability();
            return true;
        }
        return false;
    }

    private int durabilitySectionHeaderY() {
        int rows = visibleRows().size();
        int cy = layout.contentTop() + 6 + 14 + 2;
        cy += rows == 0 ? 24 : rows * 22 + 12;
        return cy;
    }

    private Rect2i durabilityDeleteButtonRect(int x, int ry, int w) {
        return new Rect2i(x + w - 26, ry + 3, 22, 14);
    }

    private void deleteDurability() {
        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();
        rule.setDurability(null);
        currentRule = rule.copy();
        rowsDirty = true;
        status = "Saving..."; statusError = false; statusOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        closeDrawer();
        layoutDirty = true;
    }

    private void renderMiningSection(GuiGraphics g, int x, int y, int w, int mx, int logicalMy) {
        int headerY = y;
        int cy = renderSectionHeader(g, x, headerY, w, "MINING");
        Rect2i addRect = addButtonRect(x, headerY, w);
        drawSmallButton(g, addRect, "+ Add", true, inRect(mx, logicalMy, addRect));

        List<EditableMiningOverride> mining = currentRule == null ? List.of() : currentRule.getMiningOverrides();
        if (mining.isEmpty()) {
            g.fill(x + 4, cy, x + w - 4, cy + 20, PANEL_DEEP);
            g.drawString(font, "No mining override.", x + 10, centeredTextY(cy, 20), TEXT_DIM, false);
            return;
        }

        for (int i = 0; i < mining.size(); i++) {
            int ry = cy + i * 22;
            EditableMiningOverride override = mining.get(i);
            boolean selected = draft.draftSource() == DraftSource.MINING && selectedMiningIndex == i;
            boolean hover = inRect(mx, logicalMy, x + 4, ry, w - 8, 20);
            g.fill(x + 4, ry, x + w - 4, ry + 20, selected ? SEL_BG : (i % 2 == 0 ? PANEL_DEEP : PANEL_ALT));
            if (hover && !selected) g.fill(x + 4, ry, x + w - 4, ry + 20, HOVER_TINT);
            if (selected) g.fill(x + 4, ry, x + 7, ry + 20, ACCENT);

            String speed = override.getSpeed() == null ? "-" : formatAmount(override.getSpeed());
            String tier = override.getTier() == null || override.getTier().isBlank() ? "-" : override.getTier();
            String text = "Speed " + speed + "  Tier " + tier;
            g.drawString(font, truncatePx(text, Math.max(20, w - 100)), x + 10, centeredTextY(ry, 20), TEXT, false);

            Rect2i modRect = miningModifyButtonRect(x, ry, w);
            Rect2i delRect = miningDeleteButtonRect(x, ry, w);
            drawSmallButton(g, modRect, "Modify", true, inRect(mx, logicalMy, modRect));
            drawDeleteButton(g, delRect, inRect(mx, logicalMy, delRect));
        }
    }

    private boolean clickMiningButtons(double mx, double my) {
        int headerY = miningSectionHeaderY() - attrScroll;
        Rect2i addRect = addButtonRect(layout.attrListX(), headerY, layout.attrListW());
        if (inRect(mx, my, addRect)) {
            clickAddMining();
            return true;
        }

        if (currentRule == null || currentRule.getMiningOverrides().isEmpty()) {
            return false;
        }

        int rowBaseY = headerY + 14 + 2;
        for (int i = 0; i < currentRule.getMiningOverrides().size(); i++) {
            int ry = rowBaseY + i * 22;
            Rect2i modRect = miningModifyButtonRect(layout.attrListX(), ry, layout.attrListW());
            Rect2i delRect = miningDeleteButtonRect(layout.attrListX(), ry, layout.attrListW());
            if (inRect(mx, my, modRect)) {
                openDrawerForMining(i);
                return true;
            }
            if (inRect(mx, my, delRect)) {
                deleteMining(i);
                return true;
            }
        }
        return false;
    }

    private int miningSectionHeaderY() {
        int base = durabilitySectionHeaderY();
        return base + 14 + 2 + 20 + 12;
    }

    private Rect2i miningModifyButtonRect(int x, int ry, int w) {
        return new Rect2i(x + w - 76, ry + 3, 46, 14);
    }

    private Rect2i miningDeleteButtonRect(int x, int ry, int w) {
        return new Rect2i(x + w - 26, ry + 3, 22, 14);
    }

    private void deleteMining(int index) {
        ensureCurrentRule();
        EditableItemRule rule = currentRule.copy();
        if (index < 0 || index >= rule.getMiningOverrides().size()) {
            return;
        }
        rule.getMiningOverrides().remove(index);
        currentRule = rule.copy();
        selectedMiningIndex = -1;
        status = "Saving..."; statusError = false; statusOk = false;
        EditorNetwork.INSTANCE.sendToServer(new C2SSaveItemRulePacket(EditorJsonPayloads.ruleToPayload(rule)));
        closeDrawer();
        layoutDirty = true;
    }

    //  Utilidades de filtrado
    // ══════════════════════════════════════════════════════════════════════

    private List<AttributeOption> filteredAttrOptions() {
        String q = drawerAttrSearch == null ? "" : drawerAttrSearch.getValue().trim().toLowerCase(Locale.ROOT);
        if (!attrFilterDirty && q.equals(attrFilterQuery)) return filteredAttrs;
        filteredAttrs.clear();
        for (AttributeOption o : attributes) {
            if (q.isBlank() || o.searchKey().contains(q)) filteredAttrs.add(o);
        }
        attrFilterQuery = q; attrFilterDirty = false;
        return filteredAttrs;
    }

    private int selectedAttrIndex(List<AttributeOption> opts) {
        String raw = drawerAttrBox == null ? "" : drawerAttrBox.getValue().trim();
        if (raw.isBlank()) return -1;
        for (int i = 0; i < opts.size(); i++)
            if (opts.get(i).id().toString().equalsIgnoreCase(raw)) return i;
        return -1;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers de renderizado
    // ══════════════════════════════════════════════════════════════════════

    private int renderSectionHeader(GuiGraphics g, int x, int y, int w, String title) {
        g.fill(x, y, x + w, y + 1, BORDER_SOFT);
        g.fill(x, y + 2, x + 3, y + 14, ACCENT);
        g.drawString(font, title, x + 8, y + 3, TEXT_DIM, false);
        return y + 14 + 2;
    }

    private void drawFieldLabel(GuiGraphics g, String label, int x, int inputY) {
        g.drawString(font, label, x, inputY - 9 - 2, TEXT_DIM, false);
    }

    private void renderInputBg(GuiGraphics g, int x, int y, int w, int h, boolean enabled) {
        g.fill(x, y, x + w, y + h, enabled ? INPUT_BG : PANEL_DIM);
        drawBorder(g, x, y, w, h, enabled ? BORDER : BORDER_SOFT);
    }

    private void drawSmallButton(GuiGraphics g, Rect2i r, String label, boolean enabled, boolean hover) {
        int border = !enabled ? BORDER_SOFT : (hover ? ACCENT : BORDER);
        int color  = !enabled ? TEXT_MUTED  : ACCENT;
        int bg     = hover && enabled ? 0xFF182028 : INPUT_BG;
        g.fill(r.getX(), r.getY(), r.getX() + r.getWidth(), r.getY() + r.getHeight(), bg);
        drawBorder(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), border);
        g.drawString(font, label,
                     r.getX() + Math.max(3, (r.getWidth() - font.width(label)) / 2),
                     centeredTextY(r.getY(), r.getHeight()), color, false);
    }

    private void drawDeleteButton(GuiGraphics g, Rect2i r, boolean hover) {
        int border = hover ? ERROR : BORDER_SOFT;
        int color  = hover ? ERROR : TEXT_MUTED;
        int bg     = hover ? 0xFF24171B : INPUT_BG;
        g.fill(r.getX(), r.getY(), r.getX() + r.getWidth(), r.getY() + r.getHeight(), bg);
        drawBorder(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), border);
        g.drawString(font, "X",
                     r.getX() + Math.max(1, (r.getWidth() - font.width("X")) / 2),
                     centeredTextY(r.getY(), r.getHeight()), color, false);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawScrollbar(GuiGraphics g, int x, int y1, int y2,
                                int scroll, int maxScroll, int total, int visible) {
        int trackH = Math.max(1, y2 - y1);
        int thumbH = Math.max(14, trackH * visible / Math.max(visible, total));
        int thumbY = y1 + (maxScroll > 0 ? (trackH - thumbH) * scroll / maxScroll : 0);
        g.fill(x, y1, x + 2, y2, BORDER_SOFT);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, TEXT_DIM);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers generales
    // ══════════════════════════════════════════════════════════════════════

    private void ensureCurrentRule() {
        if (selectedItem != null && (currentRule == null || !selectedItem.id().equals(currentRule.getTargetId()))) {
            currentRule = new EditableItemRule(selectedItem.id(), false);
            rowsDirty = true;
        }
    }

    private boolean hasRuleOverrides() {
        return currentRule != null && (!currentRule.getAttributes().isEmpty()
                || currentRule.getDurability() != null
                || !currentRule.getMiningOverrides().isEmpty());
    }

    private void setStatus(String msg, boolean error) {
        status = msg; statusError = error; statusOk = !error;
    }

    // ── Formato de valores ────────────────────────────────────────────────

    private String effectDisplay(Double amount, EditableOperationType op, EditableAttributeAction action) {
        if (action == EditableAttributeAction.REMOVE) return "removed";
        if (amount == null) return "—";
        boolean pct = op == EditableOperationType.MULTIPLY_BASE || op == EditableOperationType.MULTIPLY_TOTAL;
        return pct ? formatPercent(amount) : formatAmount(amount);
    }

    private String formatAmount(double v) {
        if (!Double.isFinite(v)) return Double.toString(v);
        BigDecimal raw = BigDecimal.valueOf(v).stripTrailingZeros();
        String s = raw.toPlainString();
        if (s.length() <= 10) return s;
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatPercent(double v) {
        return formatAmount(v * 100.0) + "%";
    }

    private String formatAmountForInput(Double v, EditableOperationType op) {
        if (v == null) return "";
        boolean pct = op == EditableOperationType.MULTIPLY_BASE || op == EditableOperationType.MULTIPLY_TOTAL;
        return pct ? formatPercent(v) : formatAmount(v);
    }

    private Double parseAmountInput(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        boolean pct = raw.endsWith("%");
        String num  = raw.replace("%", "").trim();
        double val  = Double.parseDouble(num);
        return draft.isPercentual() || pct ? val / 100.0 : val;
    }

    private boolean isAmountInput(String v) {
        if (draft.draftSource() == DraftSource.DURABILITY) {
            return v.isBlank() || v.matches("\\d*");
        }
        return v.isBlank() || v.matches("-?\\d*(\\.\\d*)?%?");
    }

    private boolean isTierInput(String v) {
        return v.isBlank() || v.matches("[A-Za-z0-9_:.\\-]*");
    }

    private Integer parseDurabilityInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            return value >= 1 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Nombres de atributos ──────────────────────────────────────────────

    private Float parseMiningSpeed(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Float.parseFloat(raw);
    }

    private float selectedItemBaseMiningSpeed() {
        if (selectedItem == null) {
            return 1.0f;
        }
        Item item = ForgeRegistries.ITEMS.getValue(selectedItem.id());
        if (item instanceof TieredItem tiered) {
            return tiered.getTier().getSpeed();
        }
        return 1.0f;
    }

    private boolean isSupportedTier(String tier) {
        if (tier == null) {
            return false;
        }
        String normalized = normalizeTierInput(tier);
        if (miningTiers.isEmpty()) {
            return true;
        }
        return miningTiers.stream().anyMatch(registered -> normalizeTierInput(registered).equals(normalized));
    }

    private int selectedMiningTierIndex() {
        String raw = drawerMiningTierBox == null ? "" : drawerMiningTierBox.getValue().trim();
        if (raw.isBlank()) {
            return -1;
        }
        String normalized = normalizeTierInput(raw);
        for (int i = 0; i < miningTiers.size(); i++) {
            if (normalizeTierInput(miningTiers.get(i)).equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeTierInput(String tier) {
        String normalized = tier == null ? "" : tier.trim().toLowerCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "wooden" -> "wood";
            case "golden" -> "gold";
            default -> normalized;
        };
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private String truncateTierList() {
        if (miningTiers.size() <= 4) {
            return String.join(", ", miningTiers);
        }
        return String.join(", ", miningTiers.subList(0, 4)) + " +" + (miningTiers.size() - 4);
    }

    private String attrDisplayName(ResourceLocation id) {
        if (id == null) return "Unknown";
        for (AttributeOption o : attributes) {
            if (o.id().equals(id) && !o.translatedName().isBlank()
                    && !o.translatedName().equals(o.descriptionId())
                    && !o.translatedName().equals(id.toString()))
                return o.translatedName();
        }
        return titleCasePath(id.getPath());
    }

    private String titleCasePath(String path) {
        String s = (path == null ? "" : path)
                .replace("generic.", "").replace("player.", "")
                .replace('.', ' ').replace('_', ' ').trim();
        if (s.isEmpty()) return "Attribute";
        StringBuilder sb = new StringBuilder(s.length());
        boolean up = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) { sb.append(' '); up = true; }
            else if (up)                   { sb.append(Character.toUpperCase(c)); up = false; }
            else                           { sb.append(c); }
        }
        return sb.toString();
    }

    // ── Claves estructurales ──────────────────────────────────────────────

    private String structKey(ResourceLocation id, EditableOperationType op,
                              EditableSlotType slotType, String slot) {
        return Objects.toString(id, "") + "|" + Objects.toString(op, "") + "|"
                + Objects.toString(slotType, "") + "|" + Objects.toString(slot, "");
    }

    // ── Geometría ─────────────────────────────────────────────────────────

    private int centeredTextY(int y, int h) { return y + Math.max(0, (h - font.lineHeight) / 2); }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean inRect(double mx, double my, Rect2i r) {
        return inRect(mx, my, r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private String truncatePx(String s, int maxPx) {
        if (s == null || s.isEmpty() || font.width(s) <= maxPx) return s == null ? "" : s;
        String ell = "...";
        int max = Math.max(0, maxPx - font.width(ell));
        String r = s;
        while (!r.isEmpty() && font.width(r) > max) r = r.substring(0, r.length() - 1);
        return r + ell;
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
