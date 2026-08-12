package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.render.CompanionRenderer;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID, value = Dist.CLIENT)
public final class KindredSpiritsClient {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(KindredSpiritsClient::clientSetup);
        modEventBus.addListener(KindredSpiritsClient::registerRenderers);
        modEventBus.addListener(KindredKeyMappings::register);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> KindredSpirits.LOGGER.debug("Kindred Spirits client setup complete"));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            event.registerEntityRenderer(KindredEntities.type(species), context -> new CompanionRenderer(context, species));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (KindredKeyMappings.CYCLE_COMMAND.consumeClick()) {
            ClientPacketDistributor.sendToServer(KindredNetworking.CycleCommandPayload.INSTANCE);
        }
    }

    private KindredSpiritsClient() {
    }
}
