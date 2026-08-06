package com.etema.attributemodify.mixin;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attributemodify$cancelDecorativeAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        ItemStack stack = self.getMainHandItem();
        if (!stack.isEmpty() && ItemAttributeDataManager.getInstance().isDecorative(stack.getItem())) {
            ci.cancel();
        }
    }
}
