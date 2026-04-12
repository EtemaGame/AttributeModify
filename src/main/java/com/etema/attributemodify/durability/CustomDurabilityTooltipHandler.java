package com.etema.attributemodify.durability;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CustomDurabilityTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        DurabilityRule rule = ItemAttributeDataManager.getInstance().getDurabilityRule(stack.getItem());

        if (rule == null || rule.mode() != DurabilityMode.CUSTOM) {
            return;
        }

        if (!DurabilityHelper.isCustomDurabilitySupported(stack)) {
            return;
        }

        DurabilityHelper.ensureCustomDurabilityState(stack, rule);
        if (!DurabilityHelper.hasCustomDurability(stack)) {
            return;
        }

        int max = DurabilityHelper.getCustomMaxDurability(stack);
        int remaining = DurabilityHelper.getCustomRemaining(stack);

        event.getToolTip().add(Component.translatable("attributemodify.tooltip.custom_durability")
                .append(Component.literal(": " + remaining + " / " + max))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
