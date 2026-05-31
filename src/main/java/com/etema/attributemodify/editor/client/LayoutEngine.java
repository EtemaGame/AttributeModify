package com.etema.attributemodify.editor.client;

import net.minecraft.client.renderer.Rect2i;

final class LayoutEngine {
    private LayoutEngine() {}

    // Dimensiones fijas del chrome
    static final int TOP_BAR_H      = 26;
    static final int STATUS_BAR_H   = 20;
    static final int PANEL_GAP      = 6;
    static final int OUTER_MARGIN   = 8;
    static final int PANEL_HEADER_H = 28;
    static final int EDITOR_FOOTER_H = 22;

    // Drawer de edición
    static final int DRAWER_W       = 320;
    static final int DRAWER_PADDING = 10;
    static final int DRAWER_FIELD_H = 17;
    static final int DRAWER_LABEL_H = 9;
    static final int DRAWER_GAP     = 10;
    static final int DRAWER_BUTTON_H = 16;

    static EditorLayout computeLayout(int screenW, int screenH, boolean drawerOpen) {
        // Panel de lista de ítems
        int listW = clamp((int) Math.round(screenW * 0.235), 184, 280);
        int panelY = TOP_BAR_H + PANEL_GAP;
        int panelH = Math.max(180, screenH - panelY - STATUS_BAR_H - PANEL_GAP);

        Rect2i listPanel   = new Rect2i(OUTER_MARGIN, panelY, listW, panelH);
        int editorX = OUTER_MARGIN + listW + PANEL_GAP;
        int editorW = Math.max(260, screenW - editorX - OUTER_MARGIN);
        Rect2i editorPanel = new Rect2i(editorX, panelY, editorW, panelH);

        // Zona de contenido dentro del editor panel
        int contentTop    = panelY + PANEL_HEADER_H + 4;
        int contentBottom = panelY + panelH - EDITOR_FOOTER_H - 2;

        // Lista de atributos ocupa todo el ancho del editor (menos padding)
        int attrListX = editorX + 6;
        int attrListW = editorW - 12;

        // Drawer lateral (aparece sobre el panel derecho)
        int drawerX = 0, drawerY = 0, drawerW = 0, drawerH = 0;
        int drawerFieldAttrY = 0, drawerFieldValueY = 0, drawerFieldTypeY = 0, drawerButtonsY = 0;

        if (drawerOpen) {
            drawerW = Math.min(DRAWER_W, editorW - 20);
            drawerH = panelH - EDITOR_FOOTER_H - 4;
            drawerX = editorX + editorW - drawerW - 4;
            drawerY = panelY + 2;

            int cy = drawerY + DRAWER_PADDING + PANEL_HEADER_H;
            // Atributo
            drawerFieldAttrY  = cy + DRAWER_LABEL_H + 2;
            cy = drawerFieldAttrY + DRAWER_FIELD_H + DRAWER_GAP;
            // Valor
            drawerFieldValueY = cy + DRAWER_LABEL_H + 2;
            cy = drawerFieldValueY + DRAWER_FIELD_H + DRAWER_GAP;
            // Tipo (exacto / porcentual)
            drawerFieldTypeY  = cy + DRAWER_LABEL_H + 2;
            cy = drawerFieldTypeY + DRAWER_FIELD_H + DRAWER_GAP + 4;
            // Botones Apply / Cancel
            drawerButtonsY = cy;
        }

        return new EditorLayout(
                listPanel, editorPanel,
                contentTop, contentBottom,
                attrListX, attrListW,
                drawerOpen,
                drawerX, drawerY, drawerW, drawerH,
                drawerFieldAttrY, drawerFieldValueY, drawerFieldTypeY, drawerButtonsY
        );
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
