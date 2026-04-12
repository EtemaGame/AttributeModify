package com.etema.attributemodify.mixin;

import com.etema.attributemodify.durability.VanillaDurabilityOverrides;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Unique
    private ItemStack attributemodify$self() {
        return (ItemStack) (Object) this;
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void attributemodify$getMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = attributemodify$self();
        int overridden = VanillaDurabilityOverrides.getEffectiveMaxDamage(self);
        if (overridden > 0) {
            cir.setReturnValue(overridden);
        }
    }

    @Inject(method = "getDamageValue", at = @At("HEAD"), cancellable = true)
    private void attributemodify$getDamageValue(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = attributemodify$self();
        if (!VanillaDurabilityOverrides.hasRule(self)) {
            return;
        }

        int rawStoredDamage = VanillaDurabilityOverrides.getRawStoredDamage(self);
        int effective = VanillaDurabilityOverrides.getEffectiveDamageValue(self, rawStoredDamage);
        cir.setReturnValue(effective);
    }

    @Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
    private void attributemodify$setDamageValue(int damage, CallbackInfo ci) {
        ItemStack self = attributemodify$self();
        if (!VanillaDurabilityOverrides.hasRule(self)) {
            return;
        }

        int normalized = VanillaDurabilityOverrides.normalizeWriteDamage(self, damage);
        self.getOrCreateTag().putInt("Damage", Math.max(0, normalized));
        ci.cancel();
    }
}
