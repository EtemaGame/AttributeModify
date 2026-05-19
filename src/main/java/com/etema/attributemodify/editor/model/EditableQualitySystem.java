package com.etema.attributemodify.editor.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EditableQualitySystem {
    private String tagPath = "quality";
    private final Set<String> triggers = new LinkedHashSet<>();
    private final List<EditableQualityLevel> levels = new ArrayList<>();

    public String getTagPath() {
        return tagPath;
    }

    public void setTagPath(String tagPath) {
        this.tagPath = tagPath == null || tagPath.isBlank() ? "quality" : tagPath;
    }

    public Set<String> getTriggers() {
        return triggers;
    }

    public List<EditableQualityLevel> getLevels() {
        return levels;
    }

    public EditableQualitySystem copy() {
        EditableQualitySystem copy = new EditableQualitySystem();
        copy.setTagPath(tagPath);
        copy.getTriggers().addAll(triggers);
        for (EditableQualityLevel level : levels) {
            copy.getLevels().add(level.copy());
        }
        return copy;
    }
}
