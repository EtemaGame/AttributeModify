package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorDatapackWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesSingleGeneratedDatapackAndCleansBackups() throws Exception {
        Path rulesPath = tempDir.resolve("AttributeModify_Editor")
                .resolve("data")
                .resolve("attributemodify_editor")
                .resolve("item_attributes")
                .resolve("editor_rules.json");
        Path backupDir = rulesPath.getParent();

        EditableItemRule rule = new EditableItemRule(EditorRuleValidatorTest.id("minecraft:diamond_sword"), false);
        rule.getAttributes().add(new EditableAttributeModifier(
                EditorRuleValidatorTest.id("minecraft:generic.attack_damage"),
                EditableAttributeAction.MODIFY,
                10.0,
                EditableOperationType.ADDITION,
                EditableSlotType.STANDARD,
                "mainhand"));

        EditorDatapackWriter.SaveResult first = EditorDatapackWriter.write(rulesPath, List.of(rule));
        Files.writeString(rulesPath.resolveSibling("editor_rules-20260520-002901.json.bak"), "{}");
        EditorDatapackWriter.SaveResult second = EditorDatapackWriter.write(rulesPath, List.of(rule));

        assertTrue(first.success(), first.error());
        assertTrue(second.success(), second.error());
        assertTrue(Files.exists(rulesPath));
        assertTrue(Files.exists(tempDir.resolve("AttributeModify_Editor").resolve("pack.mcmeta")));
        assertNull(second.backupPath());

        try (Stream<Path> files = Files.list(backupDir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".bak")));
        }
    }
}
