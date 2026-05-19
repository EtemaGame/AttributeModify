package com.etema.attributemodify.durability;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomDurabilityHandler {
    private static final Map<String, Integer> RECENT_TRIGGER_TICKS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ensureDurabilityState(event.getCrafting());
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ensureDurabilityState(event.getItem().getItem());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        scanPlayerInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        String playerPrefix = event.getEntity().getUUID() + "|";
        RECENT_TRIGGER_TICKS.keySet().removeIf(key -> key.startsWith(playerPrefix));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        scanPlayerInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        scanPlayerInventory(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!prepareStackForUse(stack)) {
            return;
        }

        if (event.isCanceled() || isBroken(stack)) {
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(player, InteractionHand.MAIN_HAND, DurabilityRule.TRIGGER_MELEE_HIT);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!prepareStackForUse(stack)) {
            return;
        }

        if (event.isCanceled() || isBroken(stack)) {
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(player, InteractionHand.MAIN_HAND, DurabilityRule.TRIGGER_BLOCK_BREAK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }

        if (!allowInteraction(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(player, event.getHand(), DurabilityRule.TRIGGER_RIGHT_CLICK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!allowInteraction(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(event.getEntity(), event.getHand(), DurabilityRule.TRIGGER_RIGHT_CLICK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!allowInteraction(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(event.getEntity(), event.getHand(), DurabilityRule.TRIGGER_RIGHT_CLICK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!allowInteraction(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        consumeTriggeredDurability(event.getEntity(), event.getHand(), DurabilityRule.TRIGGER_RIGHT_CLICK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!allowInteraction(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!allowInteraction(event.getEntity(), event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (event.player.tickCount % 20 == 0) {
            scanPlayerInventory(event.player);
        }
    }

    private static void scanPlayerInventory(Player player) {
        if (player == null) return;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ensureDurabilityState(player.getInventory().getItem(i));
        }
    }

    private static void ensureDurabilityState(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemAttributeDataManager manager = ItemAttributeDataManager.getInstance();
        DurabilityRule rule = manager.getDurabilityRule(stack.getItem());
        
        if (rule != null) {
            if (rule.mode() == DurabilityMode.CUSTOM) {
                DurabilityHelper.ensureCustomDurabilityState(stack, rule);
            } else if (rule.mode() == DurabilityMode.VANILLA_OVERRIDE) {
                VanillaDurabilityOverrides.migrateStackIfNeeded(stack);
            }
        }
    }

    private static boolean allowInteraction(LivingEntity entity, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }

        if (!entity.level().isClientSide) {
            ensureDurabilityState(stack);
        }

        return !isBroken(stack);
    }

    private static boolean prepareStackForUse(ItemStack stack) {
        if (!DurabilityHelper.usesCustomDurability(stack)) {
            return false;
        }

        ensureDurabilityState(stack);
        return true;
    }

    private static boolean isBroken(ItemStack stack) {
        return DurabilityHelper.usesCustomDurability(stack) && DurabilityHelper.isCustomBroken(stack);
    }

    private static void consumeTriggeredDurability(Player player, InteractionHand hand, String trigger) {
        if (player == null || hand == null || player.getAbilities().instabuild) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }

        DurabilityRule rule = ItemAttributeDataManager.getInstance().getDurabilityRule(stack.getItem());
        if (rule == null || rule.mode() != DurabilityMode.CUSTOM || !rule.hasTrigger(trigger)) {
            return;
        }

        if (!DurabilityHelper.isCustomDurabilitySupported(stack)) {
            return;
        }

        if (alreadyConsumedThisTick(player, hand, trigger)) {
            return;
        }

        DurabilityHelper.ensureCustomDurabilityState(stack, rule);
        if (!DurabilityHelper.hasCustomDurability(stack)) {
            return;
        }

        DurabilityHelper.damageCustom(stack, 1, player, hand);
    }

    private static boolean alreadyConsumedThisTick(Player player, InteractionHand hand, String trigger) {
        String key = player.getUUID() + "|" + hand.name() + "|" + trigger;
        Integer previousTick = RECENT_TRIGGER_TICKS.put(key, player.tickCount);
        return previousTick != null && previousTick == player.tickCount;
    }
}
