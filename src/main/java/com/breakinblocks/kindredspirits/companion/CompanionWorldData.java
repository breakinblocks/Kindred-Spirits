package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Persistent work that cannot depend on the companion or owner remaining loaded. */
@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionWorldData {
    public record Death(UUID owner, UUID companion, CompanionSnapshot snapshot, long readyAt) {
        public static final Codec<Death> CODEC = RecordCodecBuilder.create(i -> i.group(
                        UUIDUtil.CODEC.fieldOf("owner").forGetter(Death::owner),
                        UUIDUtil.CODEC.fieldOf("companion").forGetter(Death::companion),
                        CompanionSnapshot.CODEC.fieldOf("snapshot").forGetter(Death::snapshot),
                        Codec.LONG.fieldOf("ready_at").forGetter(Death::readyAt))
                .apply(i, Death::new));
    }

    public record LightRemoval(BlockPos pos, Block block) {
        public static final Codec<LightRemoval> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(LightRemoval::pos),
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(LightRemoval::block))
                .apply(i, LightRemoval::new));
    }

    public static final Supplier<AttachmentType<List<Death>>> DEATHS = KindredAttachments.ATTACHMENT_TYPES.register(
            "pending_deaths",
            () -> AttachmentType.<List<Death>>builder(() -> List.of())
                    .serialize(Death.CODEC.listOf().fieldOf("deaths").codec())
                    .build());
    public static final Supplier<AttachmentType<List<LightRemoval>>> LIGHT_REMOVALS =
            KindredAttachments.ATTACHMENT_TYPES.register(
                    "pending_light_removals",
                    () -> AttachmentType.<List<LightRemoval>>builder(() -> List.of())
                            .serialize(LightRemoval.CODEC
                                    .listOf()
                                    .fieldOf("lights")
                                    .codec())
                            .build());

    public static final Supplier<AttachmentType<List<UUID>>> ABANDONED = KindredAttachments.ATTACHMENT_TYPES.register(
            "abandoned_companions",
            () -> AttachmentType.<List<UUID>>builder(() -> List.of())
                    .serialize(UUIDUtil.CODEC.listOf().fieldOf("companions").codec())
                    .build());

    public static void init() {}

    public static void abandon(MinecraftServer server, UUID companion) {
        ServerLevel storage = server.overworld();
        List<UUID> abandoned = storage.getData(ABANDONED);
        if (abandoned.contains(companion)) return;
        List<UUID> updated = new ArrayList<>(abandoned);
        updated.add(companion);
        storage.setData(ABANDONED, List.copyOf(updated));
    }

    public static void recordDeath(ServerLevel level, Death death) {
        ServerLevel storage = level.getServer().overworld();
        List<Death> deaths = new ArrayList<>(storage.getData(DEATHS));
        deaths.removeIf(entry -> entry.companion().equals(death.companion()));
        deaths.add(death);
        storage.setData(DEATHS, List.copyOf(deaths));
    }

    public static void reconcile(ServerPlayer player) {
        ServerLevel storage = player.level().getServer().overworld();
        List<Death> deaths = new ArrayList<>(storage.getData(DEATHS));
        boolean changed = deaths.removeIf(death -> {
            if (!death.owner().equals(player.getUUID())) return false;
            if (KindredAttachments.bond(player).isBoundTo(death.companion())) {
                KindredAttachments.modifyBond(
                        player,
                        bond -> bond.withSnapshot(death.companion(), death.snapshot())
                                .withStored(true)
                                .withReviveReadyAt(death.readyAt()));
            }
            return true;
        });
        if (changed) storage.setData(DEATHS, List.copyOf(deaths));
    }

    public static void removeLight(ServerLevel level, BlockPos pos, Block block) {
        if (level.isLoaded(pos)) {
            if (level.getBlockState(pos).is(block)) level.removeBlock(pos, false);
            return;
        }
        List<LightRemoval> pending = new ArrayList<>(level.getData(LIGHT_REMOVALS));
        LightRemoval removal = new LightRemoval(pos.immutable(), block);
        if (!pending.contains(removal)) {
            pending.add(removal);
            level.setData(LIGHT_REMOVALS, List.copyOf(pending));
        }
    }

    public static void cleanLoadedLights(ServerLevel level) {
        List<LightRemoval> pending = new ArrayList<>(level.getData(LIGHT_REMOVALS));
        if (pending.removeIf(removal -> {
            if (!level.isLoaded(removal.pos())) return false;
            if (level.getBlockState(removal.pos()).is(removal.block())) level.removeBlock(removal.pos(), false);
            return true;
        })) level.setData(LIGHT_REMOVALS, List.copyOf(pending));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcile(player);
            KindredAttachments.syncTooltip(player);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof CompanionEntity companion
                && event.getLevel() instanceof ServerLevel level
                && level.getServer().overworld().getData(ABANDONED).contains(companion.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0) cleanLoadedLights(level);
    }

    private CompanionWorldData() {}
}
