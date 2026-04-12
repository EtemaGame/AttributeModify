package com.etema.attributemodify.integration;

import com.etema.attributemodify.AttributeModify;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public class CuriosIntegration {

    private static boolean curiosLoaded = false;
    private static boolean integrationAttempted = false;

    public static void initialize() {
        if (integrationAttempted)
            return;
        integrationAttempted = true;

        curiosLoaded = ModList.get().isLoaded("curios");

        if (curiosLoaded) {
            try {
                Class<?> handlerClass = Class.forName("com.etema.attributemodify.integration.CuriosEventHandler");
                Object handler = handlerClass.getDeclaredConstructor().newInstance();
                MinecraftForge.EVENT_BUS.register(handler);

                AttributeModify.LOGGER.info("Curios detected - event integration enabled successfully");
            } catch (Exception e) {
                AttributeModify.LOGGER.warn("Failed to initialize Curios integration: {}", e.getMessage());
                curiosLoaded = false;
            }
        } else {
            // AttributeModify.LOGGER.info("Curios not found - standard equipment slots
            // only");
        }
    }

    public static boolean isCuriosLoaded() {
        return curiosLoaded;
    }

    public static boolean shouldProcessCuriosSlots() {
        return curiosLoaded;
    }
}
