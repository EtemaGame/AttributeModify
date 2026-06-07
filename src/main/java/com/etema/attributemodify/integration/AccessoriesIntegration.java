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
                AttributeModify.LOGGER.debug("Accessories detected - event integration enabled successfully");
            } catch (Exception e) {
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
