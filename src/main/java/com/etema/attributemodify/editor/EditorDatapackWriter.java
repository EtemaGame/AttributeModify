package com.etema.attributemodify.editor;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class EditorDatapackWriter {
    public static final String DATAPACK_NAME = "AttributeModify_Editor";
    public static final String EDITOR_NAMESPACE = "attributemodify_editor";
    public static final String RULE_FILE_NAME = "editor_rules.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private EditorDatapackWriter() {
    }

    public static Path editorRulesPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.DATAPACK_DIR)
                .resolve(DATAPACK_NAME)
                .resolve("data")
                .resolve(EDITOR_NAMESPACE)
                .resolve("item_attributes")
                .resolve(RULE_FILE_NAME);
    }

    public static SaveResult write(MinecraftServer server, List<EditableItemRule> rules) {
        return write(editorRulesPath(server), rules);
    }

    public static SaveResult write(Path rulesPath, List<EditableItemRule> rules) {
        try {
            Path datapackRoot = rulesPath.getParent().getParent().getParent().getParent();
            Files.createDirectories(rulesPath.getParent());
            writePackMeta(datapackRoot.resolve("pack.mcmeta"));

            Path backupPath = null;
            if (Files.exists(rulesPath)) {
                backupPath = backupExisting(rulesPath);
            }

            JsonObject document = EditorRuleSerializer.toDocument(rules);
            atomicWrite(rulesPath, GSON.toJson(document));
            return SaveResult.success(rulesPath, backupPath);
        } catch (IOException | RuntimeException e) {
            AttributeModify.LOGGER.error("Failed to write editor datapack rules: {}", e.getMessage(), e);
            return SaveResult.failure(e.getMessage());
        }
    }

    private static void writePackMeta(Path packMetaPath) throws IOException {
        Files.createDirectories(packMetaPath.getParent());
        if (Files.exists(packMetaPath)) {
            return;
        }

        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 15);
        pack.addProperty("description", "AttributeModify generated editor rules");
        root.add("pack", pack);
        atomicWrite(packMetaPath, GSON.toJson(root));
    }

    private static Path backupExisting(Path rulesPath) throws IOException {
        Path backupPath = rulesPath.resolveSibling("editor_rules-" + LocalDateTime.now().format(BACKUP_FORMAT) + ".json.bak");
        Files.copy(rulesPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    private static void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.write(content);
            writer.flush();
        }

        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
            channel.force(true);
        }

        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailed) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record SaveResult(boolean success, Path rulesPath, Path backupPath, String error) {
        public static SaveResult success(Path rulesPath, Path backupPath) {
            return new SaveResult(true, rulesPath, backupPath, null);
        }

        public static SaveResult failure(String error) {
            return new SaveResult(false, null, null, error);
        }
    }
}
