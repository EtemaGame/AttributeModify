package com.etema.attributemodify.editor.client;

import net.minecraft.client.renderer.Rect2i;

/**
 * Resultado del cálculo de layout para un frame dado.
 *
 * listPanel  — panel izquierdo con la lista de ítems
 * editorPanel — panel derecho con header + lista de atributos
 * drawerOpen  — si el drawer lateral de edición está visible
 * drawerX/Y/W/H — coordenadas del drawer de edición
 * contentTop/Bottom — zona de scroll de la lista de atributos
 * headerH — altura del header del item seleccionado
 */
record EditorLayout(
        Rect2i listPanel,
        Rect2i editorPanel,
        int contentTop,
        int contentBottom,
        int attrListX,
        int attrListW,
        boolean drawerOpen,
        int drawerX,
        int drawerY,
        int drawerW,
        int drawerH,
        int drawerFieldAttrY,
        int drawerFieldValueY,
        int drawerFieldTypeY,
        int drawerButtonsY
) {
}
