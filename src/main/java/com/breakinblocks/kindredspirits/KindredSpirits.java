package com.breakinblocks.kindredspirits;

import com.breakinblocks.kindredspirits.client.KindredSpiritsClient;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredBlocks;
import com.breakinblocks.kindredspirits.registry.KindredCreativeTabs;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.breakinblocks.kindredspirits.registry.KindredMenus;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(KindredSpirits.MOD_ID)
public class KindredSpirits {
    public static final String MOD_ID = "kindredspirits";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public KindredSpirits(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        modContainer.registerConfig(ModConfig.Type.STARTUP, KindredConfig.STARTUP_SPEC);

        KindredBlocks.BLOCKS.register(modEventBus);
        KindredEntities.ENTITY_TYPES.register(modEventBus);
        KindredItems.ITEMS.register(modEventBus);
        KindredAttachments.ATTACHMENT_TYPES.register(modEventBus);
        KindredCreativeTabs.CREATIVE_TABS.register(modEventBus);
        KindredSounds.SOUND_EVENTS.register(modEventBus);
        KindredMenus.MENUS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, KindredConfig.COMMON_SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::createAttributes);

        if (dist.isClient()) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, KindredConfig.CLIENT_SPEC);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
            KindredSpiritsClient.init(modEventBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.debug("Kindred Spirits loaded {} companion species", CompanionSpecies.values().length));
    }

    private void createAttributes(EntityAttributeCreationEvent event) {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            event.put(KindredEntities.type(species), CompanionEntity.createCompanionAttributes(species).build());
        }
    }

}
