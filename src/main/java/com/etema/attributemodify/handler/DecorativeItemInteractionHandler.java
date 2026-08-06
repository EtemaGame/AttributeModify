package com.etema.attributemodify.handler;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.compat.DecorativeCompatService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DecorativeItemInteractionHandler {
    private DecorativeItemInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (DecorativeCompatService.shouldBlockAttack(event.getEntity().getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player != null && DecorativeCompatService.shouldBlockAttack(player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteraction(event, event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteraction(event, event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelInteraction(event, event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelInteraction(event, event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (DecorativeCompatService.shouldBlockAttack(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (DecorativeCompatService.shouldBlockUse(event.getItem())) {
            event.setCanceled(true);
        }
    }

    private static void cancelInteraction(PlayerInteractEvent event, ItemStack stack) {
        if (DecorativeCompatService.shouldBlockUse(stack)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

}
