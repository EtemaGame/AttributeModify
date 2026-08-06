package com.etema.attributemodify.compat;

import com.google.gson.JsonObject;

public record DecorativeItemPolicy(
        boolean blockAllEffects,
        boolean clearExistingEffects,
        boolean blockAttack,
        boolean blockUse) {
    public static final DecorativeItemPolicy DEFAULT = new DecorativeItemPolicy(true, true, true, true);
    public static final DecorativeItemPolicy DISABLED = new DecorativeItemPolicy(false, false, false, false);

    public static DecorativeItemPolicy fromItemJson(JsonObject itemData) {
        if (itemData == null || !itemData.has("decorative_options")
                || !itemData.get("decorative_options").isJsonObject()) {
            return DEFAULT;
        }

        JsonObject options = itemData.getAsJsonObject("decorative_options");
        return new DecorativeItemPolicy(
                booleanOption(options, "block_all_effects", true),
                booleanOption(options, "clear_existing_effects", true),
                booleanOption(options, "block_attack", true),
                booleanOption(options, "block_use", true));
    }

    private static boolean booleanOption(JsonObject options, String name, boolean fallback) {
        if (!options.has(name) || !options.get(name).isJsonPrimitive()
                || !options.get(name).getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return options.get(name).getAsBoolean();
    }
}
