package com.etema.attributemodify.mixin;

import com.etema.attributemodify.compat.DecorativeCompatService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void attributemodify$blockDecorativeUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (DecorativeCompatService.shouldBlockUse(stack)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void attributemodify$blockDecorativeUseOn(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (DecorativeCompatService.shouldBlockUse(context.getItemInHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "canAttackBlock", at = @At("HEAD"), cancellable = true)
    private void attributemodify$blockDecorativeCanAttackBlock(BlockState state, Level level, BlockPos pos,
            Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DecorativeCompatService.shouldBlockAttack(player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void attributemodify$blockDecorativeHurtEnemy(ItemStack stack, LivingEntity target,
            LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (DecorativeCompatService.shouldBlockAttack(stack)) {
            cir.setReturnValue(false);
        }
    }

}
