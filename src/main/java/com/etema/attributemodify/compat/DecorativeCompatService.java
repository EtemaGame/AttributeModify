package com.etema.attributemodify.compat;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class DecorativeCompatService {
    private DecorativeCompatService() {
    }

    public static boolean isKnownCompatItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && DecorativeCompatRegistry.isKnownDecorativeFamily(id.getNamespace());
    }

    public static boolean shouldSuppressEffects(LivingEntity entity) {
        return entity != null && ItemAttributeDataManager.getInstance().hasEffectBlockingDecorativeEquipped(entity);
    }

    public static boolean shouldClearEffects(LivingEntity entity) {
        return entity != null && ItemAttributeDataManager.getInstance().hasEffectClearingDecorativeEquipped(entity);
    }

    public static boolean shouldBlockAttack(ItemStack stack) {
        return isDecorative(stack) && ItemAttributeDataManager.getInstance()
                .getDecorativePolicy(stack.getItem()).blockAttack();
    }

    public static boolean shouldBlockUse(ItemStack stack) {
        return isDecorative(stack) && ItemAttributeDataManager.getInstance()
                .getDecorativePolicy(stack.getItem()).blockUse();
    }

    private static boolean isDecorative(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && ItemAttributeDataManager.getInstance().isDecorative(stack.getItem());
    }

    public static String describeKnownFamily(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return null;
        }

        KnownDecorativeCompatProfile profile = DecorativeCompatRegistry.get(id.getNamespace());
        return profile != null ? profile.displayName() : null;
    }
}
