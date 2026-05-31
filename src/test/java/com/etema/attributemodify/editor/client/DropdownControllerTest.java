package com.etema.attributemodify.editor.client;

import net.minecraft.client.renderer.Rect2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DropdownControllerTest {
    @Test
    void toggleAndCloseResetScrollAndSelection() {
        DropdownController controller = new DropdownController();

        controller.dropdownScroll(7);
        controller.toggleDropdown(OpenDropdown.ATTRIBUTE);

        assertEquals(OpenDropdown.ATTRIBUTE, controller.openDropdown());
        assertEquals(0, controller.dropdownScroll());

        controller.dropdownScroll(3);
        controller.toggleDropdown(OpenDropdown.ATTRIBUTE);

        assertEquals(OpenDropdown.NONE, controller.openDropdown());
        assertEquals(0, controller.dropdownScroll());
    }

    @Test
    void boundsClampMenuHeightWithinScreenSpace() {
        DropdownController controller = new DropdownController();
        Rect2i anchor = new Rect2i(20, 80, 120, 15);
        Rect2i editor = new Rect2i(10, 30, 500, 260);

        DropdownController.DropdownBounds bounds = controller.dropdownBounds(anchor, 10, 400, 26, 20, editor, 24);

        assertEquals(20, bounds.x());
        assertEquals(120, bounds.w());
        assertEquals(4, bounds.visibleRows());
        assertEquals(96, bounds.y());
    }
}
