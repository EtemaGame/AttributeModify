package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado del borrador de edición de un atributo.
 *
 * Desde la UX el usuario solo ve:
 *   - qué atributo está editando
 *   - valor numérico
 *   - tipo: EXACTO (ADDITION) o PORCENTUAL (MULTIPLY_BASE)
 *   - si es una nueva adición o una edición de existente
 *
 * La distinción ADD/MODIFY/SET interna se resuelve automáticamente.
 */
final class EditorDraftState {

    /** Origen del borrador en curso */
    private DraftSource draftSource = DraftSource.NONE;

    /** Índice en la lista de modifiers del rule (−1 = nuevo) */
    private int selectedModifierIndex = -1;

    /** Índice en baseAttributes (−1 = no aplica) */
    private int selectedBaseAttributeIndex = -1;

    /**
     * Tipo de operación simplificado:
     *   ADDITION       → "Exacto"
     *   MULTIPLY_BASE  → "Porcentual"
     */
    private EditableOperationType operationType = EditableOperationType.ADDITION;

    /**
     * Acción interna. El usuario nunca la elige directamente:
     *   - Al añadir nuevo: ADD
     *   - Al editar base/modifier existente: MODIFY
     *   - Al eliminar: REMOVE (gestionado fuera del draft)
     */
    private EditableAttributeAction action = EditableAttributeAction.ADD;

    private EditableSlotType slotType = EditableSlotType.AUTO;
    private String slot = "mainhand";

    // ── getters/setters ────────────────────────────────────────────────────

    DraftSource draftSource()                    { return draftSource; }
    void        draftSource(DraftSource v)       { draftSource = v == null ? DraftSource.NONE : v; }

    int  selectedModifierIndex()                 { return selectedModifierIndex; }
    void selectedModifierIndex(int v)            { selectedModifierIndex = v; }

    int  selectedBaseAttributeIndex()            { return selectedBaseAttributeIndex; }
    void selectedBaseAttributeIndex(int v)       { selectedBaseAttributeIndex = v; }

    EditableOperationType operationType()        { return operationType; }
    void operationType(EditableOperationType v)  { operationType = v == null ? EditableOperationType.ADDITION : v; }

    /** Indica si el tipo es porcentual (MULTIPLY_BASE o MULTIPLY_TOTAL). */
    boolean isPercentual() {
        return operationType == EditableOperationType.MULTIPLY_BASE
                || operationType == EditableOperationType.MULTIPLY_TOTAL;
    }

    EditableAttributeAction action()             { return action; }
    void action(EditableAttributeAction v)       { action = v == null ? EditableAttributeAction.ADD : v; }

    EditableSlotType slotType()                  { return slotType; }
    void slotType(EditableSlotType v)            { slotType = v == null ? EditableSlotType.AUTO : v; }

    String slot()                                { return slot; }
    void   slot(String v)                        { slot = (v == null || v.isBlank()) ? "mainhand" : v; }

    boolean hasActiveDraft()                     { return draftSource != DraftSource.NONE; }

    // ── operaciones ────────────────────────────────────────────────────────

    void resetDraft() {
        draftSource             = DraftSource.NONE;
        selectedModifierIndex   = -1;
        selectedBaseAttributeIndex = -1;
        operationType           = EditableOperationType.ADDITION;
        action                  = EditableAttributeAction.ADD;
        slotType                = EditableSlotType.AUTO;
        slot                    = "mainhand";
    }

    /** Inicia un nuevo atributo desde el registry (botón + Add). */
    void startNewAttributeDraft() {
        resetDraft();
        draftSource = DraftSource.REGISTRY;
        action      = EditableAttributeAction.ADD;
    }

    /** Carga un atributo base (vanilla/mod) para editarlo. */
    void loadBaseAttributeIntoDraft(ResourceLocation attributeId, double amount,
                                     EditableOperationType op, String slotValue) {
        resetDraft();
        draftSource              = DraftSource.BASE;
        action                   = EditableAttributeAction.MODIFY;
        operationType            = op == null ? EditableOperationType.ADDITION : op;
        slotType                 = EditableSlotType.STANDARD;
        slot                     = (slotValue == null || slotValue.isBlank()) ? "mainhand" : slotValue;
    }

    /** Carga un modifier existente (ya guardado en el rule) para editarlo. */
    void loadModifierIntoDraft(EditableAttributeModifier modifier) {
        if (modifier == null) return;
        resetDraft();
        draftSource              = DraftSource.MODIFIER;
        action                   = modifier.getAction() == null ? EditableAttributeAction.MODIFY : modifier.getAction();
        operationType            = modifier.getOperation() == null ? EditableOperationType.ADDITION : modifier.getOperation();
        slotType                 = modifier.getSlotType() == null ? EditableSlotType.AUTO : modifier.getSlotType();
        slot                     = (modifier.getSlot() == null || modifier.getSlot().isBlank()) ? "mainhand" : modifier.getSlot();
    }

    /**
     * Devuelve la acción efectiva que se debe serializar.
     * Simplifica la lógica que antes vivía dispersa en la screen.
     */
    EditableAttributeAction effectiveAction() {
        return action;
    }

    /**
     * Devuelve la operación efectiva.
     * Si el tipo es porcentual se usa MULTIPLY_BASE por defecto.
     */
    EditableOperationType effectiveOperation() {
        return operationType;
    }
}
