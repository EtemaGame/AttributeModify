package com.etema.attributemodify.editor;

public final class EditorClientState {
    private static String latestCatalogJson = "{}";
    private static String latestRuleJson = "{}";
    private static String latestSaveResultJson = "{}";

    private EditorClientState() {
    }

    public static String latestCatalogJson() {
        return latestCatalogJson;
    }

    public static void setLatestCatalogJson(String latestCatalogJson) {
        EditorClientState.latestCatalogJson = latestCatalogJson;
    }

    public static String latestRuleJson() {
        return latestRuleJson;
    }

    public static void setLatestRuleJson(String latestRuleJson) {
        EditorClientState.latestRuleJson = latestRuleJson;
    }

    public static String latestSaveResultJson() {
        return latestSaveResultJson;
    }

    public static void setLatestSaveResultJson(String latestSaveResultJson) {
        EditorClientState.latestSaveResultJson = latestSaveResultJson;
    }
}
