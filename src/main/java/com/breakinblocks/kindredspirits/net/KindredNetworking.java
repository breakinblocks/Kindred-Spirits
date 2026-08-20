package com.breakinblocks.kindredspirits.net;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredClientHooks;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;


@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class KindredNetworking {
    private static final double COMMAND_RANGE = 24.0;

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(
                CycleCommandPayload.TYPE,
                CycleCommandPayload.STREAM_CODEC,
                CycleCommandPayload::handleOnServer);

        registrar.playToClient(
                CompanionLevelUpPayload.TYPE,
                CompanionLevelUpPayload.STREAM_CODEC,
                CompanionLevelUpPayload::handleOnClient);

        registrar.playToClient(
                CharmViewPayload.TYPE,
                CharmViewPayload.STREAM_CODEC,
                CharmViewPayload::handleOnClient);

        registrar.playToServer(
                CharmActionPayload.TYPE,
                CharmActionPayload.STREAM_CODEC,
                CharmActionPayload::handleOnServer);
    }

    public static void sendLevelUp(ServerPlayer player, int companionId, int level) {
        PacketDistributor.sendToPlayer(player, new CompanionLevelUpPayload(companionId, level));
    }

    public static void sendCharmView(ServerPlayer player, boolean open) {
        PacketDistributor.sendToPlayer(player, new CharmViewPayload(KindredCharmItem.viewFor(player), open));
    }

    public record CharmViewPayload(CharmView view, boolean open) implements CustomPacketPayload {
        public static final Type<CharmViewPayload> TYPE = new Type<>(KindredSpirits.id("charm_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CharmViewPayload> STREAM_CODEC =
                StreamCodec.composite(
                        CharmView.STREAM_CODEC, CharmViewPayload::view,
                        ByteBufCodecs.BOOL, CharmViewPayload::open,
                        CharmViewPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(CharmViewPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> KindredClientHooks.acceptCharmView(payload.view(), payload.open()));
        }
    }

    public record CharmActionPayload(Action action, int value, String text) implements CustomPacketPayload {
        public static final int MAX_NAME_LENGTH = 32;

        public static final Type<CharmActionPayload> TYPE = new Type<>(KindredSpirits.id("charm_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CharmActionPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(ordinal -> Action.values()[ordinal], Action::ordinal),
                        CharmActionPayload::action,
                        ByteBufCodecs.VAR_INT, CharmActionPayload::value,
                        ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), CharmActionPayload::text,
                        CharmActionPayload::new);

        public CharmActionPayload(Action action, int value) {
            this(action, value, "");
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnServer(CharmActionPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    KindredCharmItem.handleAction(player, payload.action(), payload.value(), payload.text());
                }
            });
        }

        public enum Action {
            SUMMON,
            DISMISS,
            RECALL,
            RELEASE,
            SET_COMMAND,
            SET_AGGRESSION,
            SET_NAME,
            SET_SKIN,
            REFRESH
        }
    }

    public record CycleCommandPayload() implements CustomPacketPayload {
        public static final CycleCommandPayload INSTANCE = new CycleCommandPayload();
        public static final Type<CycleCommandPayload> TYPE = new Type<>(KindredSpirits.id("cycle_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleCommandPayload> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnServer(CycleCommandPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    CompanionEntity.nearestOwned(player, COMMAND_RANGE)
                            .ifPresent(companion -> companion.cycleCommandBy(player));
                }
            });
        }
    }

    public record CompanionLevelUpPayload(int entityId, int level) implements CustomPacketPayload {
        public static final Type<CompanionLevelUpPayload> TYPE = new Type<>(KindredSpirits.id("companion_level_up"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CompanionLevelUpPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, CompanionLevelUpPayload::entityId,
                        ByteBufCodecs.VAR_INT, CompanionLevelUpPayload::level,
                        CompanionLevelUpPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(CompanionLevelUpPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Player player = context.player();
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof CompanionEntity companion) {
                    companion.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
                }
            });
        }
    }

    private KindredNetworking() {
    }
}
