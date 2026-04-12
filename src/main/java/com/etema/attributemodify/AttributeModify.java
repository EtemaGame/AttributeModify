package com.etema.attributemodify;

import com.etema.attributemodify.handler.*;
import com.etema.attributemodify.integration.CuriosIntegration;
import com.etema.attributemodify.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AttributeModify.MODID)
public class AttributeModify {
    public static final String MODID = "attributemodify";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AttributeModify() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ItemAttributeHandler());
        MinecraftForge.EVENT_BUS.register(new AttributeTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new QualitySystemHandler());
        MinecraftForge.EVENT_BUS.register(new MiningTierHandler());

        modEventBus.addListener(this::commonSetup);

        CuriosIntegration.initialize();
        LOGGER.info("AttributeModify Initialized (Forge 1.20.1)");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // ConfigHandler removed - using datapack-only mode
            NetworkHandler.register();
            LOGGER.info("AttributeModify - Setup completed (datapack-only mode)");
        });
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ItemAttributeDataManager.getInstance());
        LOGGER.info("AttributeModify - Datapack reload listener registered");
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        ItemAttributeDataManager.getInstance().resolveDeferredTags();
    }
}
