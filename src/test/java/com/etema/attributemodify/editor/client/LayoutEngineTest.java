package com.etema.attributemodify.editor.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutEngineTest {
    @Test
    void computeLayoutProducesDrawerAndCompactVariants() {
        EditorLayout wide = LayoutEngine.computeLayout(1280, 900, true);
        EditorLayout narrow = LayoutEngine.computeLayout(720, 540, false);

        assertTrue(wide.drawerOpen());
        assertFalse(narrow.drawerOpen());
        assertTrue(wide.contentBottom() > wide.contentTop());
        assertTrue(narrow.contentBottom() > narrow.contentTop());
        assertTrue(wide.editorPanel().getWidth() > narrow.editorPanel().getWidth());
    }
}
