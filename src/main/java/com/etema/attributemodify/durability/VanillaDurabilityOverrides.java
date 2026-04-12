package com.etema.attributemodify.durability;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

public final class VanillaDurabilityOverrides {
    private VanillaDurabilityOverrides() {}

    /**
     * El damage crudo almacenado en la stack pertenece a esta escala.
     * Si falta, asumimos la escala vanilla original del item.
     */
    public static final String NBT_APPLIED_MAX = "attributemodify.vdur.applied_max";
    public static final String NBT_ORIGINAL_MAX = "attributemodify.vdur.original_max";

    public static boolean hasRule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        DurabilityRule rule = ItemAttributeDataManager.getInstance().getDurabilityRule(stack.getItem());
        return rule != null && rule.mode() == DurabilityMode.VANILLA_OVERRIDE;
    }

    public static int getConfiguredMax(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        DurabilityRule rule = ItemAttributeDataManager.getInstance().getDurabilityRule(stack.getItem());
        if (rule == null || rule.mode() != DurabilityMode.VANILLA_OVERRIDE) {
            return 0;
        }

        return Math.max(1, rule.maxDurability());
    }

    /**
     * Lee el damage crudo persistido en NBT sin pasar por getters de ItemStack,
     * porque esos pueden estar interceptados y devolver la escala efectiva.
     */
    public static int getRawStoredDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt("Damage"));
    }

    public static int getOriginalVanillaMax(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_ORIGINAL_MAX, Tag.TAG_INT)) {
            int stored = tag.getInt(NBT_ORIGINAL_MAX);
            if (stored > 0) return stored;
        }

        Item item = stack.getItem();
        int original = ItemAttributeDataManager.getInstance().getOriginalVanillaDurability(item);
        if (original > 0) return original;

        return Math.max(0, item.getMaxDamage(item.getDefaultInstance()));
    }

    /**
     * Devuelve la escala a la que pertenece el damage CRUDO almacenado.
     */
    public static int getStoredAppliedMax(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_APPLIED_MAX, Tag.TAG_INT)) {
            int stored = tag.getInt(NBT_APPLIED_MAX);
            if (stored > 0) return stored;
        }

        return getOriginalVanillaMax(stack);
    }

    /**
     * Max efectivo que debe ver vanilla para esta stack.
     */
    public static int getEffectiveMaxDamage(ItemStack stack) {
        int configured = getConfiguredMax(stack);
        return configured > 0 ? configured : 0;
    }

    /**
     * Damage efectivo visible para la regla actual.
     * No muta la stack.
     */
    public static int getEffectiveDamageValue(ItemStack stack, int rawStoredDamage) {
        if (!hasRule(stack)) {
            return rawStoredDamage;
        }

        int currentMax = getConfiguredMax(stack);
        int storedScale = getStoredAppliedMax(stack);

        if (currentMax <= 0 || storedScale <= 0) {
            return rawStoredDamage;
        }

        if (currentMax == storedScale) {
            return clamp(rawStoredDamage, 0, currentMax);
        }

        return scaleDamage(rawStoredDamage, storedScale, currentMax);
    }

    /**
     * Cuando vanilla o otro mod escriba damage, queremos dejarlo ya
     * en la escala ACTUAL y sellar el applied max correspondiente.
     */
    public static int normalizeWriteDamage(ItemStack stack, int requestedDamage) {
        if (!hasRule(stack)) {
            return requestedDamage;
        }

        int currentMax = getConfiguredMax(stack);
        if (currentMax <= 0) {
            return requestedDamage;
        }

        stampCurrentScale(stack, currentMax);
        return clamp(requestedDamage, 0, currentMax);
    }

    public static void stampCurrentScale(ItemStack stack, int currentMax) {
        if (stack == null || stack.isEmpty() || currentMax <= 0) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_APPLIED_MAX, currentMax);

        int original = getOriginalVanillaMax(stack);
        if (original > 0) {
            tag.putInt(NBT_ORIGINAL_MAX, original);
        }
    }

    /**
     * Útil para migrar stacks cuando las toques en server:
     * convierte el damage CRUDO antiguo a la escala nueva y lo persiste.
     */
    public static void migrateStackIfNeeded(ItemStack stack) {
        if (!hasRule(stack)) {
            return;
        }

        int configured = getConfiguredMax(stack);
        int storedScale = getStoredAppliedMax(stack);
        if (configured <= 0 || storedScale <= 0 || configured == storedScale) {
            stampCurrentScale(stack, configured);
            return;
        }

        int raw = getRawStoredDamage(stack);
        int migrated = scaleDamage(raw, storedScale, configured);

        // setDamageValue quedará interceptado por mixin y estampará la escala actual.
        stack.setDamageValue(migrated);
    }

    private static int scaleDamage(int damage, int oldMax, int newMax) {
        if (newMax <= 0) return 0;
        if (damage <= 0 || oldMax <= 0) return 0;
        if (damage >= oldMax) return newMax;

        return clamp((int) Math.round((double) damage * (double) newMax / (double) oldMax), 0, newMax);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
