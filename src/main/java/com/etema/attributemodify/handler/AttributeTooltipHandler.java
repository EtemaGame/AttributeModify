package com.etema.attributemodify.handler;

import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.service.TooltipPresentationService;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Customizes attribute tooltip lines.
 * Now delegates logic to TooltipPresentationService for better separation of concerns.
 */
public class AttributeTooltipHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) {
            return;
        }

        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        if (!dataManager.hasCustomAttributes(itemStack.getItem())) {
            return;
        }

        TooltipPresentationService.handleTooltip(event);
    }
}
