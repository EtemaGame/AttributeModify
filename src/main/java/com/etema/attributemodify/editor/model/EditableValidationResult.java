package com.etema.attributemodify.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EditableValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String message) {
        if (message != null && !message.isBlank()) {
            errors.add(message);
        }
    }

    public void warning(String message) {
        if (message != null && !message.isBlank()) {
            warnings.add(message);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
