package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableItemRule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EditorRuleRepository {
    private EditorRuleRepository() {
    }

    public static List<EditableItemRule> loadEditorRules(MinecraftServer server) {
        return loadEditorRules(EditorDatapackWriter.editorRulesPath(server));
    }

    public static List<EditableItemRule> loadEditorRules(Path rulesPath) {
        if (!Files.exists(rulesPath)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(rulesPath, StandardCharsets.UTF_8)) {
            JsonObject document = JsonParser.parseReader(reader).getAsJsonObject();
            return new ArrayList<>(EditorRuleSerializer.fromDocument(document));
        } catch (IOException | RuntimeException ignored) {
            return new ArrayList<>();
        }
    }

    public static Optional<EditableItemRule> findRule(List<EditableItemRule> rules, ResourceLocation targetId, boolean tagTarget) {
        return rules.stream()
                .filter(rule -> rule.isTagTarget() == tagTarget)
                .filter(rule -> targetId.equals(rule.getTargetId()))
                .findFirst()
                .map(EditableItemRule::copy);
    }

    public static List<EditableItemRule> replaceRule(List<EditableItemRule> rules, EditableItemRule replacement) {
        List<EditableItemRule> updated = new ArrayList<>();
        boolean replaced = false;

        for (EditableItemRule rule : rules) {
            if (sameTarget(rule, replacement)) {
                updated.add(replacement.copy());
                replaced = true;
            } else {
                updated.add(rule.copy());
            }
        }

        if (!replaced) {
            updated.add(replacement.copy());
        }
        return updated;
    }

    private static boolean sameTarget(EditableItemRule first, EditableItemRule second) {
        return first != null
                && second != null
                && first.isTagTarget() == second.isTagTarget()
                && first.getTargetId() != null
                && first.getTargetId().equals(second.getTargetId());
    }
}
