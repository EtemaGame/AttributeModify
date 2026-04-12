package com.etema.attributemodify.durability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

public class DurabilityHelper {
    public static final String NBT_MAX = "attributemodify.custom_durability_max";
    public static final String NBT_DAMAGE = "attributemodify.custom_durability_damage";
    public static final String NBT_INITIALIZED = "attributemodify.custom_durability_initialized";

    public static boolean isVanillaDamageableTarget(Item item) {
        return item.getDefaultInstance().isDamageableItem() || item.getMaxDamage(item.getDefaultInstance()) > 0;
    }

    public static DurabilityMode resolveMode(Item item, int durability) {
        if (isVanillaDamageableTarget(item)) {
            return DurabilityMode.VANILLA_OVERRIDE;
        }
        return DurabilityMode.CUSTOM;
    }

    public static boolean isCustomDurabilitySupported(Item item) {
        if (item == null) {
            return false;
        }
        return !isVanillaDamageableTarget(item) && item.getDefaultInstance().getMaxStackSize() == 1;
    }

    public static boolean isCustomDurabilitySupported(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return !isVanillaDamageableTarget(stack.getItem()) && stack.getMaxStackSize() == 1;
    }

    public static boolean hasCustomDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }

        return tag.contains(NBT_INITIALIZED, Tag.TAG_BYTE)
                && tag.getBoolean(NBT_INITIALIZED)
                && tag.contains(NBT_MAX, Tag.TAG_INT)
                && tag.getInt(NBT_MAX) > 0
                && tag.contains(NBT_DAMAGE, Tag.TAG_INT);
    }

    public static boolean usesCustomDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        DurabilityRule rule = com.etema.attributemodify.ItemAttributeDataManager.getInstance()
                .getDurabilityRule(stack.getItem());
        return rule != null && rule.mode() == DurabilityMode.CUSTOM;
    }

    public static int getCustomMaxDurability(ItemStack stack) {
        if (!hasCustomDurability(stack)) {
            return 0;
        }
        return Math.max(0, stack.getTag().getInt(NBT_MAX));
    }

    public static int getCustomDamage(ItemStack stack) {
        if (!hasCustomDurability(stack)) {
            return 0;
        }
        return clamp(stack.getTag().getInt(NBT_DAMAGE), 0, getCustomMaxDurability(stack));
    }

    public static int getCustomRemaining(ItemStack stack) {
        int max = getCustomMaxDurability(stack);
        return Math.max(0, max - getCustomDamage(stack));
    }

    public static boolean isCustomBroken(ItemStack stack) {
        int max = getCustomMaxDurability(stack);
        return max > 0 && getCustomDamage(stack) >= max;
    }

    public static void setCustomMax(ItemStack stack, int max) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        int sanitizedMax = Math.max(1, max);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_MAX, sanitizedMax);
        tag.putInt(NBT_DAMAGE, clamp(tag.getInt(NBT_DAMAGE), 0, sanitizedMax));
        tag.putBoolean(NBT_INITIALIZED, true);
    }

    public static void setCustomDamage(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        int max = tag.contains(NBT_MAX, Tag.TAG_INT) ? Math.max(1, tag.getInt(NBT_MAX)) : Integer.MAX_VALUE;
        tag.putInt(NBT_DAMAGE, clamp(value, 0, max));
        if (tag.contains(NBT_MAX, Tag.TAG_INT)) {
            tag.putBoolean(NBT_INITIALIZED, true);
        }
    }

    public static boolean damageCustom(ItemStack stack, int amount) {
        return damageCustom(stack, amount, null, null);
    }

    public static boolean damageCustom(ItemStack stack, int amount, LivingEntity entity, InteractionHand hand) {
        if (!hasCustomDurability(stack)) {
            return false;
        }

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }

        if (!hasCustomDurability(stack)) {
            return false;
        }

        if (amount <= 0) {
            return isCustomBroken(stack);
        }

        int max = getCustomMaxDurability(stack);
        if (max <= 0) {
            return false;
        }

        int newDamage = clamp(getCustomDamage(stack) + amount, 0, max);
        setCustomDamage(stack, newDamage);
        if (newDamage >= max) {
            breakCustom(stack, entity, hand);
            return true;
        }

        return false;
    }

    public static boolean repairCustom(ItemStack stack, int amount) {
        if (!hasCustomDurability(stack) || amount <= 0) {
            return false;
        }

        int oldDamage = getCustomDamage(stack);
        int newDamage = clamp(oldDamage - amount, 0, getCustomMaxDurability(stack));
        if (newDamage == oldDamage) {
            return false;
        }

        setCustomDamage(stack, newDamage);
        return true;
    }

    public static void ensureCustomDurabilityState(ItemStack stack, DurabilityRule rule) {
        if (stack == null || stack.isEmpty() || rule == null || rule.mode() != DurabilityMode.CUSTOM) {
            return;
        }

        if (!isCustomDurabilitySupported(stack)) {
            return;
        }

        int ruleMax = Math.max(1, rule.maxDurability());
        if (!hasCustomDurability(stack)) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(NBT_MAX, ruleMax);
            tag.putInt(NBT_DAMAGE, 0);
            tag.putBoolean(NBT_INITIALIZED, true);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        int storedMax = Math.max(1, tag.getInt(NBT_MAX));
        int storedDamage = clamp(tag.getInt(NBT_DAMAGE), 0, storedMax);
        int migratedDamage = storedMax == ruleMax ? storedDamage : scaleDamage(storedDamage, storedMax, ruleMax);

        tag.putInt(NBT_MAX, ruleMax);
        tag.putInt(NBT_DAMAGE, clamp(migratedDamage, 0, ruleMax));
        tag.putBoolean(NBT_INITIALIZED, true);
    }

    public static void initCustomDurabilityIfNeeded(ItemStack stack, DurabilityRule rule) {
        if (!hasCustomDurability(stack)) {
            ensureCustomDurabilityState(stack, rule);
        }
    }

    public static void breakCustom(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack original = stack.copyWithCount(1);
        stack.shrink(1);

        if (entity instanceof Player player) {
            ForgeEventFactory.onPlayerDestroyItem(player, original, hand);
        }
    }

    private static int scaleDamage(int damage, int oldMax, int newMax) {
        if (newMax <= 0) {
            return 0;
        }
        if (damage <= 0 || oldMax <= 0) {
            return 0;
        }
        if (damage >= oldMax) {
            return newMax;
        }

        return clamp((int) Math.round((double) damage * (double) newMax / (double) oldMax), 0, newMax);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
