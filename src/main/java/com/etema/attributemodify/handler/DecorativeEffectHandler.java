package com.etema.attributemodify.handler;

import com.etema.attributemodify.compat.DecorativeCompatService;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.etema.attributemodify.AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DecorativeEffectHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        if (!DecorativeCompatService.shouldClearEffects(player)) {
            return;
        }

        clearActiveEffects(player);
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !DecorativeCompatService.shouldClearEffects(entity)) {
            return;
        }

        clearActiveEffects(entity);
    }

    private static void clearActiveEffects(LivingEntity entity) {
        for (MobEffectInstance effect : java.util.List.copyOf(entity.getActiveEffects())) {
            if (effect != null) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }
}
