package com.etema.attributemodify.editor.client;

import net.minecraft.client.renderer.Rect2i;

final class DropdownController {
    record DropdownBounds(int x, int y, int w, int h, int visibleRows) {
    }

    record AttributeDropdownBounds(int x, int y, int w, int h, int visibleRows, int searchY, int listY, int listH) {
    }

    private OpenDropdown openDropdown = OpenDropdown.NONE;
    private int dropdownScroll;

    OpenDropdown openDropdown() {
        return openDropdown;
    }

    int dropdownScroll() {
        return dropdownScroll;
    }

    void dropdownScroll(int value) {
        dropdownScroll = Math.max(0, value);
    }

    boolean isOpen(OpenDropdown target) {
        return openDropdown == target;
    }

    void toggleDropdown(OpenDropdown target) {
        if (openDropdown == target) {
            closeDropdown();
            return;
        }
        openDropdown = target == null ? OpenDropdown.NONE : target;
        dropdownScroll = 0;
    }

    void closeDropdown() {
        openDropdown = OpenDropdown.NONE;
        dropdownScroll = 0;
    }

    DropdownBounds dropdownBounds(Rect2i anchor, int itemCount, int screenHeight, int topBarH, int globalFooterH, Rect2i editorPanel, int editorFooterH) {
        int visibleRows = Math.min(4, itemCount);
        int menuW = anchor.getWidth();
        int menuH = visibleRows * 16;

        int screenBottom = screenHeight - globalFooterH - 2;
        if (editorPanel != null) {
            screenBottom = Math.min(screenBottom, editorPanel.getY() + editorPanel.getHeight() - editorFooterH - 2);
        }

        int screenTop = topBarH + 2;
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
                visibleRows = Math.max(1, availableBelow / 16);
                menuH = visibleRows * 16;
                menuY = belowY;
            } else {
                visibleRows = Math.max(1, availableAbove / 16);
                menuH = visibleRows * 16;
                menuY = anchor.getY() - menuH - 1;
            }
        }

        return new DropdownBounds(anchor.getX(), menuY, menuW, menuH, visibleRows);
    }

    AttributeDropdownBounds attributeDropdownBounds(Rect2i anchor, int itemCount, int screenHeight, int topBarH, int globalFooterH, Rect2i editorPanel, int editorFooterH) {
        int visibleRows = Math.max(1, Math.min(6, itemCount == 0 ? 1 : itemCount));
        int menuW = anchor.getWidth();
        int menuH = 14 + 18 + 6 + visibleRows * 20 + 4;

        int screenBottom = screenHeight - globalFooterH - 2;
        if (editorPanel != null) {
            screenBottom = Math.min(screenBottom, editorPanel.getY() + editorPanel.getHeight() - editorFooterH - 2);
        }

        int screenTop = topBarH + 2;
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
                visibleRows = Math.max(1, availableBelow / 20);
                menuH = 14 + 18 + 6 + visibleRows * 20 + 4;
                menuY = belowY;
            } else {
                visibleRows = Math.max(1, availableAbove / 20);
                menuH = 14 + 18 + 6 + visibleRows * 20 + 4;
                menuY = anchor.getY() - menuH - 1;
            }
        }

        int searchY = menuY + 14 + 3;
        int listY = searchY + 18 + 5;
        int listH = visibleRows * 20;
        return new AttributeDropdownBounds(anchor.getX(), menuY, menuW, menuH, visibleRows, searchY, listY, listH);
    }
}
