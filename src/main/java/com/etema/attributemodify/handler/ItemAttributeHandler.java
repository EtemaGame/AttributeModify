package com.etema.attributemodify.handler;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.service.AttributeApplicationService;
import com.etema.attributemodify.service.AttributeResolutionService;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.List;

public class ItemAttributeHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty() || !AttributeResolutionService.hasCustomAttributes(itemStack)) {
            return;
        }

        if (AttributeResolutionService.isDecorative(itemStack)) {
            event.clearModifiers();
            return;
        }

        try {
            List<ItemAttributeDataManager.AttributeEntry> entries = 
                AttributeResolutionService.resolveEntries(itemStack, event.getSlotType());

            if (!entries.isEmpty()) {
                AttributeApplicationService.applyRules(event, entries);
            }
        } catch (Exception e) {
            AttributeModify.LOGGER.error("[Handler] Error delegating attribute application for {} in slot {}: {}",
                    itemStack.getItem(), event.getSlotType(), e.getMessage(), e);
        }
    }

}
