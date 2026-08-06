package com.etema.attributemodify.editor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorCatalogPayloadTest {
    @Test
    void metadataCatalogUsesLocalEntriesAndKeepsServerAuthorityData() {
        EditorCatalogService.EditorCatalog catalog = new EditorCatalogService.EditorCatalog(
                List.of(),
                List.of(),
                Set.of("mainhand"),
                Set.of("ring"),
                Set.of(),
                Set.of("minecraft"),
                Set.of(id("forge:tools")),
                Set.of(id("minecraft:diamond")),
                true,
                false);

        JsonObject payload = EditorJsonPayloads.metadataCatalogToJson(catalog);

        assertEquals(EditorCatalogService.HYBRID_SCHEMA_VERSION, payload.get("schemaVersion").getAsInt());
        assertTrue(payload.get("localEntries").getAsBoolean());
        assertEquals(0, payload.getAsJsonArray("items").size());
        assertEquals(0, payload.getAsJsonArray("attributes").size());
        assertEquals("forge:tools", payload.getAsJsonArray("itemTags").get(0).getAsString());
        assertTrue(payload.get("curiosLoaded").getAsBoolean());
        assertFalse(payload.get("accessoriesLoaded").getAsBoolean());
    }

    @Test
    void legacyCatalogRemainsExplicitlyServerBacked() {
        EditorCatalogService.EditorCatalog catalog = new EditorCatalogService.EditorCatalog(
                List.of(new EditorCatalogService.EditorItemInfo(
                        id("minecraft:stick"), "item.minecraft.stick", 0, false, "minecraft")),
                List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, false);

        JsonObject payload = EditorJsonPayloads.catalogToJson(catalog);

        assertEquals(1, payload.get("schemaVersion").getAsInt());
        assertFalse(payload.get("localEntries").getAsBoolean());
        assertEquals(1, payload.getAsJsonArray("items").size());
    }

    private static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException(value);
        }
        return id;
    }
}
