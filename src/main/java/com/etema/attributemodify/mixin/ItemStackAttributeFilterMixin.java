package com.etema.attributemodify.mixin;

import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.service.AttributeInspectionContext;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackAttributeFilterMixin {

    @Unique
    private ItemStack attributemodify$self() {
        return (ItemStack) (Object) this;
    }

    @Inject(method = "getAttributeModifiers(Lnet/minecraft/world/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;", at = @At("RETURN"), cancellable = true)
    private void attributemodify$filterAttributeModifiers(EquipmentSlot slot,
            CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        ItemStack self = attributemodify$self();
        Multimap<Attribute, AttributeModifier> original = cir.getReturnValue();
        if (self.isEmpty() || slot == null || original == null || original.isEmpty()
                || AttributeInspectionContext.isInspectingExternalAttributes()) {
            return;
        }

        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        boolean changed = false;
        Multimap<Attribute, AttributeModifier> filtered = HashMultimap.create(original);

        for (var entry : original.entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            if (!dataManager.shouldSuppressAttributeModifier(self, slot, attribute, modifier.getOperation())) {
                continue;
            }
            changed |= filtered.remove(attribute, modifier);
        }

        if (changed) {
            cir.setReturnValue(filtered);
        }
    }
}
