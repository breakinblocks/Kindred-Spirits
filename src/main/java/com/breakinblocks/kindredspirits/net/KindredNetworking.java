package com.breakinblocks.kindredspirits.net;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Comparator;
import java.util.List;

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
    }

    public static void sendLevelUp(ServerPlayer player, int companionId, int level) {
        PacketDistributor.sendToPlayer(player, new CompanionLevelUpPayload(companionId, level));
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
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }

                AABB search = player.getBoundingBox().inflate(COMMAND_RANGE);
                List<CompanionEntity> owned = player.level().getEntitiesOfClass(CompanionEntity.class, search,
                        companion -> companion.isTame() && companion.isOwnedBy(player));

                owned.stream()
                        .min(Comparator.comparingDouble(companion -> companion.distanceToSqr(player)))
                        .ifPresent(companion -> {
                            CompanionCommand command = companion.getCommand().next();
                            companion.setCommand(command);
                            player.sendSystemMessage(Component.translatable("message.kindredspirits.command_set",
                                    companion.getDisplayName(), command.displayName()));
                        });
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
