package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.render.CompanionRenderer;
import com.breakinblocks.kindredspirits.client.render.MeteorRenderer;
import com.breakinblocks.kindredspirits.client.render.MiniPlayerRenderer;
import com.breakinblocks.kindredspirits.client.render.SpiritArrowRenderer;
import com.breakinblocks.kindredspirits.client.screen.KindredStorageScreen;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredMenus;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID, value = Dist.CLIENT)
public final class KindredSpiritsClient {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(KindredSpiritsClient::clientSetup);
        modEventBus.addListener(KindredSpiritsClient::registerRenderers);
        modEventBus.addListener(KindredKeyMappings::register);
        modEventBus.addListener(KindredParticle::register);
        modEventBus.addListener(KindredSpiritsClient::registerScreens);
        modEventBus.addListener(KindredSpiritsClient::registerRenderStateModifiers);
        modEventBus.addListener(KindredSpiritsClient::registerReloadListeners);
    }

    private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<EntityRenderer<Entity, EntityRenderState>>() {}, PackGlowOverlay::outline);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> KindredSpirits.LOGGER.debug("Kindred Spirits client setup complete"));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(KindredMenus.COMPANION_STORAGE.get(), KindredStorageScreen::new);
    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(KindredSpirits.id("companion_skins"), (ResourceManagerReloadListener)
                resourceManager -> CompanionSkins.clear());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            event.registerEntityRenderer(
                    KindredEntities.type(species),
                    context -> species.usesPlayerSkin()
                            ? new MiniPlayerRenderer(context, species)
                            : new CompanionRenderer(context, species));
        }
        event.registerEntityRenderer(KindredEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(KindredEntities.SPIRIT_ARROW.get(), SpiritArrowRenderer::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        OreRevealOverlay.clientTick();
        PackGlowOverlay.clientTick();
        while (KindredKeyMappings.CYCLE_COMMAND.consumeClick()) {
            ClientPacketDistributor.sendToServer(KindredNetworking.CycleCommandPayload.INSTANCE);
        }
        while (KindredKeyMappings.OPEN_CHARM.consumeClick()) {
            ClientPacketDistributor.sendToServer(KindredNetworking.OpenCharmPayload.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ScreenShake.apply(event);
    }

    private KindredSpiritsClient() {}
}
