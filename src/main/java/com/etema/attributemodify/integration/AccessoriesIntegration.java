package com.etema.attributemodify.integration;

import com.etema.attributemodify.AttributeModify;
import net.minecraftforge.fml.ModList;

public class AccessoriesIntegration {

    private static boolean accessoriesLoaded = false;
    private static boolean integrationAttempted = false;

    public static void initialize() {
        if (integrationAttempted) {
            return;
        }
        integrationAttempted = true;

        accessoriesLoaded = ModList.get().isLoaded("accessories");

        if (accessoriesLoaded) {
            try {
                Class<?> handlerClass = Class.forName("com.etema.attributemodify.integration.AccessoriesEventHandler");
                handlerClass.getDeclaredConstructor().newInstance();
                String version = ModList.get().getModContainerById("accessories")
                        .map(container -> container.getModInfo().getVersion().toString())
                        .orElse("unknown");
                AttributeModify.LOGGER.debug("Accessories detected (version {}) - event integration enabled successfully", version);
            } catch (Throwable e) {
                AttributeModify.LOGGER.warn("Failed to initialize Accessories integration: {}", e.getMessage());
                accessoriesLoaded = false;
            }
        }
    }

    public static boolean isAccessoriesLoaded() {
        return accessoriesLoaded;
    }

    public static boolean shouldProcessAccessoriesSlots() {
        return accessoriesLoaded;
    }
}
